"""Sơ đồ Chương 3–6 ở mức phân tích và thiết kế chức năng (theo docs/bao-cao/update.md).

Các hình chỉ gồm tác nhân, Client, Server, CSDL, thành phần chức năng, trạng thái, luồng dữ liệu
và nhánh xử lý chính; không có tên lớp, hàm hay chi tiết cài đặt. Mỗi hàm diagram_* vẽ một hình
và trả về đường dẫn PNG trong generated-report-assets/.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

from report_common import BLACK, WHITE, arrow, font, orthogonal_arrow, rgb, round_box, save_image, text_centered, wrapped_lines
from report_diagrams import dashed_line

WIDTH = 1500
TEXT = 28
SMALL = 24
LINE = 36
TITLE = 40
SUBTITLE = 26


def _measure() -> ImageDraw.ImageDraw:
    return ImageDraw.Draw(Image.new("RGB", (10, 10)))


def _lines(text: str, max_width: float, size: int = TEXT, bold: bool = False) -> list[str]:
    return wrapped_lines(_measure(), text, font(size, bold), int(max_width))


def _header_height(subtitle: str) -> int:
    return 118 + 34 * len(_lines(subtitle, WIDTH - 140, SUBTITLE))


def new_canvas(title: str, subtitle: str, height: int):
    """White page with a thin frame, the figure name and a one-line note; returns (image, draw, top)."""
    image = Image.new("RGB", (WIDTH, height), rgb(WHITE))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((18, 18, WIDTH - 18, height - 18), radius=22, outline=rgb(BLACK), width=3)
    draw.text((56, 38), title, font=font(TITLE, True), fill=rgb(BLACK))
    y = 100
    for line in _lines(subtitle, WIDTH - 140, SUBTITLE):
        draw.text((56, y), line, font=font(SUBTITLE), fill=rgb(BLACK))
        y += 34
    rule = _header_height(subtitle) - 14
    draw.line((56, rule, WIDTH - 56, rule), fill=rgb(BLACK), width=3)
    return image, draw, rule + 34


def box(draw, rect, text: str, bold: bool = False, size: int = TEXT, radius: int = 14, width: int = 3):
    round_box(draw, rect, rgb(WHITE), rgb(BLACK), radius=radius, width=width)
    text_centered(draw, rect, text, font(size, bold), rgb(BLACK), spacing=6)


def pill(draw, rect, text: str, size: int = TEXT):
    box(draw, rect, text, bold=True, size=size, radius=(rect[3] - rect[1]) // 2, width=4)


def diamond(draw, center, half_width: int, half_height: int, text: str, size: int = TEXT):
    cx, cy = center
    points = [(cx, cy - half_height), (cx + half_width, cy), (cx, cy + half_height), (cx - half_width, cy)]
    draw.polygon(points, fill=rgb(WHITE))
    draw.line(points + [points[0]], fill=rgb(BLACK), width=4)
    text_centered(draw, (cx - half_width * 0.62, cy - half_height, cx + half_width * 0.62, cy + half_height), text, font(size), rgb(BLACK), spacing=4)


def dashed_frame(draw, rect, title: str | None = None):
    x1, y1, x2, y2 = rect
    for start, end in (((x1, y1), (x2, y1)), ((x2, y1), (x2, y2)), ((x2, y2), (x1, y2)), ((x1, y2), (x1, y1))):
        dashed_line(draw, start, end, width=3, dash=16, gap=10)
    if title:
        text_centered(draw, (x1, y1 + 8, x2, y1 + 52), title, font(SMALL, True), rgb(BLACK))


def label(draw, rect, text: str, bold: bool = False, size: int = SMALL):
    """Short arrow label on a white backing so it never hides behind a line."""
    chosen = font(size, bold)
    x1, y1, x2, y2 = rect
    lines = wrapped_lines(draw, text, chosen, int(x2 - x1))
    heights = [draw.textbbox((0, 0), line, font=chosen)[3] for line in lines]
    y = y1 + (y2 - y1 - (sum(heights) + 4 * (len(lines) - 1))) / 2
    for line, height in zip(lines, heights):
        line_width = draw.textbbox((0, 0), line, font=chosen)[2]
        x = x1 + (x2 - x1 - line_width) / 2
        draw.rectangle((x - 5, y - 2, x + line_width + 5, y + height + 4), fill=rgb(WHITE))
        draw.text((x, y), line, font=chosen, fill=rgb(BLACK))
        y += height + 4


def link(draw, start, end, width: int = 4):
    arrow(draw, start, end, BLACK, width=width, head=18)


def path(draw, points, width: int = 4):
    orthogonal_arrow(draw, points, BLACK, width=width, head=18)


def sequence(file_name: str, title: str, subtitle: str, participants: list[str], steps: list[tuple]) -> Path:
    """Sequence diagram at analysis level. Steps:
    ("msg", a, b, text) request · ("reply", a, b, text) answer/push (dashed) ·
    ("act", a, text) processing step on one participant · ("note", text[, a, b]) condition or phase."""
    left, right = 56, WIDTH - 56
    column = (right - left) / len(participants)
    xs = [left + column * (index + 0.5) for index in range(len(participants))]
    action_width = min(column * 1.1, 560)

    def span(a, b):
        return max(abs(xs[b] - xs[a]) - 40, 300)

    def note_bounds(step):
        if len(step) > 2:
            return xs[step[2]] - column * 0.47, xs[step[3]] + column * 0.47
        return left + 6, right - 6

    heights = []
    for step in steps:
        kind = step[0]
        if kind in ("msg", "reply"):
            heights.append(LINE * len(_lines("00. " + step[3], span(step[1], step[2]))) + 46)
        elif kind == "act":
            heights.append(LINE * len(_lines("00. " + step[2], action_width - 36)) + 58)
        else:
            x1, x2 = note_bounds(step)
            heights.append(LINE * len(_lines(step[1], x2 - x1 - 40, SMALL, True)) + 48)

    top = _header_height(subtitle) + 20
    head_height = 96
    first = top + head_height + 34
    height = first + sum(heights) + 48
    image, draw, _ = new_canvas(title, subtitle, height)

    head_width = min(column - 30, 380)
    for x, name in zip(xs, participants):
        rect = (x - head_width / 2, top, x + head_width / 2, top + head_height)
        box(draw, rect, name, bold=True)
        dashed_line(draw, (x, top + head_height), (x, height - 42), width=2, dash=12, gap=10)

    y = first
    number = 0
    for step, step_height in zip(steps, heights):
        kind = step[0]
        if kind in ("msg", "reply"):
            number += 1
            a, b, text = step[1], step[2], step[3]
            lines = _lines(f"{number}. {text}", span(a, b))
            label(draw, (min(xs[a], xs[b]) + 20, y, max(xs[a], xs[b]) - 20, y + LINE * len(lines)), f"{number}. {text}", size=TEXT)
            arrow_y = y + LINE * len(lines) + 14
            direction = 1 if xs[b] > xs[a] else -1
            if kind == "msg":
                link(draw, (xs[a], arrow_y), (xs[b] - direction * 4, arrow_y))
            else:
                dashed_line(draw, (xs[a], arrow_y), (xs[b] - direction * 22, arrow_y), width=3)
                arrow(draw, (xs[b] - direction * 24, arrow_y), (xs[b] - direction * 4, arrow_y), BLACK, width=3, head=18)
        elif kind == "act":
            number += 1
            a, text = step[1], step[2]
            rect = (xs[a] - action_width / 2, y + 8, xs[a] + action_width / 2, y + step_height - 12)
            draw.rectangle(rect, fill=rgb(WHITE), outline=rgb(BLACK), width=3)
            text_centered(draw, rect, f"{number}. {text}", font(TEXT), rgb(BLACK), spacing=6)
        else:
            x1, x2 = note_bounds(step)
            rect = (x1, y + 6, x2, y + step_height - 10)
            draw.rectangle(rect, fill=rgb(WHITE), outline=rgb(BLACK), width=2)
            text_centered(draw, rect, step[1], font(SMALL, True), rgb(BLACK), spacing=4)
        y += step_height
    return save_image(image, file_name)


def vertical_flow(file_name: str, title: str, subtitle: str, nodes: list[tuple]) -> Path:
    """Top-down activity flow. Nodes: ("start"|"step"|"end", text) or
    ("decision", question, side_text, down_label, side_label); the side branch ends to the right."""
    cx, half = 520, 270
    side_x1, side_x2 = cx + half + 130, WIDTH - 60
    gap = 56

    def box_height(text, width):
        return max(LINE * len(_lines(text, width - 36)) + 40, 90)

    layout = []
    y = _header_height(subtitle) + 20
    for node in nodes:
        if node[0] == "decision":
            node_height = max(LINE * len(_lines(node[1], half * 1.2)) + 96, 160)
        else:
            node_height = box_height(node[1], 2 * half)
        layout.append((y, node_height))
        y += node_height + gap
    height = y - gap + 56
    image, draw, _ = new_canvas(title, subtitle, height)

    for index, (node, (top, node_height)) in enumerate(zip(nodes, layout)):
        bottom = top + node_height
        if node[0] == "decision":
            middle = top + node_height / 2
            diamond(draw, (cx, middle), half + 20, node_height // 2, node[1])
            side_height = box_height(node[2], side_x2 - side_x1)
            side = (side_x1, middle - side_height / 2, side_x2, middle + side_height / 2)
            box(draw, side, node[2])
            link(draw, (cx + half + 20, middle), (side_x1 - 4, middle))
            label(draw, (cx + half + 24, middle - 44, side_x1 - 6, middle - 8), node[4], bold=True)
        else:
            rect = (cx - half, top, cx + half, bottom)
            if node[0] == "step":
                box(draw, rect, node[1])
            else:
                pill(draw, rect, node[1])
        if index < len(nodes) - 1:
            link(draw, (cx, bottom), (cx, layout[index + 1][0] - 4))
            if node[0] == "decision":
                label(draw, (cx + 14, bottom + 8, cx + 160, bottom + 40), node[3], bold=True)
    return save_image(image, file_name)


# ---------------------------------------------------------------- Chương 3

def diagram_3_1_khung_thong_diep() -> Path:
    image, draw, top = new_canvas(
        "CẤU TRÚC KHUNG THÔNG ĐIỆP VÀ QUÁ TRÌNH GỬI, NHẬN",
        "Mỗi thông điệp được đóng thành một khung gồm độ dài và nội dung để bên nhận tách đúng từng thông điệp",
        900,
    )
    frame_length = (80, top + 10, 460, top + 130)
    frame_body = (460, top + 10, 1420, top + 130)
    for rect in (frame_length, frame_body):
        draw.rectangle(rect, fill=rgb(WHITE), outline=rgb(BLACK), width=4)
    text_centered(draw, frame_length, "Độ dài\n(4 byte)", font(TEXT, True), rgb(BLACK))
    text_centered(draw, frame_body, "Nội dung JSON\nloại thông điệp · mã yêu cầu · dữ liệu", font(TEXT, True), rgb(BLACK))
    label(draw, (80, top + 138, 1420, top + 172), "Một khung thông điệp")

    columns = [80, 440, 800, 1160]
    width = 260
    rows = {
        "send": (top + 250, "BÊN GỬI", ["Thông điệp", "Chuyển thành\ndữ liệu JSON", "Đóng khung\n[Độ dài + Nội dung]", "Gửi qua TCP"]),
        "receive": (top + 540, "BÊN NHẬN", ["Giải mã\nthông điệp", "Đọc nội dung", "Xác định độ dài", "Nhận dữ liệu\nTCP"]),
    }
    for key, (y, heading, steps) in rows.items():
        draw.text((80, y - 48), heading, font=font(TEXT, True), fill=rgb(BLACK))
        rects = [(x, y, x + width, y + 130) for x in columns]
        for rect, text in zip(rects, steps):
            box(draw, rect, text)
        for first, second in zip(rects, rects[1:]):
            if key == "send":
                link(draw, (first[2] + 4, y + 65), (second[0] - 4, y + 65))
            else:
                link(draw, (second[0] - 4, y + 65), (first[2] + 4, y + 65))
    x = columns[-1] + width / 2
    link(draw, (x, top + 380), (x, top + 536))
    label(draw, (x + 14, top + 430, x + 190, top + 480), "Kết nối TCP", bold=True)
    return save_image(image, "chuc-nang-3-1-khung-thong-diep.png")


def diagram_3_2_xu_ly_ket_noi() -> Path:
    image, draw, top = new_canvas(
        "MÔ HÌNH XỬ LÝ KẾT NỐI PHÍA SERVER",
        "Một Server phục vụ nhiều Client đồng thời; mỗi Client được quản lý bằng một phiên kết nối riêng",
        990,
    )
    server = (330, top + 10, WIDTH - 50, top + 650)
    draw.rounded_rectangle(server, radius=20, outline=rgb(BLACK), width=4)
    draw.text((server[0] + 24, server[1] + 14), "SERVER", font=font(TEXT, True), fill=rgb(BLACK))

    accept = (380, top + 70, 880, top + 160)
    box(draw, accept, "Tiếp nhận kết nối TCP", bold=True)
    group = (380, top + 210, 880, top + 630)
    dashed_frame(draw, group, "Quản lý phiên kết nối")
    link(draw, (630, accept[3]), (630, group[1] - 4))

    session_rows = [top + 270, top + 390, top + 510]
    for index, y in enumerate(session_rows):
        name = f"Client {index + 1}" if index < 2 else "Client n"
        client = (56, y, 276, y + 90)
        session = (430, y, 830, y + 90)
        box(draw, client, name, bold=True)
        box(draw, session, f"Phiên {index + 1 if index < 2 else 'n'}")
        link(draw, (client[2] + 4, y + 30), (session[0] - 4, y + 30), width=3)
        link(draw, (session[0] - 4, y + 60), (client[2] + 4, y + 60), width=3)

    route = (940, top + 320, 1150, top + 480)
    box(draw, route, "Xác định loại yêu cầu", bold=True)
    for y, target_y in zip(session_rows, (top + 355, top + 400, top + 445)):
        link(draw, (830, y + 45), (route[0] - 4, target_y), width=3)

    functions = ["Xác thực", "Sảnh", "Phòng", "Trò chơi", "Lịch sử / xếp hạng"]
    for index, name in enumerate(functions):
        y = top + 70 + index * 112
        target = (1210, y, WIDTH - 80, y + 86)
        box(draw, target, name)
        link(draw, (route[2], (route[1] + route[3]) / 2), (target[0] - 4, y + 43), width=3)
    label(
        draw,
        (60, top + 690, WIDTH - 60, top + 760),
        "Các phiên hoạt động đồng thời và độc lập: yêu cầu của Client nào được xử lý trong phiên của Client đó.",
        size=SMALL,
    )
    return save_image(image, "chuc-nang-3-2-xu-ly-ket-noi-server.png")


def diagram_3_3_ket_noi_phan_hoi() -> Path:
    return sequence(
        "chuc-nang-3-3-ket-noi-phan-hoi.png",
        "TRÌNH TỰ KẾT NỐI VÀ NHẬN PHẢN HỒI",
        "Mỗi yêu cầu mang một mã yêu cầu; Client dùng mã này để biết phản hồi nhận được thuộc yêu cầu nào",
        ["Người dùng", "Client", "Server"],
        [
            ("msg", 0, 1, "Thực hiện thao tác"),
            ("msg", 1, 2, "Mở kết nối TCP (nếu chưa có)"),
            ("reply", 2, 1, "Chấp nhận kết nối, tạo phiên"),
            ("act", 1, "Tạo yêu cầu kèm mã yêu cầu"),
            ("msg", 1, 2, "Gửi yêu cầu"),
            ("act", 2, "Nhận và xử lý yêu cầu"),
            ("reply", 2, 1, "Gửi phản hồi kèm mã yêu cầu"),
            ("act", 1, "Xác định phản hồi tương ứng"),
            ("reply", 1, 0, "Cập nhật giao diện"),
            ("note", "Server không phản hồi hoặc mất kết nối: Client thông báo lỗi cho người dùng", 0, 1),
        ],
    )


def diagram_3_4_dang_nhap() -> Path:
    return sequence(
        "chuc-nang-3-4-dang-nhap.png",
        "TRÌNH TỰ ĐĂNG NHẬP",
        "Server kiểm tra tài khoản, chặn đăng nhập trùng và gắn người dùng với phiên kết nối hiện tại",
        ["Người dùng", "Client", "Server", "CSDL"],
        [
            ("msg", 0, 1, "Nhập tên đăng nhập và mật khẩu"),
            ("msg", 1, 2, "Gửi yêu cầu đăng nhập"),
            ("msg", 2, 3, "Tìm tài khoản"),
            ("reply", 3, 2, "Thông tin tài khoản"),
            ("act", 2, "Kiểm tra mật khẩu"),
            ("act", 2, "Kiểm tra trạng thái trực tuyến"),
            ("act", 2, "Tạo phiên người dùng"),
            ("reply", 2, 1, "Trả kết quả đăng nhập"),
            ("reply", 1, 0, "Chuyển sang màn hình chính"),
            ("note", "Sai thông tin hoặc tài khoản đang trực tuyến ở nơi khác: Server trả lỗi, Client hiển thị thông báo", 1, 2),
        ],
    )


def diagram_3_5_mat_ket_noi() -> Path:
    return vertical_flow(
        "chuc-nang-3-5-mat-ket-noi.png",
        "LUỒNG XỬ LÝ MẤT KẾT NỐI",
        "Server dọn trạng thái của người dùng khi kết nối bị ngắt và thông báo thay đổi tới các Client liên quan",
        [
            ("start", "Kết nối của một Client bị mất"),
            ("step", "Server phát hiện kết nối bị đóng"),
            ("decision", "Phiên đã đăng nhập?", "Xóa phiên kết nối", "Có", "Không"),
            ("step", "Xác định người dùng của phiên"),
            ("step", "Xử lý phòng và trận: rời phòng; nếu đang chơi, người chơi bị loại"),
            ("step", "Xóa trạng thái trực tuyến"),
            ("end", "Cập nhật các Client liên quan"),
        ],
    )


# ---------------------------------------------------------------- Chương 4

def _state_row(draw, top: int, heading: str, states: list[str], forward: list[str], back: list[tuple[int, int, str]]):
    draw.text((70, top), heading, font=font(TEXT, True), fill=rgb(BLACK))
    xs = [150, 620, 1090]
    box_top, box_bottom = top + 120, top + 220
    rects = [(x, box_top, x + 280, box_bottom) for x in xs]
    for rect, state in zip(rects, states):
        box(draw, rect, state, bold=True, radius=48, width=4)
    for index, text in enumerate(forward):
        first, second = rects[index], rects[index + 1]
        link(draw, (first[2] + 4, box_top + 50), (second[0] - 4, box_top + 50))
        label(draw, (first[2] + 6, box_top - 50, second[0] - 6, box_top + 40), text)
    for depth, (source, target, text) in enumerate(back):
        sx = (rects[source][0] + rects[source][2]) / 2 - 40
        tx = (rects[target][0] + rects[target][2]) / 2 + 40
        low = box_bottom + 60 + depth * 70
        draw.line((sx, box_bottom, sx, low), fill=rgb(BLACK), width=4)
        draw.line((sx, low, tx, low), fill=rgb(BLACK), width=4)
        link(draw, (tx, low), (tx, box_bottom + 4))
        label(draw, (min(sx, tx) + 10, low + 6, max(sx, tx) - 10, low + 40), text)


def diagram_4_1_trang_thai() -> Path:
    image, draw, top = new_canvas(
        "TRẠNG THÁI NGƯỜI CHƠI VÀ TRẠNG THÁI PHÒNG",
        "Server quản lý hai trạng thái này; các sự kiện của người chơi làm trạng thái thay đổi",
        1020,
    )
    _state_row(
        draw,
        top,
        "NGƯỜI CHƠI",
        ["FREE", "IN_ROOM", "PLAYING"],
        ["Vào phòng", "Bắt đầu trận"],
        [(2, 1, "Kết thúc trận"), (1, 0, "Rời phòng")],
    )
    _state_row(
        draw,
        top + 420,
        "PHÒNG",
        ["WAITING", "PLAYING", "FINISHED"],
        ["Bắt đầu trận", "Kết thúc trận"],
        [(2, 0, "Chơi lại")],
    )
    return save_image(image, "chuc-nang-4-1-trang-thai.png")


def diagram_4_2_dong_bo_sanh() -> Path:
    return sequence(
        "chuc-nang-4-2-dong-bo-sanh.png",
        "ĐỒNG BỘ SẢNH",
        "Client nhận danh sách khi mở sảnh; khi sảnh thay đổi, Server chủ động gửi danh sách mới",
        ["Client A", "Server", "Các Client ở sảnh"],
        [
            ("note", "Khi mở màn hình sảnh"),
            ("msg", 0, 1, "Yêu cầu danh sách người trực tuyến và phòng"),
            ("act", 1, "Lấy danh sách người trực tuyến và phòng"),
            ("reply", 1, 0, "Trả danh sách"),
            ("act", 0, "Hiển thị sảnh"),
            ("note", "Khi sảnh thay đổi: đăng nhập, đăng xuất, tạo, vào hoặc rời phòng, kết thúc trận"),
            ("act", 1, "Tạo danh sách mới"),
            ("reply", 1, 2, "Chủ động gửi danh sách mới"),
            ("act", 2, "Cập nhật sảnh"),
        ],
    )


def diagram_4_3_tao_tham_gia_phong() -> Path:
    return sequence(
        "chuc-nang-4-3-tao-tham-gia-phong.png",
        "TẠO PHÒNG VÀ THAM GIA PHÒNG",
        "Sau mỗi thay đổi, Server gửi trạng thái phòng mới tới tất cả thành viên trong phòng",
        ["Client 1", "Server", "Client 2"],
        [
            ("msg", 0, 1, "Yêu cầu tạo phòng"),
            ("act", 1, "Tạo phòng, Client 1 là chủ phòng"),
            ("reply", 1, 0, "Trạng thái phòng"),
            ("act", 0, "Vào phòng chờ"),
            ("msg", 2, 1, "Yêu cầu tham gia phòng"),
            ("act", 1, "Kiểm tra phòng: còn tồn tại, đang chờ, chưa đủ 4 người"),
            ("act", 1, "Thêm Client 2 vào phòng"),
            ("reply", 1, 0, "Trạng thái phòng mới"),
            ("reply", 1, 2, "Trạng thái phòng mới"),
        ],
    )


def diagram_4_4_choi_nhanh() -> Path:
    return vertical_flow(
        "chuc-nang-4-4-choi-nhanh.png",
        "LUỒNG CHƠI NHANH",
        "Client tự chọn phòng phù hợp; Server vẫn là nơi quyết định việc vào phòng có thành công hay không",
        [
            ("start", "Người chơi chọn PLAY"),
            ("step", "Tìm phòng đang chờ và còn chỗ"),
            ("decision", "Có phòng phù hợp?", "Tạo phòng mới", "Có", "Không"),
            ("step", "Gửi yêu cầu tham gia phòng"),
            ("decision", "Server chấp nhận?", "Thử phòng khác (tối đa 2 phòng), không được thì tạo phòng mới", "Có", "Không"),
            ("end", "Chuyển vào phòng chờ"),
        ],
    )


def diagram_4_5_san_sang_bat_dau() -> Path:
    return sequence(
        "chuc-nang-4-5-san-sang-bat-dau.png",
        "SẴN SÀNG VÀ BẮT ĐẦU TRẬN",
        "Chỉ chủ phòng được bắt đầu; Server kiểm tra lại điều kiện trước khi chuyển phòng sang trạng thái thi đấu",
        ["Chủ phòng", "Server", "Người chơi khác"],
        [
            ("msg", 2, 1, "Sẵn sàng (READY)"),
            ("act", 1, "Cập nhật trạng thái sẵn sàng"),
            ("reply", 1, 0, "Trạng thái phòng mới"),
            ("reply", 1, 2, "Trạng thái phòng mới"),
            ("msg", 0, 1, "Bắt đầu trận (START)"),
            ("act", 1, "Kiểm tra: là chủ phòng, ít nhất 2 người, tất cả đã sẵn sàng"),
            ("act", 1, "Chuyển phòng sang PLAYING, khởi tạo trận"),
            ("reply", 1, 0, "Trạng thái phòng: PLAYING"),
            ("reply", 1, 2, "Trạng thái phòng: PLAYING"),
            ("note", "Các Client chuyển sang màn hình GAME"),
        ],
    )


# ---------------------------------------------------------------- Chương 5

def diagram_5_1_thanh_phan() -> Path:
    image, draw, top = new_canvas(
        "THÀNH PHẦN GAME ENGINE",
        "Mỗi phòng đang thi đấu có bộ xử lý trận và trạng thái trận riêng; Server chuyển thao tác tới đúng phòng",
        1000,
    )
    rooms = [("A", top + 90), ("B", top + 420)]
    receive = (430, top + 300, 690, top + 470)
    box(draw, receive, "Server tiếp nhận thao tác", bold=True)
    for index, (room, y) in enumerate(rooms):
        clients = (56, y + 50, 330, y + 230)
        engine = (790, y, WIDTH - 56, y + 280)
        box(draw, clients, f"Các Client\nphòng {room}", bold=True)
        draw.rounded_rectangle(engine, radius=18, outline=rgb(BLACK), width=4)
        text_centered(draw, (engine[0], y + 10, engine[2], y + 60), f"Bộ xử lý trận - phòng {room}", font(TEXT, True), rgb(BLACK))
        state = (engine[0] + 30, y + 80, engine[2] - 30, y + 255)
        draw.rectangle(state, outline=rgb(BLACK), width=2)
        text_centered(draw, (state[0], state[1] + 4, state[2], state[1] + 44), "Trạng thái trận", font(SMALL, True), rgb(BLACK))
        for part_index, part in enumerate(["Bản đồ", "Người chơi", "Bom", "Vụ nổ"]):
            x = state[0] + 20 + part_index * 150
            box(draw, (x, state[1] + 60, x + 130, state[1] + 150), part, size=SMALL)
        middle = (receive[1] + receive[3]) / 2
        link(draw, (clients[2] + 4, (clients[1] + clients[3]) / 2), (receive[0] - 4, middle - 30 + index * 60))
        link(draw, (receive[2] + 4, middle - 30 + index * 60), (engine[0] - 4, y + 140))
        center = (clients[0] + clients[2]) / 2
        if index == 0:
            rail = top + 30
            path(draw, [(1040, engine[1]), (1040, rail), (center, rail), (center, clients[1] - 4)])
            label(draw, (360, rail - 20, 760, rail + 20), "Gửi trạng thái mới")
        else:
            rail = engine[3] + 40
            path(draw, [(1040, engine[3]), (1040, rail), (center, rail), (center, clients[3] + 4)])
            label(draw, (360, rail - 20, 760, rail + 20), "Gửi trạng thái mới")
    label(draw, (340, top + 230, 700, top + 290), "Gửi thao tác")
    return save_image(image, "chuc-nang-5-1-thanh-phan-game-engine.png")


def diagram_5_2_di_chuyen() -> Path:
    return sequence(
        "chuc-nang-5-2-di-chuyen.png",
        "XỬ LÝ THAO TÁC DI CHUYỂN",
        "Client chỉ gửi hướng di chuyển; vị trí mới do Server quyết định và được đồng bộ tới các Client",
        ["Người chơi", "Client", "Server", "Game Engine (trận của phòng)"],
        [
            ("msg", 0, 1, "Nhấn phím di chuyển"),
            ("msg", 1, 2, "Gửi hướng di chuyển"),
            ("act", 2, "Kiểm tra thao tác: đúng phòng, trận đang diễn ra, người chơi còn sống"),
            ("msg", 2, 3, "Chuyển thao tác tới trận"),
            ("act", 3, "Kiểm tra ô đích: trong bản đồ, không có tường, bom hoặc người chơi khác"),
            ("act", 3, "Hợp lệ: cập nhật vị trí; không hợp lệ: giữ nguyên"),
            ("reply", 3, 2, "Trạng thái trận mới"),
            ("reply", 2, 1, "Gửi trạng thái mới tới các Client trong phòng"),
            ("reply", 1, 0, "Hiển thị vị trí mới"),
        ],
    )


def diagram_5_3_chu_ky() -> Path:
    image, draw, top = new_canvas(
        "CHU KỲ XỬ LÝ TRẬN",
        "Trong khi trận diễn ra, Server lặp lại chu kỳ này theo nhịp cố định cho từng phòng",
        900,
    )
    columns = [60, 420, 780, 1140]
    width, height = 300, 130
    row1, row2 = top + 60, top + 360
    cycle_start = (columns[0], row1, columns[0] + width, row1 + height)
    pill(draw, cycle_start, "Bắt đầu chu kỳ")
    top_steps = ["Tiếp nhận thao tác\nđang chờ", "Xử lý di chuyển,\nđặt bom", "Cập nhật bom\nvà vụ nổ"]
    top_rects = [cycle_start] + [(x, row1, x + width, row1 + height) for x in columns[1:]]
    for rect, text in zip(top_rects[1:], top_steps):
        box(draw, rect, text)
    for first, second in zip(top_rects, top_rects[1:]):
        link(draw, (first[2] + 4, row1 + height / 2), (second[0] - 4, row1 + height / 2))

    eliminated = (columns[3], row2, columns[3] + width, row2 + height)
    box(draw, eliminated, "Kiểm tra người chơi bị loại")
    link(draw, (columns[3] + width / 2, row1 + height), (columns[3] + width / 2, row2 - 4))
    decision_center = (columns[2] + width / 2, row2 + height / 2)
    diamond(draw, decision_center, 175, 95, "Đã có kết quả?")
    link(draw, (eliminated[0] - 4, row2 + height / 2), (decision_center[0] + 179, row2 + height / 2))
    send = (columns[1], row2, columns[1] + width, row2 + height)
    box(draw, send, "Gửi trạng thái mới tới các Client")
    link(draw, (decision_center[0] - 175, row2 + height / 2), (send[2] + 4, row2 + height / 2))
    label(draw, (send[2] + 8, row2 + 10, decision_center[0] - 178, row2 + 50), "Chưa", bold=True)
    path(draw, [(send[0], row2 + height / 2), (columns[0] + width / 2, row2 + height / 2), (columns[0] + width / 2, row1 + height + 4)])
    label(draw, (columns[0] + 10, row1 + height + 60, columns[0] + width - 10, row1 + height + 110), "Chu kỳ tiếp theo", bold=True)

    finish = (decision_center[0] - 210, row2 + 230, decision_center[0] + 210, row2 + 340)
    pill(draw, finish, "Gửi kết quả, kết thúc trận")
    link(draw, (decision_center[0], row2 + height / 2 + 95), (decision_center[0], finish[1] - 4))
    label(draw, (decision_center[0] + 12, row2 + 175, decision_center[0] + 90, row2 + 215), "Có", bold=True)
    return save_image(image, "chuc-nang-5-3-chu-ky-xu-ly-tran.png")


def diagram_5_4_dat_bom() -> Path:
    return sequence(
        "chuc-nang-5-4-dat-bom.png",
        "ĐẶT BOM VÀ PHÁT NỔ",
        "Thời điểm nổ và vùng ảnh hưởng do Server tính; các Client chỉ hiển thị trạng thái nhận được",
        ["Client (người đặt bom)", "Server", "Game Engine", "Các Client trong phòng"],
        [
            ("msg", 0, 1, "Yêu cầu đặt bom"),
            ("act", 1, "Kiểm tra thao tác"),
            ("msg", 1, 2, "Chuyển yêu cầu tới trận"),
            ("act", 2, "Kiểm tra vị trí và số bom, tạo bom"),
            ("note", "Chờ tới thời gian nổ", 1, 2),
            ("act", 2, "Bom phát nổ, tính vùng ảnh hưởng"),
            ("act", 2, "Cập nhật bản đồ, người chơi, vụ nổ"),
            ("reply", 2, 1, "Trạng thái trận mới"),
            ("reply", 1, 3, "Gửi trạng thái mới"),
        ],
    )


def diagram_5_5_lan_lua() -> Path:
    image, draw, top = new_canvas(
        "LAN LỬA VÀ NỔ DÂY CHUYỀN",
        "Lửa lan từ vị trí bom theo bốn hướng, tối đa bằng tầm nổ; kết quả phụ thuộc vào ô mà lửa gặp",
        1010,
    )
    center_x = WIDTH / 2
    start = (center_x - 230, top + 20, center_x + 230, top + 110)
    spread = (center_x - 330, top + 170, center_x + 330, top + 270)
    pill(draw, start, "Bom phát nổ")
    box(draw, spread, "Lan theo bốn hướng: lên, xuống, trái, phải")
    link(draw, (center_x, start[3]), (center_x, spread[1] - 4))
    cases = [
        ("Ô trống", "Tiếp tục lan sang ô kế tiếp"),
        ("Tường cứng", "Dừng lan theo hướng này"),
        ("Tường phá được", "Phá tường và dừng"),
        ("Bom khác", "Kích hoạt bom đó\n(nổ dây chuyền)"),
        ("Người chơi", "Người chơi bị loại"),
    ]
    width, gap = 250, 28
    x = (WIDTH - (len(cases) * width + (len(cases) - 1) * gap)) / 2
    bus = top + 330
    draw.line((x + width / 2, bus, x + (len(cases) - 1) * (width + gap) + width / 2, bus), fill=rgb(BLACK), width=4)
    draw.line((center_x, spread[3], center_x, bus), fill=rgb(BLACK), width=4)
    for condition, outcome in cases:
        case = (x, top + 400, x + width, top + 500)
        result = (x, top + 590, x + width, top + 720)
        box(draw, case, "Gặp " + condition.lower() if condition != "Ô trống" else condition, bold=True)
        box(draw, result, outcome)
        link(draw, (x + width / 2, bus), (x + width / 2, case[1] - 4))
        link(draw, (x + width / 2, case[3]), (x + width / 2, result[1] - 4))
        x += width + gap
    label(draw, (60, top + 750, WIDTH - 60, top + 790), "Mỗi hướng dừng khi hết tầm nổ, gặp tường hoặc gặp bom khác; bom bị kích hoạt nổ ngay trong cùng lúc.")
    return save_image(image, "chuc-nang-5-5-lan-lua.png")


def diagram_5_6_ket_qua() -> Path:
    image, draw, top = new_canvas(
        "XÁC ĐỊNH KẾT QUẢ TRẬN",
        "Kết quả được kiểm tra lại mỗi khi có người chơi bị loại",
        1080,
    )
    center_x = WIDTH / 2
    start = (center_x - 260, top + 20, center_x + 260, top + 110)
    count = (center_x - 260, top + 170, center_x + 260, top + 270)
    pill(draw, start, "Có người chơi bị loại")
    box(draw, count, "Kiểm tra số người còn sống")
    link(draw, (center_x, start[3]), (center_x, count[1] - 4))
    branches = [(250, "> 1", "Tiếp tục trận"), (center_x, "= 1", "Người còn lại thắng"), (WIDTH - 250, "= 0", "Hòa")]
    bus = top + 330
    draw.line((250, bus, WIDTH - 250, bus), fill=rgb(BLACK), width=4)
    draw.line((center_x, count[3], center_x, bus), fill=rgb(BLACK), width=4)
    results = []
    for x, condition, outcome in branches:
        rect = (x - 190, top + 410, x + 190, top + 510)
        box(draw, rect, outcome, bold=True)
        link(draw, (x, bus), (x, rect[1] - 4))
        label(draw, (x + 10, bus + 14, x + 90, bus + 54), condition, bold=True, size=TEXT)
        results.append(rect)
    send = (center_x + 180, top + 600, WIDTH - 60, top + 690)
    finish = (center_x + 250, top + 760, WIDTH - 130, top + 850)
    box(draw, send, "Gửi kết quả tới các Client")
    pill(draw, finish, "Kết thúc trận")
    send_middle = (send[1] + send[3]) / 2
    path(draw, [(center_x, results[1][3]), (center_x, send_middle), (send[0] - 4, send_middle)])
    link(draw, (WIDTH - 250, results[2][3]), (WIDTH - 250, send[1] - 4))
    link(draw, ((send[0] + send[2]) / 2 + 25, send[3]), ((send[0] + send[2]) / 2 + 25, finish[1] - 4))
    return save_image(image, "chuc-nang-5-6-ket-qua-tran.png")


def diagram_5_7_client() -> Path:
    image, draw, top = new_canvas(
        "XỬ LÝ HIỂN THỊ PHÍA CLIENT",
        "Client gửi thao tác và vẽ lại màn hình từ trạng thái Server gửi về; Client không tự quyết định kết quả",
        840,
    )
    lanes = [("CLIENT", top + 20, top + 260), ("SERVER", top + 300, top + 520)]
    for name, y1, y2 in lanes:
        draw.rectangle((56, y1, WIDTH - 56, y2), outline=rgb(BLACK), width=2)
        draw.text((72, y1 + 12), name, font=font(SMALL, True), fill=rgb(BLACK))
    width, gap = 200, 28
    columns = [80 + index * (width + gap) for index in range(6)]
    client_y, server_y = top + 90, top + 360
    steps = [
        (0, client_y, "Người chơi nhập bàn phím"),
        (1, client_y, "Gửi thao tác"),
        (2, server_y, "Server kiểm tra và xử lý"),
        (3, client_y, "Nhận trạng thái mới"),
        (4, client_y, "Cập nhật bản đồ, người chơi, bom, vụ nổ"),
        (5, client_y, "Hiển thị khung hình mới"),
    ]
    rects = []
    for column, y, text in steps:
        rect = (columns[column], y, columns[column] + width, y + 140)
        box(draw, rect, text, size=SMALL)
        rects.append(rect)
    link(draw, (rects[0][2] + 4, client_y + 70), (rects[1][0] - 4, client_y + 70))
    link(draw, ((rects[1][0] + rects[1][2]) / 2, rects[1][3]), (rects[2][0] + 20, rects[2][1] - 4))
    link(draw, (rects[2][2] - 20, rects[2][1]), ((rects[3][0] + rects[3][2]) / 2, rects[3][3] + 4))
    link(draw, (rects[3][2] + 4, client_y + 70), (rects[4][0] - 4, client_y + 70))
    link(draw, (rects[4][2] + 4, client_y + 70), (rects[5][0] - 4, client_y + 70))
    label(draw, (56, top + 560, WIDTH - 56, top + 600), "Chuyển động được làm mượt khi hiển thị, nhưng vị trí chính thức luôn do Server quyết định.")
    return save_image(image, "chuc-nang-5-7-xu-ly-client.png")


# ---------------------------------------------------------------- Chương 6

def diagram_6_1_phan_tang() -> Path:
    image, draw, top = new_canvas(
        "KIẾN TRÚC PHÂN TẦNG DESKTOP CLIENT",
        "Giao diện tách khỏi kết nối mạng; dữ liệu đi xuống khi người dùng thao tác và đi lên khi Server gửi về",
        1150,
    )
    user = (560, top + 10, 940, top + 90)
    pill(draw, user, "Người dùng")
    layers = [
        ("GIAO DIỆN", "Các màn hình: nhận thao tác, hiển thị dữ liệu"),
        ("TRẠNG THÁI CLIENT", "Dữ liệu đang hiển thị: người dùng, sảnh, phòng, trận"),
        ("XỬ LÝ ỨNG DỤNG", "Tạo yêu cầu, phân loại thông điệp nhận được, điều hướng"),
        ("KẾT NỐI MẠNG", "Duy trì kết nối TCP, gửi và nhận thông điệp"),
    ]
    y = top + 150
    for name, description in layers:
        rect = (300, y, 1200, y + 120)
        round_box(draw, rect, rgb(WHITE), rgb(BLACK), radius=14, width=4)
        text_centered(draw, (300, y + 10, 1200, y + 60), name, font(TEXT, True), rgb(BLACK))
        text_centered(draw, (300, y + 56, 1200, y + 112), description, font(SMALL), rgb(BLACK))
        y += 160
    server = (560, y + 20, 940, y + 110)
    pill(draw, server, "SERVER")
    link(draw, ((user[0] + user[2]) / 2, user[3]), ((user[0] + user[2]) / 2, top + 146))
    link(draw, (750, y - 40), (750, server[1] - 4))
    down_x, up_x = 200, 1300
    link(draw, (down_x, top + 150), (down_x, server[1] + 40))
    link(draw, (up_x, server[1] + 40), (up_x, top + 150))
    draw.line((down_x, server[1] + 40, server[0], server[1] + 40), fill=rgb(BLACK), width=2)
    draw.line((server[2], server[1] + 40, up_x, server[1] + 40), fill=rgb(BLACK), width=2)
    label(draw, (60, top + 420, 290, top + 520), "Chiều gửi\nNgười dùng → Server", bold=True)
    label(draw, (1210, top + 420, 1440, top + 520), "Chiều nhận\nServer → Giao diện", bold=True)
    return save_image(image, "chuc-nang-6-1-phan-tang-client.png")


def diagram_6_2_phan_phoi() -> Path:
    image, draw, top = new_canvas(
        "PHÂN PHỐI THÔNG ĐIỆP TỚI GIAO DIỆN",
        "Mỗi thông điệp nhận được làm thay đổi trạng thái Client, sau đó giao diện được cập nhật theo trạng thái mới",
        900,
    )
    columns = [70, 560, 1050]
    width, height = 380, 130
    row1, row2 = top + 40, top + 320
    first_row = ["Server gửi thông điệp", "Client nhận thông điệp", "Xác định loại dữ liệu"]
    rects = [(x, row1, x + width, row1 + height) for x in columns]
    for rect, text in zip(rects, first_row):
        box(draw, rect, text, bold=text.startswith("Server"))
    for a, b in zip(rects, rects[1:]):
        link(draw, (a[2] + 4, row1 + height / 2), (b[0] - 4, row1 + height / 2))
    update_state = (columns[2], row2, columns[2] + width, row2 + height)
    box(draw, update_state, "Cập nhật trạng thái Client")
    link(draw, (columns[2] + width / 2, row1 + height), (columns[2] + width / 2, row2 - 4))
    decision_center = (columns[1] + width / 2, row2 + height / 2)
    diamond(draw, decision_center, 200, 100, "Cần chuyển màn hình?")
    link(draw, (update_state[0] - 4, row2 + height / 2), (decision_center[0] + 204, row2 + height / 2))
    update_view = (columns[0], row2, columns[0] + width, row2 + height)
    pill(draw, update_view, "Cập nhật giao diện")
    link(draw, (decision_center[0] - 200, row2 + height / 2), (update_view[2] + 4, row2 + height / 2))
    label(draw, (update_view[2] + 6, row2 + 12, decision_center[0] - 204, row2 + 52), "Không", bold=True)
    navigate = (columns[1], row2 + 250, columns[1] + width, row2 + 360)
    box(draw, navigate, "Điều hướng tới màn hình phù hợp")
    link(draw, (decision_center[0], row2 + height / 2 + 100), (decision_center[0], navigate[1] - 4))
    label(draw, (decision_center[0] + 12, row2 + 185, decision_center[0] + 80, row2 + 225), "Có", bold=True)
    path(draw, [(navigate[0], (navigate[1] + navigate[3]) / 2), (columns[0] + width / 2, (navigate[1] + navigate[3]) / 2), (columns[0] + width / 2, update_view[3] + 4)])
    return save_image(image, "chuc-nang-6-2-phan-phoi-thong-diep.png")


def diagram_6_3_dieu_huong() -> Path:
    image, draw, top = new_canvas(
        "ĐIỀU HƯỚNG MÀN HÌNH",
        "Chuyển màn hình xảy ra khi người dùng thao tác hoặc khi Server gửi trạng thái mới",
        840,
    )
    y1, y2 = top + 130, top + 430
    screens = {
        "LOGIN": (56, y1, 296, y1 + 100),
        "HOME": (416, y1, 656, y1 + 100),
        "ROOM LIST": (776, y1, 1016, y1 + 100),
        "ROOM LOBBY": (1164, y1, WIDTH - 56, y1 + 100),
        "LEADERBOARD": (56, y2, 316, y2 + 100),
        "HISTORY": (376, y2, 616, y2 + 100),
        "RESULT": (776, y2, 1016, y2 + 100),
        "GAME": (1164, y2, WIDTH - 56, y2 + 100),
    }
    for name, rect in screens.items():
        box(draw, rect, name, bold=True, width=4)

    def right_mid(name):
        rect = screens[name]
        return rect[2], (rect[1] + rect[3]) / 2

    def left_mid(name):
        rect = screens[name]
        return rect[0], (rect[1] + rect[3]) / 2

    link(draw, (right_mid("LOGIN")[0] + 4, y1 + 50), (left_mid("HOME")[0] - 4, y1 + 50))
    label(draw, (270, y1 - 40, 442, y1 - 4), "Đăng nhập")
    link(draw, (right_mid("HOME")[0] + 4, y1 + 50), (left_mid("ROOM LIST")[0] - 4, y1 + 50))
    label(draw, (630, y1 - 40, 802, y1 - 4), "Xem phòng")
    link(draw, (right_mid("ROOM LIST")[0] + 4, y1 + 50), (left_mid("ROOM LOBBY")[0] - 4, y1 + 50))
    label(draw, (1020, y1 - 50, 1160, y1 + 40), "Tạo / vào phòng")
    path(draw, [(536, y1), (536, y1 - 80), (1306, y1 - 80), (1306, y1 - 4)])
    label(draw, (760, y1 - 118, 1080, y1 - 84), "Chơi nhanh")
    link(draw, (1306, y1 + 100), (1306, y2 - 4))
    label(draw, (1316, y1 + 150, WIDTH - 60, y1 + 230), "Bắt đầu trận")
    link(draw, (screens["GAME"][0] - 4, y2 + 50), (screens["RESULT"][2] + 4, y2 + 50))
    label(draw, (1020, y2 + 56, 1160, y2 + 130), "Kết thúc trận")
    link(draw, (986, y2), (1200, y1 + 104))
    label(draw, (1060, y1 + 190, 1190, y1 + 230), "Chơi lại")
    link(draw, (806, y2), (630, y1 + 104))
    label(draw, (640, y1 + 190, 780, y1 + 230), "Về HOME")
    link(draw, (476, y1 + 100), (186, y2 - 4))
    label(draw, (190, y1 + 170, 340, y1 + 210), "Xếp hạng")
    link(draw, (536, y1 + 100), (496, y2 - 4))
    label(draw, (520, y1 + 250, 640, y1 + 290), "Lịch sử")
    link(draw, (176, y1 - 80), (176, y1 - 4))
    label(draw, (40, y1 - 124, 420, y1 - 86), "Mất kết nối (từ mọi màn hình)", bold=True)
    label(
        draw,
        (56, y2 + 130, WIDTH - 56, y2 + 170),
        "LEADERBOARD, HISTORY và ROOM LIST có nút quay lại HOME; rời phòng cũng đưa người chơi về HOME.",
    )
    return save_image(image, "chuc-nang-6-3-dieu-huong-man-hinh.png")


def diagram_6_4_luu_ket_qua() -> Path:
    return sequence(
        "chuc-nang-6-4-luu-ket-qua.png",
        "LƯU KẾT QUẢ TRẬN ĐẤU",
        "Kết quả WIN / LOSS / DRAW được lưu theo từng người chơi trong CHI TIẾT TRẬN ĐẤU",
        ["Game Engine", "Server", "CSDL", "Client trong phòng"],
        [
            ("msg", 0, 1, "Thông báo trận đã có kết quả"),
            ("act", 1, "Tạo thông tin trận đấu: phòng, thời gian bắt đầu và kết thúc"),
            ("act", 1, "Xác định kết quả cá nhân và điểm của từng người"),
            ("msg", 1, 2, "Lưu TRẬN ĐẤU"),
            ("msg", 1, 2, "Lưu CHI TIẾT TRẬN ĐẤU của từng người chơi"),
            ("msg", 1, 2, "Cập nhật thống kê TÀI KHOẢN"),
            ("note", "Các bước lưu cùng thành công hoặc cùng bị hủy", 1, 2),
            ("reply", 1, 3, "Gửi kết quả trận"),
        ],
    )


def diagram_6_5_lich_su_xep_hang() -> Path:
    image, draw, top = new_canvas(
        "TẢI LỊCH SỬ VÀ BẢNG XẾP HẠNG",
        "Hai luồng độc lập, cùng đi qua Client, Server và CSDL; dữ liệu được tải lại mỗi khi mở màn hình",
        1180,
    )
    flows = [
        (
            "LỊCH SỬ",
            90,
            [
                ("CLIENT", "Yêu cầu lịch sử"),
                ("SERVER", "Xác định tài khoản từ phiên đăng nhập"),
                ("CSDL", "Lấy các trận người dùng đã tham gia"),
                ("SERVER", "Gửi danh sách trận"),
                ("CLIENT", "Hiển thị lịch sử"),
            ],
        ),
        (
            "BẢNG XẾP HẠNG",
            790,
            [
                ("CLIENT", "Yêu cầu bảng xếp hạng"),
                ("SERVER", "Lấy thống kê tài khoản"),
                ("CSDL", "Trả tổng điểm, số trận thắng, thua, hòa"),
                ("SERVER", "Sắp xếp thứ hạng, gửi bảng xếp hạng"),
                ("CLIENT", "Hiển thị bảng xếp hạng"),
            ],
        ),
    ]
    for heading, x, steps in flows:
        text_centered(draw, (x, top, x + 620, top + 50), heading, font(TEXT, True), rgb(BLACK))
        y = top + 70
        previous = None
        for actor, text in steps:
            rect = (x, y, x + 620, y + 130)
            round_box(draw, rect, rgb(WHITE), rgb(BLACK), radius=14, width=3)
            draw.line((x + 150, y, x + 150, y + 130), fill=rgb(BLACK), width=2)
            text_centered(draw, (x, y, x + 150, y + 130), actor, font(SMALL, True), rgb(BLACK))
            text_centered(draw, (x + 150, y, x + 620, y + 130), text, font(TEXT), rgb(BLACK), spacing=6)
            if previous is not None:
                link(draw, (x + 385, previous[3]), (x + 385, rect[1] - 4))
            previous = rect
            y += 180
    return save_image(image, "chuc-nang-6-5-lich-su-xep-hang.png")
