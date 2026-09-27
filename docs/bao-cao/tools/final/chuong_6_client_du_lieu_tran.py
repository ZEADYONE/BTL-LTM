"""Chương 6 - Kiến trúc Desktop Client và dữ liệu trận đấu (phần cá nhân D).

Sinh bởi docx_to_python.py từ bao_cao_nhom_bomberman-final.docx, sau đó có thể sửa tay.
Chạy lại docx_to_python.py sẽ ghi đè file này.
Cập nhật hình theo update.md (27/09/2026): vẽ lại sơ đồ ở mức phân tích chức năng (functional_diagrams.py).
Chỉnh tay sau khi sinh (27/09/2026): đánh số lại bảng (6.4 → 6.2, 6.5 → 6.3), trả cột Kết quả trận của người thua ở bảng điểm về WIN, sửa dấu câu.
"""
from __future__ import annotations

from report_common import (
    ASSET_DIR,
    SCREENSHOT_DIR,
    add_blank,
    add_body,
    add_figure,
    add_heading,
    add_table,
)


def write(doc):
    add_heading(doc, "CHƯƠNG 6. KIẾN TRÚC DESKTOP CLIENT VÀ DỮ LIỆU TRẬN ĐẤU", "CTDT-H1")
    add_heading(doc, "6.1 Phạm vi chức năng", "CTDT-H2")
    add_body(doc, "Phần này gồm hai nhóm nội dung chính.")
    add_body(
        doc,
        "Nhóm thứ nhất là kiến trúc của ứng dụng Desktop Client. Client chịu trách nhiệm nhận thao tác người dùng, gửi yêu cầu tới Server, tiếp nhận dữ liệu phản hồi, cập nhật trạng thái hiển thị và điều hướng giữa các màn hình.",
    )
    add_body(
        doc,
        "Nhóm thứ hai là xử lý dữ liệu sau trận đấu. Khi trận kết thúc, Server lưu thông tin trận và kết quả của từng người chơi, cập nhật thống kê tài khoản, sau đó cung cấp dữ liệu phục vụ màn hình kết quả, lịch sử và bảng xếp hạng.",
    )
    add_blank(doc, )
    add_table(
        doc,
        "Bảng 6.1. Các chức năng thuộc phần Desktop Client và dữ liệu trận đấu",
        ["ID", "Chức năng"],
        [
            ["CD-01", "Khởi tạo ứng dụng Client"],
            ["CD-02", "Facade gửi lệnh"],
            ["CD-03", "Phân phối thông điệp nhận"],
            ["CD-04", "Trạng thái hiển thị"],
            ["CD-05", "Điều hướng màn hình"],
            ["CD-06", "Lưu kết quả trận"],
            ["CD-07", "Tính điểm"],
            ["CD-08", "Màn hình kết quả"],
            ["CD-09", "Lịch sử trận"],
            ["CD-10", "Bảng xếp hạng"],
            ["CD-11", "Cài đặt và tài nguyên"],
            ["CD-12", "Đóng gói ứng dụng"],
        ],
        [950, 8190],
    )
    add_heading(doc, "6.2 Kiến trúc Desktop Client", "CTDT-H2")
    add_body(
        doc,
        "Desktop Client được tổ chức thành các tầng có trách nhiệm khác nhau nhằm tách phần giao diện khỏi phần giao tiếp mạng.",
    )
    add_body(
        doc,
        "Khi người dùng thao tác trên giao diện, yêu cầu được chuyển xuống tầng xử lý ứng dụng rồi gửi qua kết nối mạng. Theo chiều ngược lại, dữ liệu Server gửi về được xử lý, cập nhật trạng thái Client và cuối cùng được hiển thị trên màn hình phù hợp.",
    )
    add_body(
        doc,
        "Các màn hình không trực tiếp làm việc với socket mà sử dụng lớp xử lý trung gian. Nhờ đó, giao diện có thể thay đổi mà không ảnh hưởng trực tiếp tới cơ chế kết nối mạng.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-6-1-phan-tang-client.png",
        "Hình 6.1. Kiến trúc phân tầng Desktop Client",
        "Bốn tầng giao diện, trạng thái Client, xử lý ứng dụng, kết nối mạng; chiều gửi từ người dùng tới Server và chiều nhận từ Server lên giao diện.",
    )
    add_heading(doc, "6.3 Xử lý thông điệp và cập nhật giao diện", "CTDT-H2")
    add_body(doc, "Client duy trì một kết nối với Server để nhận các thông điệp cập nhật.")
    add_body(
        doc,
        "Khi một thông điệp được nhận, Client xác định loại dữ liệu, cập nhật trạng thái tương ứng và thông báo cho giao diện cần thay đổi.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-6-2-phan-phoi-thong-diep.png",
        "Hình 6.2. Phân phối thông điệp tới giao diện",
        "Server gửi thông điệp, Client nhận, xác định loại dữ liệu, cập nhật trạng thái, điều hướng nếu cần và cập nhật giao diện.",
    )
    add_heading(doc, "6.4 Điều hướng màn hình", "CTDT-H2")
    add_body(doc, "Desktop Client gồm nhiều màn hình tương ứng với các giai đoạn sử dụng hệ thống.")
    add_body(
        doc,
        "Việc chuyển màn hình có thể xuất phát từ thao tác của người dùng hoặc từ trạng thái Server gửi về.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-6-3-dieu-huong-man-hinh.png",
        "Hình 6.3. Điều hướng màn hình",
        "Các màn hình LOGIN, HOME, ROOM LIST, ROOM LOBBY, GAME, RESULT, HISTORY, LEADERBOARD và sự kiện chuyển màn hình; mất kết nối đưa về LOGIN.",
    )
    add_heading(doc, "6.5 Lưu kết quả trận đấu và tính điểm", "CTDT-H2")
    add_body(
        doc,
        "Khi Game Engine xác định trận đấu đã kết thúc, Server lưu thông tin trận và kết quả của từng người chơi vào cơ sở dữ liệu.",
    )
    add_table(
        doc,
        "Bảng 6.2. Quy tắc xác định kết quả và điểm của từng người",
        ["Kết quả trận", "Người chơi", "Kết quả cá nhân", "Điểm (đơn vị)", "Bộ đếm được tăng"],
        [
            ["WIN", "Người thắng", "WIN", "2 (1 điểm)", "totalWins"],
            ["WIN", "Người còn lại", "LOSS", "0", "totalLosses"],
            ["DRAW", "Mọi người", "DRAW", "1 (0,5 điểm)", "totalDraws"],
        ],
        [1700, 1900, 1900, 1800, 2000],
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-6-4-luu-ket-qua.png",
        "Hình 6.4. Lưu kết quả trận đấu",
        "Game Engine báo kết quả, Server tạo thông tin trận, xác định kết quả cá nhân, lưu trận đấu, chi tiết trận đấu, cập nhật thống kê tài khoản và gửi kết quả về Client.",
    )
    add_heading(doc, "6.6 Kết quả, lịch sử và bảng xếp hạng", "CTDT-H2")
    add_body(doc, "Sau khi trận kết thúc, Server gửi kết quả của từng người chơi tới các Client trong phòng.", bold=True)
    add_body(
        doc,
        "Màn hình kết quả dựa vào kết quả cá nhân để hiển thị một trong ba trạng thái: WIN, LOSS, DRAW.",
    )
    add_body(doc, "Khi người chơi mở màn hình lịch sử:", bold=True)
    add_body(
        doc,
        "Client gửi yêu cầu tới Server. Server xác định tài khoản từ phiên đăng nhập, truy vấn các trận mà tài khoản đó đã tham gia và gửi kết quả trở lại (thời gian trận, phòng, kết quả cá nhân, điểm nhận được).",
    )
    add_body(doc, "Khi mở màn hình bảng xếp hạng, Client yêu cầu dữ liệu từ Server.", bold=True)
    add_body(
        doc,
        "Server lấy thống kê của các tài khoản, sắp xếp theo tiêu chí của hệ thống và gửi danh sách xếp hạng về Client.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-6-5-lich-su-xep-hang.png",
        "Hình 6.5. Tải lịch sử và bảng xếp hạng",
        "Hai luồng: lịch sử (Server lấy các trận của người dùng từ CSDL) và bảng xếp hạng (Server lấy thống kê tài khoản, sắp xếp thứ hạng); Client hiển thị kết quả.",
    )
    add_heading(doc, "6.7 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Các hình dưới đây minh họa những màn hình trình bày dữ liệu trận đấu. Toàn bộ số liệu trên các màn hình đều lấy từ thông điệp Server gửi về, Client không tự tính điểm hoặc thứ hạng.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c6-result.png",
        "Hình 6.6. Màn hình kết quả trận",
        "Màn hình kết quả với tiêu đề VICTORY, DEFEAT hoặc DRAW.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c6-history.png",
        "Hình 6.7. Màn hình lịch sử trận đấu",
        "Danh sách các trận đã chơi, mới nhất ở trên.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c6-leaderboard.png",
        "Hình 6.8. Màn hình bảng xếp hạng",
        "Bục vinh danh top 3 và bảng xếp hạng chi tiết.",
    )
    add_heading(doc, "6.8 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(
        doc,
        "Bảng 6.3 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng của phần Desktop Client và dữ liệu trận đấu.",
    )
    add_table(
        doc,
        "Bảng 6.3. Ánh xạ hình vẽ với chức năng phần Desktop Client và dữ liệu trận đấu",
        ["Mã hình", "Hình vẽ / giao diện", "Chức năng được minh họa"],
        [
            ["Hình 6.1", "Kiến trúc phân tầng Desktop Client", "Tách giao diện khỏi kết nối mạng, tổ chức hai chiều gửi và nhận"],
            ["Hình 6.2", "Phân phối thông điệp tới giao diện", "Cập nhật trạng thái và giao diện theo thông điệp nhận được"],
            ["Hình 6.3", "Điều hướng màn hình", "Điều hướng màn hình theo thao tác người dùng và trạng thái Server gửi về"],
            ["Hình 6.4", "Lưu kết quả trận đấu", "Lưu trận đấu, kết quả cá nhân và thống kê tài khoản"],
            ["Hình 6.5", "Tải lịch sử và bảng xếp hạng", "Truy vấn dữ liệu đã lưu qua mạng và hiển thị"],
            ["Hình 6.6", "Màn hình kết quả trận", "Hiển thị GAME_OVER, gửi PLAY_AGAIN"],
            ["Hình 6.7", "Màn hình lịch sử trận đấu", "Hiển thị HISTORY_RESPONSE"],
            ["Hình 6.8", "Màn hình bảng xếp hạng", "Hiển thị RANKING_RESPONSE"],
        ],
        [1100, 2600, 5530],
    )
    add_heading(doc, "6.9 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 6 đã trình bày kiến trúc phân tầng của Desktop Client: màn hình chỉ gửi lệnh qua một facade và hiển thị ClientState, dispatcher chuyển dữ liệu mạng sang luồng JavaFX, còn trạng thái trận được truyền qua AtomicReference để phục vụ vòng vẽ. Ở phía dữ liệu, kết quả trận được lưu cùng bộ đếm xếp hạng trong một giao dịch trước khi thông báo cho người chơi, lịch sử và bảng xếp hạng được truy vấn qua cùng kết nối TCP và hiển thị trên các màn hình tương ứng.",
    )
