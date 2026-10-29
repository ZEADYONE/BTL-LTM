"""Chương 4 - Sảnh và phòng chờ (phần cá nhân B).

Sinh bởi docx_to_python.py từ bao_cao_nhom_bomberman-final.docx, sau đó có thể sửa tay.
Chạy lại docx_to_python.py sẽ ghi đè file này.
Cập nhật hình theo update.md (27/09/2026): vẽ lại sơ đồ ở mức phân tích chức năng (functional_diagrams.py).
Chỉnh tay sau khi sinh (27/09/2026): đánh số lại bảng (4.4 → 4.2), bỏ đoạn chỉ có dấu chấm.
"""
from __future__ import annotations

from report_common import (
    ASSET_DIR,
    SCREENSHOT_DIR,
    add_blank,
    add_body,
    add_bullet,
    add_figure,
    add_heading,
    add_table,
)


def write(doc):
    add_heading(doc, "CHƯƠNG 4. SẢNH VÀ PHÒNG CHỜ", "CTDT-H1")
    add_heading(doc, "4.1 Phạm vi chức năng", "CTDT-H2")
    add_body(
        doc,
        "Sau khi xác thực, người chơi đi vào sảnh để quan sát ai đang trực tuyến, các phòng đang có và lựa chọn tạo phòng mới hoặc tham gia một phòng. Phần sảnh và phòng chờ quản lý toàn bộ vòng đời của phòng trước và sau trận: tạo, tham gia, rời phòng, thay đổi trạng thái sẵn sàng, bắt đầu trận và chuẩn bị chơi lại.",
    )
    add_body(
        doc,
        "Đặc điểm mạng chính của phần này là đồng bộ trạng thái chung tới nhiều Client. Mỗi thay đổi của một người chơi phải được phản ánh trên màn hình của những người liên quan: thành viên cùng phòng nhận trạng thái phòng mới, còn người đang ở sảnh nhận danh sách phòng và danh sách người trực tuyến mới. Bảng 4.1 liệt kê các chức năng đã triển khai.",
    )
    add_table(
        doc,
        "Bảng 4.1. Các chức năng thuộc phần sảnh và phòng chờ",
        ["ID", "Chức năng"],
        [
            ["SP-01", "Danh sách người trực tuyến"],
            ["SP-02", "Danh sách phòng"],
            ["SP-03", "Phát cập nhật sảnh"],
            ["SP-04", "Tạo phòng"],
            ["SP-05", "Tham gia phòng"],
            ["SP-06", "Chơi nhanh"],
            ["SP-07", "Rời phòng, chuyển chủ phòng"],
            ["SP-08", "Sẵn sàng"],
            ["SP-09", "Bắt đầu trận"],
            ["SP-10", "Chơi lại"],
            ["SP-11", "Dọn phòng khi mất kết nối"],
            ["SP-12", "Giao diện sảnh và phòng"],
        ],
        [4190, 5040],
    )
    add_heading(doc, "4.2 Trạng thái sảnh và phòng chờ", "CTDT-H2")
    add_body(
        doc,
        "Thông tin về sảnh và các phòng đang hoạt động được giữ trong bộ nhớ Server vì dữ liệu này thay đổi thường xuyên và không cần lưu lâu dài trong cơ sở dữ liệu.",
    )
    add_body(
        doc,
        "Mỗi phòng lưu các thông tin chính như mã phòng, tên phòng, chủ phòng, danh sách người chơi và trạng thái hiện tại. Một người chơi chỉ được tham gia một phòng tại một thời điểm.",
    )
    add_body(doc, "Người chơi trực tuyến có thể ở một trong các trạng thái chính:")
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-4-1-trang-thai.png",
        "Hình 4.1. Trạng thái người chơi và trạng thái phòng",
        "Người chơi chuyển giữa FREE, IN_ROOM, PLAYING; phòng chuyển giữa WAITING, PLAYING, FINISHED theo các sự kiện vào phòng, bắt đầu trận, kết thúc trận, chơi lại, rời phòng.",
    )
    add_heading(doc, "4.3 Đồng bộ sảnh", "CTDT-H2")
    add_body(doc, "Client nhận thông tin sảnh theo hai cách.")
    add_body(
        doc,
        "Thứ nhất, khi người chơi mở màn hình Home hoặc danh sách phòng, Client gửi yêu cầu tới Server để lấy danh sách người trực tuyến và danh sách phòng hiện tại.",
    )
    add_body(
        doc,
        "Thứ hai, khi trạng thái sảnh thay đổi, chẳng hạn có người đăng nhập, tạo phòng, tham gia phòng hoặc rời phòng, Server chủ động gửi dữ liệu mới tới các Client đang ở sảnh.",
    )
    add_body(
        doc,
        "Nhờ đó, các Client không cần tự suy đoán thay đổi mà luôn hiển thị trạng thái mới nhất do Server cung cấp.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-4-2-dong-bo-sanh.png",
        "Hình 4.2. Đồng bộ sảnh",
        "Client yêu cầu danh sách và nhận phản hồi; khi sảnh thay đổi, Server chủ động gửi danh sách mới tới các Client đang ở sảnh.",
    )
    add_heading(doc, "4.4 Tạo, tham gia và rời phòng", "CTDT-H2")
    add_heading(doc, "4.4.1 Tạo và tham gia phòng", "CTDT-H3")
    add_body(
        doc,
        "Khi người chơi tạo phòng, Server kiểm tra dữ liệu yêu cầu và trạng thái hiện tại của người chơi. Nếu hợp lệ, một phòng mới được tạo và người tạo trở thành chủ phòng.",
    )
    add_body(
        doc,
        "Khi người chơi yêu cầu tham gia một phòng, Server kiểm tra phòng còn tồn tại, đang ở trạng thái chờ và còn chỗ hay không. Nếu đủ điều kiện, người chơi được thêm vào phòng.",
    )
    add_body(
        doc,
        "Sau mỗi thay đổi, Server gửi trạng thái phòng mới tới các thành viên để tất cả Client trong phòng hiển thị cùng một danh sách người chơi và cùng trạng thái.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-4-3-tao-tham-gia-phong.png",
        "Hình 4.3. Tạo phòng và tham gia phòng",
        "Client 1 tạo phòng, Client 2 tham gia; Server kiểm tra phòng và gửi trạng thái phòng mới cho cả hai Client.",
    )
    add_heading(doc, "4.4.2 Chơi nhanh", "CTDT-H3")
    add_body(doc, "Chức năng chơi nhanh cho phép người chơi vào phòng mà không cần chọn thủ công.")
    add_body(
        doc,
        "Client ưu tiên tìm một phòng đang chờ và còn chỗ. Nếu có phòng phù hợp, Client gửi yêu cầu tham gia. Nếu không có phòng thích hợp, hệ thống tạo một phòng mới cho người chơi.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-4-4-choi-nhanh.png",
        "Hình 4.4. Luồng chơi nhanh",
        "Người chơi chọn PLAY, Client tìm phòng phù hợp để tham gia, nếu không có thì tạo phòng mới, sau đó vào phòng chờ.",
    )
    add_heading(doc, "4.4.3 Rời phòng và dọn phòng khi mất kết nối", "CTDT-H3")
    add_body(
        doc,
        "Khi người chơi rời phòng, Server xóa người chơi khỏi danh sách thành viên. Nếu người rời là chủ phòng, quyền chủ phòng được chuyển cho một thành viên còn lại. Nếu không còn thành viên nào, phòng được xóa.",
    )
    add_body(
        doc,
        "Sau đó Server gửi trạng thái phòng mới tới những người còn lại và cập nhật lại thông tin sảnh.",
    )
    add_body(
        doc,
        "Nếu người chơi mất kết nối đột ngột, hệ thống thực hiện xử lý tương tự rời phòng để tránh giữ lại một thành viên không còn hoạt động.",
    )
    add_body(
        doc,
        "Nếu người chơi đang ở trong trận, sự kiện mất kết nối được chuyển sang phần xử lý trò chơi để xác định kết quả phù hợp.",
    )
    add_blank(doc, )
    add_heading(doc, "4.5 Sẵn sàng, bắt đầu trận và chơi lại", "CTDT-H2")
    add_body(doc, "Trong phòng chờ, mỗi người chơi có thể thay đổi trạng thái sẵn sàng.")
    add_body(doc, "Chủ phòng chỉ có thể bắt đầu trận khi:")
    add_bullet(doc, "phòng có đủ số người tối thiểu;")
    add_bullet(doc, "phòng đang ở trạng thái chờ;")
    add_bullet(doc, "tất cả người chơi đã sẵn sàng.")
    add_body(
        doc,
        "Khi các điều kiện được đáp ứng, Server chuyển trạng thái phòng sang thi đấu, khởi tạo trận và gửi trạng thái mới tới tất cả thành viên. Các Client sau đó chuyển sang màn hình trò chơi.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-4-5-san-sang-bat-dau.png",
        "Hình 4.5. Sẵn sàng và bắt đầu trận",
        "Người chơi báo sẵn sàng, chủ phòng bắt đầu trận, Server kiểm tra điều kiện, chuyển phòng sang PLAYING và các Client vào màn hình GAME.",
    )
    add_body(
        doc,
        "Sau khi trận kết thúc, phòng chuyển sang trạng thái kết thúc. Khi người chơi chọn chơi lại, phòng được đưa trở về trạng thái chờ và các trạng thái sẵn sàng được thiết lập lại để chuẩn bị cho trận tiếp theo.",
    )
    add_body(
        doc,
        "Nếu thao tác không hợp lệ, chẳng hạn phòng đã đầy, người gửi không phải chủ phòng hoặc chưa đủ người sẵn sàng, Server từ chối yêu cầu và thông báo lỗi cho Client.",
    )
    add_heading(doc, "4.6 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Giao diện của phần này gồm màn hình Home, màn hình danh sách phòng và màn hình phòng chờ. Các màn hình không tự thay đổi dữ liệu phòng mà chỉ hiển thị trạng thái mới nhất nhận từ Server, vì vậy hai Client trong cùng phòng luôn thấy cùng danh sách thành viên và trạng thái sẵn sàng.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c4-home.png",
        "Hình 4.6. Màn hình Home",
        "Màn hình Home với nút PLAY và số người trực tuyến.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c4-room-browser.png",
        "Hình 4.7. Màn hình danh sách phòng và người trực tuyến",
        "Danh sách phòng và bảng người trực tuyến.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c4-room-lobby.png",
        "Hình 4.8. Phòng chờ được đồng bộ giữa hai Client",
        "Hai cửa sổ Client cùng hiển thị một phòng chờ.",
    )
    add_heading(doc, "4.7 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(
        doc,
        "Bảng 4.2 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng của phần sảnh và phòng chờ.",
    )
    add_table(
        doc,
        "Bảng 4.2. Ánh xạ hình vẽ với chức năng phần sảnh và phòng chờ",
        ["Mã hình", "Hình vẽ / giao diện", "Chức năng được minh họa"],
        [
            ["Hình 4.1", "Trạng thái người chơi và trạng thái phòng", "Quản lý trạng thái người chơi và vòng đời phòng"],
            ["Hình 4.2", "Đồng bộ sảnh", "Đồng bộ danh sách người trực tuyến và danh sách phòng tới nhiều Client"],
            ["Hình 4.3", "Tạo phòng và tham gia phòng", "Tạo phòng, tham gia phòng và gửi trạng thái phòng tới thành viên"],
            ["Hình 4.4", "Luồng chơi nhanh", "Chọn phòng tự động hoặc tạo phòng mới"],
            ["Hình 4.5", "Sẵn sàng và bắt đầu trận", "Đồng bộ trạng thái sẵn sàng và chuyển phòng sang thi đấu"],
            ["Hình 4.6", "Màn hình Home", "Chơi nhanh, hiển thị tóm tắt sảnh"],
            ["Hình 4.7", "Màn hình danh sách phòng và người trực tuyến", "Hiển thị ROOM_LIST_UPDATE, ONLINE_USERS_UPDATE; tạo và tham gia phòng"],
            ["Hình 4.8", "Phòng chờ được đồng bộ giữa hai Client", "Đồng bộ ROOM_STATE, sẵn sàng và điều kiện bắt đầu"],
        ],
        [1100, 2820, 5220],
    )
    add_heading(doc, "4.8 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 4 đã trình bày cách Server quản lý phòng trong bộ nhớ với khóa thay đổi chung và quy tắc mỗi người một phòng, cách đồng bộ sảnh bằng cả yêu cầu trực tiếp và phát chủ động tới các phiên FREE, cách gửi trạng thái phòng có chọn lọc tới thành viên, cùng các điều kiện sẵn sàng và bắt đầu trận. Khi phòng chuyển sang PLAYING, quyền điều khiển được giao cho bộ xử lý trò chơi được trình bày ở chương tiếp theo.",
    )
