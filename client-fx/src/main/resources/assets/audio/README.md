# Bomberman audio pack

Original procedural arcade/chiptune audio for the JavaFX client. The files use
no third-party samples or recordings.

## Regenerate

Requirements: Python 3.11+ and FFmpeg available on `PATH`.

```powershell
python client-fx/audio-tools/generate_audio.py
```

The generator writes short effects as mono 44.1 kHz, 16-bit PCM WAV files. It
renders music masters into `client-fx/build/generated-audio/` and encodes the
runtime copies as stereo 160 kbps MP3. The build directory is intentionally not
tracked.

`manifest.json` records duration, channel count, encoded size and SHA-256 for
each generated runtime asset. Generation is deterministic.

## Creative direction

- Bright arcade/chiptune tone matching the colorful JavaFX interface.
- Short, readable effects that remain distinct during a four-player match.
- Restrained bass and one explosion cue per snapshot batch to avoid clipping.
- Menu music at 120 BPM; battle music at 160 BPM.

Do not edit the binary files by hand. Change the generator and regenerate the
pack so that source and outputs remain reproducible.
