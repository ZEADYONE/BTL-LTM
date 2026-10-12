#!/usr/bin/env python3
"""Generate the original arcade/chiptune audio pack used by client-fx.

The synthesizer intentionally uses only Python's standard library. FFmpeg is
required only for encoding the two music loops to MP3.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
import shutil
import struct
import subprocess
import wave
from array import array
from pathlib import Path


SAMPLE_RATE = 44_100
PEAK = 10 ** (-1.0 / 20.0)
# Lossy MP3 encoding can introduce inter-sample peaks, especially in the dense
# battle mix. Keep extra source headroom so the encoded files remain below -1 dBFS.
MUSIC_PEAK = 10 ** (-3.5 / 20.0)
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "audio"
BUILD = ROOT / "build" / "generated-audio"


def oscillator(kind: str, phase: float, duty: float = 0.5) -> float:
    cycle = phase / math.tau
    fraction = cycle - math.floor(cycle)
    if kind == "sine":
        return math.sin(phase)
    if kind == "square":
        return 1.0 if fraction < 0.5 else -1.0
    if kind == "pulse":
        return 1.0 if fraction < duty else -1.0
    if kind == "triangle":
        return 1.0 - 4.0 * abs(fraction - 0.5)
    if kind == "saw":
        return 2.0 * fraction - 1.0
    raise ValueError(f"Unknown oscillator: {kind}")


def envelope(position: float, duration: float, attack: float, release: float) -> float:
    fade_in = min(1.0, position / max(attack, 1e-6))
    fade_out = min(1.0, (duration - position) / max(release, 1e-6))
    return max(0.0, min(fade_in, fade_out))


def add_tone(
    left: array,
    right: array,
    start: float,
    duration: float,
    frequency: float,
    amplitude: float,
    *,
    kind: str = "square",
    end_frequency: float | None = None,
    attack: float = 0.004,
    release: float = 0.04,
    decay: float = 0.0,
    pan: float = 0.0,
    vibrato_hz: float = 0.0,
    vibrato_depth: float = 0.0,
    duty: float = 0.5,
) -> None:
    first = max(0, round(start * SAMPLE_RATE))
    count = min(round(duration * SAMPLE_RATE), len(left) - first)
    if count <= 0:
        return
    phase = 0.0
    left_gain = math.sqrt((1.0 - pan) * 0.5)
    right_gain = math.sqrt((1.0 + pan) * 0.5)
    ratio = (end_frequency / frequency) if end_frequency else 1.0
    for offset in range(count):
        position = offset / SAMPLE_RATE
        progress = offset / max(1, count - 1)
        current_frequency = frequency * (ratio ** progress)
        if vibrato_hz:
            current_frequency *= 1.0 + vibrato_depth * math.sin(math.tau * vibrato_hz * position)
        phase += math.tau * current_frequency / SAMPLE_RATE
        value = oscillator(kind, phase, duty)
        value *= envelope(position, duration, attack, release)
        if decay:
            value *= math.exp(-decay * position)
        value *= amplitude
        index = first + offset
        left[index] += value * left_gain
        right[index] += value * right_gain


def add_noise(
    left: array,
    right: array,
    start: float,
    duration: float,
    amplitude: float,
    *,
    decay: float,
    pan: float = 0.0,
    lowpass: float = 0.18,
    seed: int = 1,
) -> None:
    first = max(0, round(start * SAMPLE_RATE))
    count = min(round(duration * SAMPLE_RATE), len(left) - first)
    generator = random.Random(seed)
    filtered = 0.0
    left_gain = math.sqrt((1.0 - pan) * 0.5)
    right_gain = math.sqrt((1.0 + pan) * 0.5)
    for offset in range(count):
        position = offset / SAMPLE_RATE
        raw = generator.uniform(-1.0, 1.0)
        filtered += lowpass * (raw - filtered)
        value = filtered * amplitude * math.exp(-decay * position)
        value *= envelope(position, duration, 0.001, min(0.08, duration * 0.4))
        index = first + offset
        left[index] += value * left_gain
        right[index] += value * right_gain


def add_kick(left: array, right: array, start: float, amplitude: float = 0.42) -> None:
    add_tone(
        left, right, start, 0.18, 125, amplitude,
        kind="sine", end_frequency=43, attack=0.001, release=0.06, decay=11.0,
    )


def add_snare(left: array, right: array, start: float, amplitude: float = 0.22) -> None:
    add_noise(left, right, start, 0.14, amplitude, decay=16.0, lowpass=0.55, seed=round(start * 10_000) + 71)
    add_tone(left, right, start, 0.1, 180, amplitude * 0.42, kind="triangle", release=0.05, decay=18.0)


def add_hat(left: array, right: array, start: float, amplitude: float = 0.09) -> None:
    add_noise(left, right, start, 0.045, amplitude, decay=45.0, lowpass=0.95, seed=round(start * 20_000) + 19)


def note(midi: int) -> float:
    return 440.0 * 2 ** ((midi - 69) / 12)


def buffer(duration: float) -> tuple[array, array]:
    size = math.ceil(duration * SAMPLE_RATE)
    return array("f", [0.0]) * size, array("f", [0.0]) * size


def soft_limit_and_normalize(left: array, right: array, target_peak: float = PEAK) -> None:
    maximum = 0.0
    for channel in (left, right):
        for index, value in enumerate(channel):
            limited = math.tanh(value * 1.12) / math.tanh(1.12)
            channel[index] = limited
            maximum = max(maximum, abs(limited))
    gain = target_peak / maximum if maximum > 0 else 1.0
    for channel in (left, right):
        for index in range(len(channel)):
            channel[index] *= gain


def write_wav(path: Path, left: array, right: array | None = None, target_peak: float = PEAK) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if right is None:
        scratch = array("f", left)
        soft_limit_and_normalize(left, scratch, target_peak)
    else:
        soft_limit_and_normalize(left, right, target_peak)
    stereo = right is not None
    frames = bytearray()
    if stereo:
        for l_value, r_value in zip(left, right):
            frames.extend(struct.pack("<hh", round(l_value * 32767), round(r_value * 32767)))
    else:
        for value in left:
            frames.extend(struct.pack("<h", round(value * 32767)))
    with wave.open(str(path), "wb") as output:
        output.setnchannels(2 if stereo else 1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(frames)


def mono_mix(duration: float, builder) -> array:
    left, right = buffer(duration)
    builder(left, right)
    return array("f", ((l + r) * 0.70710678 for l, r in zip(left, right)))


def generate_sfx() -> list[Path]:
    target = OUTPUT / "sfx"
    generated: list[Path] = []

    def save(name: str, duration: float, builder) -> None:
        path = target / name
        write_wav(path, mono_mix(duration, builder))
        generated.append(path)

    save("ui-click.wav", 0.085, lambda l, r: (
        add_tone(l, r, 0.0, 0.055, 760, 0.52, kind="square", end_frequency=980, release=0.025),
        add_tone(l, r, 0.012, 0.045, 1520, 0.16, kind="sine", release=0.022),
    ))

    save("ui-success.wav", 0.44, lambda l, r: [
        add_tone(l, r, index * 0.085, 0.16, note(midi), 0.30, kind="triangle", release=0.07)
        for index, midi in enumerate((72, 76, 79, 84))
    ])

    save("ui-error.wav", 0.34, lambda l, r: (
        add_tone(l, r, 0.0, 0.17, 370, 0.34, kind="square", end_frequency=310, release=0.05),
        add_tone(l, r, 0.15, 0.18, 277, 0.34, kind="square", end_frequency=220, release=0.07),
    ))

    save("match-start.wav", 1.18, lambda l, r: (
        *[
            add_tone(l, r, index * 0.24, 0.15, note(67), 0.28, kind="pulse", release=0.06, duty=0.25)
            for index in range(3)
        ],
        add_tone(l, r, 0.76, 0.38, note(79), 0.42, kind="square", release=0.16),
        add_tone(l, r, 0.76, 0.38, note(71), 0.22, kind="triangle", release=0.16),
    ))

    save("bomb-place.wav", 0.25, lambda l, r: (
        add_tone(l, r, 0.0, 0.20, 155, 0.48, kind="sine", end_frequency=72, attack=0.001, release=0.06, decay=6.0),
        add_tone(l, r, 0.018, 0.11, 520, 0.16, kind="pulse", end_frequency=690, release=0.045, duty=0.2),
    ))

    save("bomb-place-soft.wav", 0.22, lambda l, r: (
        add_tone(l, r, 0.0, 0.18, 130, 0.32, kind="sine", end_frequency=76, attack=0.001, release=0.06, decay=7.0),
        add_tone(l, r, 0.02, 0.09, 430, 0.10, kind="triangle", end_frequency=520, release=0.04),
    ))

    save("explosion.wav", 0.78, lambda l, r: (
        add_noise(l, r, 0.0, 0.75, 0.95, decay=5.4, lowpass=0.12, seed=90421),
        add_noise(l, r, 0.0, 0.42, 0.34, decay=9.0, lowpass=0.72, seed=48217),
        add_tone(l, r, 0.0, 0.62, 92, 0.66, kind="sine", end_frequency=38, attack=0.001, release=0.18, decay=4.5),
    ))

    save("player-defeated.wav", 0.95, lambda l, r: (
        add_tone(l, r, 0.0, 0.83, 430, 0.36, kind="saw", end_frequency=92, attack=0.003, release=0.18, decay=1.4, vibrato_hz=8, vibrato_depth=0.025),
        add_tone(l, r, 0.08, 0.72, 215, 0.27, kind="triangle", end_frequency=58, release=0.20, decay=1.9),
    ))

    save("victory.wav", 1.85, lambda l, r: (
        *[
            add_tone(l, r, start, duration, note(midi), 0.30, kind="square", release=0.10)
            for start, duration, midi in (
                (0.00, 0.20, 72), (0.20, 0.20, 76), (0.40, 0.20, 79),
                (0.62, 0.24, 84), (0.88, 0.20, 79), (1.08, 0.68, 84),
            )
        ],
        *[
            add_tone(l, r, 1.08, 0.70, note(midi), 0.15, kind="triangle", release=0.24)
            for midi in (72, 76, 79)
        ],
    ))

    save("defeat.wav", 1.55, lambda l, r: (
        *[
            add_tone(l, r, index * 0.25, 0.34, note(midi), 0.31, kind="triangle", release=0.12)
            for index, midi in enumerate((67, 64, 60, 55))
        ],
        add_tone(l, r, 0.78, 0.70, note(43), 0.22, kind="sine", end_frequency=46, release=0.26, decay=0.8),
    ))

    save("draw.wav", 1.35, lambda l, r: (
        add_tone(l, r, 0.0, 0.26, note(67), 0.27, kind="pulse", release=0.09, duty=0.25),
        add_tone(l, r, 0.29, 0.26, note(70), 0.27, kind="pulse", release=0.09, duty=0.25),
        *[
            add_tone(l, r, 0.60, 0.67, note(midi), 0.18, kind="triangle", release=0.22)
            for midi in (60, 65, 67)
        ],
    ))
    return generated


def generate_menu_music() -> tuple[array, array, float]:
    bpm = 120
    beat = 60.0 / bpm
    bars = 8
    duration = bars * 4 * beat
    left, right = buffer(duration)
    chords = ((60, 64, 67), (57, 60, 64), (53, 57, 60), (55, 59, 62))
    melody = (72, 76, 79, 76, 74, 72, 69, 71, 72, 74, 76, 79, 77, 76, 74, 71)

    for bar in range(bars):
        start = bar * 4 * beat
        chord = chords[bar % len(chords)]
        for step in range(8):
            midi = chord[step % len(chord)] + 12
            add_tone(left, right, start + step * beat / 2, beat * 0.38, note(midi), 0.105,
                     kind="triangle", release=0.08, pan=-0.28 if step % 2 == 0 else 0.28)
        for step in range(4):
            add_tone(left, right, start + step * beat, beat * 0.72, note(chord[0] - 12), 0.16,
                     kind="pulse", release=0.12, duty=0.25, pan=-0.05)
        for step in range(8):
            midi = melody[(bar * 2 + step) % len(melody)] + (12 if bar >= 6 and step >= 4 else 0)
            add_tone(left, right, start + step * beat / 2, beat * 0.39, note(midi), 0.13,
                     kind="square", release=0.07, pan=0.18, vibrato_hz=5.2, vibrato_depth=0.004)
        add_kick(left, right, start, 0.25)
        add_kick(left, right, start + 2 * beat, 0.22)
        add_snare(left, right, start + beat, 0.12)
        add_snare(left, right, start + 3 * beat, 0.12)
        for step in range(8):
            add_hat(left, right, start + step * beat / 2, 0.035 if step % 2 else 0.05)
    return left, right, duration


def generate_battle_music() -> tuple[array, array, float]:
    bpm = 160
    beat = 60.0 / bpm
    bars = 12
    duration = bars * 4 * beat
    left, right = buffer(duration)
    roots = (45, 45, 48, 43, 45, 45, 50, 48, 41, 43, 45, 45)
    riff = (0, 0, 7, 0, 3, 2, 0, 10, 0, 0, 7, 12, 10, 7, 3, 2)

    for bar in range(bars):
        start = bar * 4 * beat
        root = roots[bar]
        for step in range(16):
            tone = root + 12 + riff[(step + (bar % 3) * 4) % len(riff)]
            accent = 0.15 if step % 4 == 0 else 0.105
            add_tone(left, right, start + step * beat / 4, beat * 0.20, note(tone), accent,
                     kind="pulse", release=0.026, duty=0.25, pan=0.12 if step % 2 else -0.12)
        for step in range(8):
            bass = root if step % 4 != 3 else root + 7
            add_tone(left, right, start + step * beat / 2, beat * 0.38, note(bass), 0.19,
                     kind="saw", release=0.055, pan=-0.08)
        if bar % 2 == 1:
            lead = (69, 72, 76, 74, 72, 69, 67, 64)
            for step, midi in enumerate(lead):
                add_tone(left, right, start + step * beat / 2, beat * 0.37, note(midi), 0.10,
                         kind="square", release=0.06, pan=0.30, vibrato_hz=6, vibrato_depth=0.006)
        for step in range(4):
            add_kick(left, right, start + step * beat, 0.31 if step in (0, 2) else 0.22)
        add_snare(left, right, start + beat, 0.17)
        add_snare(left, right, start + 3 * beat, 0.18)
        for step in range(8):
            add_hat(left, right, start + step * beat / 2, 0.055 if step % 2 == 0 else 0.04)
    return left, right, duration


def encode_mp3(source: Path, target: Path) -> None:
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise RuntimeError("FFmpeg is required to encode music loops")
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(source),
            "-map_metadata", "-1", "-codec:a", "libmp3lame", "-b:a", "160k",
            "-ar", str(SAMPLE_RATE), "-ac", "2", "-write_xing", "1", str(target),
        ],
        check=True,
    )


def generate_music() -> list[Path]:
    BUILD.mkdir(parents=True, exist_ok=True)
    generated: list[Path] = []
    for name, generator in (("menu-loop", generate_menu_music), ("battle-loop", generate_battle_music)):
        left, right, _ = generator()
        source = BUILD / f"{name}.wav"
        target = OUTPUT / "music" / f"{name}.mp3"
        write_wav(source, left, right, MUSIC_PEAK)
        encode_mp3(source, target)
        generated.append(target)
    return generated


def inspect(path: Path) -> dict[str, object]:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if path.suffix == ".wav":
        with wave.open(str(path), "rb") as source:
            duration = source.getnframes() / source.getframerate()
            channels = source.getnchannels()
            sample_rate = source.getframerate()
    else:
        ffprobe = shutil.which("ffprobe")
        if ffprobe is None:
            raise RuntimeError("ffprobe is required to inspect generated MP3 files")
        result = subprocess.run(
            [
                ffprobe, "-v", "error", "-select_streams", "a:0",
                "-show_entries", "stream=sample_rate,channels:format=duration",
                "-of", "json", str(path),
            ],
            check=True, capture_output=True, text=True,
        )
        details = json.loads(result.stdout)
        duration = float(details["format"]["duration"])
        channels = int(details["streams"][0]["channels"])
        sample_rate = int(details["streams"][0]["sample_rate"])
    return {
        "path": path.relative_to(OUTPUT).as_posix(),
        "durationSeconds": round(duration, 3),
        "sampleRate": sample_rate,
        "channels": channels,
        "sizeBytes": path.stat().st_size,
        "sha256": digest,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sfx-only", action="store_true", help="Skip music generation and MP3 encoding")
    arguments = parser.parse_args()

    generated = generate_sfx()
    if not arguments.sfx_only:
        generated.extend(generate_music())
    manifest = {
        "generator": "client-fx/audio-tools/generate_audio.py",
        "sampleRate": SAMPLE_RATE,
        "sfxTargetPeakDbfs": -1.0,
        "musicSourceTargetPeakDbfs": -3.5,
        "assets": [inspect(path) for path in sorted(generated)],
    }
    manifest_path = OUTPUT / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(generated)} audio files in {OUTPUT}")
    print(f"Total encoded size: {sum(path.stat().st_size for path in generated):,} bytes")


if __name__ == "__main__":
    main()
