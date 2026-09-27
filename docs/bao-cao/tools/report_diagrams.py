"""Generic sequence and flow diagram drawing in the black-and-white style of Chapter 2."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

from report_common import BLACK, WHITE, arrow, font, rgb, round_box, save_image, text_centered, wrapped_lines

WIDTH = 1600
LINE_HEIGHT = 30


def _measure() -> ImageDraw.ImageDraw:
    return ImageDraw.Draw(Image.new("RGB", (10, 10)))


def _header_height(subtitle: str | None, width: int) -> int:
    if not subtitle:
        return 130
    lines = wrapped_lines(_measure(), subtitle, font(23), width - 150)
    return 112 + LINE_HEIGHT * len(lines)


def canvas(title: str, subtitle: str | None, height: int, width: int = WIDTH):
    """White framed canvas with a title rule; returns (image, draw, content_top)."""
    image = Image.new("RGB", (width, height), rgb(WHITE))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((20, 20, width - 20, height - 20), radius=24, outline=rgb(BLACK), width=3)
    draw.text((60, 42), title, font=font(38, True), fill=rgb(BLACK))
    y = 100
    if subtitle:
        for line in wrapped_lines(draw, subtitle, font(23), width - 150):
            draw.text((60, y), line, font=font(23), fill=rgb(BLACK))
            y += LINE_HEIGHT
    rule = _header_height(subtitle, width) - 12
    draw.line((60, rule, width - 60, rule), fill=rgb(BLACK), width=3)
    return image, draw, rule + 30


def dashed_line(draw, start, end, width=3, dash=14, gap=9):
    (sx, sy), (ex, ey) = start, end
    length = max(abs(ex - sx), abs(ey - sy))
    if length == 0:
        return
    step_x, step_y = (ex - sx) / length, (ey - sy) / length
    position = 0.0
    while position < length:
        stop = min(position + dash, length)
        draw.line(
            (sx + step_x * position, sy + step_y * position, sx + step_x * stop, sy + step_y * stop),
            fill=rgb(BLACK),
            width=width,
        )
        position = stop + gap


def text_left(draw, x: float, y: float, text: str, max_width: int, chosen_font=None) -> int:
    """Left-aligned wrapped text; returns the height used."""
    chosen_font = chosen_font or font(22)
    lines = wrapped_lines(draw, text, chosen_font, max_width)
    for index, line in enumerate(lines):
        draw.text((x, y + index * LINE_HEIGHT), line, font=chosen_font, fill=rgb(BLACK))
    return LINE_HEIGHT * len(lines)


def _label(draw, x: float, y: float, text: str, chosen_font):
    """Text on a white backing so lifelines crossing the label stay readable."""
    x1, y1, x2, y2 = draw.textbbox((x, y), text, font=chosen_font)
    draw.rectangle((x1 - 4, y1 - 3, x2 + 4, y2 + 3), fill=rgb(WHITE))
    draw.text((x, y), text, font=chosen_font, fill=rgb(BLACK))


def text_right(draw, x_right: float, y: float, text: str, max_width: int, chosen_font=None) -> int:
    chosen_font = chosen_font or font(22)
    lines = wrapped_lines(draw, text, chosen_font, max_width)
    for index, line in enumerate(lines):
        width = draw.textbbox((0, 0), line, font=chosen_font)[2]
        draw.text((x_right - width, y + index * LINE_HEIGHT), line, font=chosen_font, fill=rgb(BLACK))
    return LINE_HEIGHT * len(lines)


def sequence_diagram(
    file_name: str,
    title: str,
    subtitle: str | None,
    participants: list[str],
    steps: list[tuple],
    width: int = WIDTH,
) -> Path:
    """Steps: ("msg", a, b, label), ("reply", a, b, label), ("self", a, label),
    ("note", label) spanning all participants, or ("note", label, a, b)."""
    measure = _measure()
    label_font = font(22)
    left, right = 50, width - 50
    column = (right - left) / len(participants)
    xs = [left + column * (index + 0.5) for index in range(len(participants))]

    def message_lines(a: int, b: int, label: str) -> list[str]:
        span = max(abs(xs[b] - xs[a]) - 24, 230)
        return wrapped_lines(measure, label, label_font, int(span))

    def self_lines(a: int, label: str) -> list[str]:
        room = (right - xs[a] - 80) if a < len(participants) - 1 else (xs[a] - left - 80)
        return wrapped_lines(measure, label, label_font, int(max(room, 220)))

    heights = []
    for step in steps:
        kind = step[0]
        if kind in ("msg", "reply"):
            heights.append(LINE_HEIGHT * len(message_lines(step[1], step[2], step[3])) + 34)
        elif kind == "self":
            heights.append(max(LINE_HEIGHT * len(self_lines(step[1], step[2])), 46) + 32)
        else:
            heights.append(LINE_HEIGHT * len(wrapped_lines(measure, step[1], label_font, int(right - left - 80))) + 40)

    header = _header_height(subtitle, width)
    box_top, box_height = header + 18, 80
    first_step = box_top + box_height + 36
    height = first_step + sum(heights) + 50
    image, draw, _ = canvas(title, subtitle, height, width)

    box_width = min(column - 22, 320)
    for x, name in zip(xs, participants):
        box = (x - box_width / 2, box_top, x + box_width / 2, box_top + box_height)
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=12, width=3)
        text_centered(draw, box, name, font(22, True), rgb(BLACK), spacing=4)
        dashed_line(draw, (x, box_top + box_height), (x, height - 45), width=2, dash=10, gap=10)

    y = first_step
    number = 0
    for step, step_height in zip(steps, heights):
        kind = step[0]
        if kind in ("msg", "reply"):
            number += 1
            a, b, label = step[1], step[2], step[3]
            lines = message_lines(a, b, label)
            lines[0] = f"{number}. {lines[0]}"
            mid = (xs[a] + xs[b]) / 2
            for index, line in enumerate(lines):
                line_width = draw.textbbox((0, 0), line, font=label_font)[2]
                _label(draw, mid - line_width / 2, y + index * LINE_HEIGHT, line, label_font)
            arrow_y = y + LINE_HEIGHT * len(lines) + 8
            direction = 1 if xs[b] > xs[a] else -1
            if kind == "msg":
                arrow(draw, (xs[a], arrow_y), (xs[b] - direction * 4, arrow_y), BLACK, width=4, head=16)
            else:
                dashed_line(draw, (xs[a], arrow_y), (xs[b] - direction * 20, arrow_y), width=3)
                arrow(draw, (xs[b] - direction * 22, arrow_y), (xs[b] - direction * 4, arrow_y), BLACK, width=3, head=16)
        elif kind == "self":
            number += 1
            a, label = step[1], step[2]
            lines = self_lines(a, label)
            lines[0] = f"{number}. {lines[0]}"
            x = xs[a]
            last = a == len(participants) - 1
            side = -1 if last else 1
            loop_y1, loop_y2 = y + 8, y + 44
            draw.line((x, loop_y1, x + side * 50, loop_y1), fill=rgb(BLACK), width=3)
            draw.line((x + side * 50, loop_y1, x + side * 50, loop_y2), fill=rgb(BLACK), width=3)
            arrow(draw, (x + side * 50, loop_y2), (x + side * 4, loop_y2), BLACK, width=3, head=14)
            text_x = x + side * 64
            for index, line in enumerate(lines):
                if last:
                    line_width = draw.textbbox((0, 0), line, font=label_font)[2]
                    _label(draw, text_x - line_width, y + index * LINE_HEIGHT, line, label_font)
                else:
                    _label(draw, text_x, y + index * LINE_HEIGHT, line, label_font)
        else:
            label = step[1]
            if len(step) > 2:
                x1, x2 = xs[step[2]] - column * 0.46, xs[step[3]] + column * 0.46
            else:
                x1, x2 = left + 10, right - 10
            box = (x1, y + 4, x2, y + step_height - 8)
            draw.rectangle(box, fill=rgb(WHITE), outline=rgb(BLACK), width=2)
            text_centered(draw, box, label, font(22, True), rgb(BLACK), spacing=4)
        y += step_height
    return save_image(image, file_name)


def flow_diagram(
    file_name: str,
    title: str,
    subtitle: str | None,
    nodes: list[tuple],
    loop: tuple[int, int, str] | None = None,
    width: int = WIDTH,
) -> Path:
    """Vertical activity diagram. Nodes: ("start", text), ("step", text), ("end", text),
    ("decision", question, side_text, down_label="Có", side_label="Không").
    A decision's side branch ends in a box to the right. loop=(from, to, label) draws a return edge."""
    measure = _measure()
    body_font = font(24)
    cx = 700
    main_half = 290
    side_x1, side_x2 = cx + main_half + 120, width - 60
    gap = 58

    def box_height(text: str, max_width: int) -> int:
        return max(LINE_HEIGHT * len(wrapped_lines(measure, text, body_font, max_width)) + 40, 84)

    layout = []
    header = _header_height(subtitle, width)
    y = header + 20
    for node in nodes:
        kind = node[0]
        if kind == "decision":
            node_height = max(LINE_HEIGHT * len(wrapped_lines(measure, node[1], body_font, 330)) + 90, 150)
        else:
            node_height = box_height(node[1], 2 * main_half - 40)
        layout.append((y, node_height))
        y += node_height + gap
    height = y - gap + 60
    image, draw, _ = canvas(title, subtitle, height, width)

    for index, (node, (top, node_height)) in enumerate(zip(nodes, layout)):
        kind = node[0]
        bottom = top + node_height
        if kind == "decision":
            mid = top + node_height / 2
            diamond = [(cx, top), (cx + main_half, mid), (cx, bottom), (cx - main_half, mid)]
            draw.polygon(diamond, fill=rgb(WHITE), outline=rgb(BLACK))
            draw.line(diamond + [diamond[0]], fill=rgb(BLACK), width=4)
            text_centered(draw, (cx - 190, top + 20, cx + 190, bottom - 20), node[1], body_font, rgb(BLACK), spacing=4)
            side_label = node[4] if len(node) > 4 else "Không"
            side_height = box_height(node[2], side_x2 - side_x1 - 40)
            side_box = (side_x1, mid - side_height / 2, side_x2, mid + side_height / 2)
            round_box(draw, side_box, rgb(WHITE), rgb(BLACK), radius=16, width=3)
            text_centered(draw, side_box, node[2], body_font, rgb(BLACK), spacing=4)
            arrow(draw, (cx + main_half, mid), (side_x1 - 4, mid), BLACK, width=4, head=16)
            text_centered(draw, (cx + main_half, mid - 46, side_x1, mid - 6), side_label, font(22, True), rgb(BLACK))
        else:
            box = (cx - main_half, top, cx + main_half, bottom)
            radius = 42 if kind in ("start", "end") else 14
            round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=radius, width=4 if kind != "step" else 3)
            text_centered(draw, box, node[1], font(24, kind in ("start", "end")), rgb(BLACK), spacing=4)
        if index < len(nodes) - 1:
            arrow(draw, (cx, bottom), (cx, layout[index + 1][0] - 4), BLACK, width=4, head=16)
            if kind == "decision":
                down_label = node[3] if len(node) > 3 else "Có"
                text_left(draw, cx + 16, bottom + 10, down_label, 200, font(22, True))

    if loop is not None:
        source, target, label = loop
        source_top, source_height = layout[source]
        target_top, target_height = layout[target]
        sy = source_top + source_height / 2
        ty = target_top + target_height / 2
        rail = 250
        draw.line((cx - main_half, sy, rail, sy), fill=rgb(BLACK), width=4)
        draw.line((rail, sy, rail, ty), fill=rgb(BLACK), width=4)
        arrow(draw, (rail, ty), (cx - main_half - 4, ty), BLACK, width=4, head=16)
        label_top = (sy + ty) / 2 - LINE_HEIGHT
        text_right(draw, rail - 14, label_top, label, 180, font(22, True))
    return save_image(image, file_name)
