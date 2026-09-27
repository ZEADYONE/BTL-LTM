"""Chương 3 - Kết nối và xác thực (phần cá nhân A).

Sinh bởi docx_to_python.py từ bao_cao_nhom_bomberman-final.docx, sau đó có thể sửa tay.
Chạy lại docx_to_python.py sẽ ghi đè file này.
Cập nhật hình theo update.md (27/09/2026): vẽ lại sơ đồ ở mức phân tích chức năng (functional_diagrams.py).
Chỉnh tay sau khi sinh (27/09/2026): đánh số lại bảng liên tục (3.3 → 3.2, 3.5 → 3.3).
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
    add_heading(doc, "CHƯƠNG 3. KẾT NỐI VÀ XÁC THỰC", "CTDT-H1")
    add_heading(doc, "3.1 Phạm vi chức năng", "CTDT-H2")
    add_body(
        doc,
        "Phần kết nối và xác thực xây dựng nền tảng để mọi chức năng khác trao đổi dữ liệu qua mạng. Phạm vi gồm giao thức đóng khung thông điệp dùng chung, cơ chế Server chấp nhận và quản lý nhiều kết nối TCP đồng thời, tầng kết nối phía Client, các chức năng đăng ký, đăng nhập, đăng xuất, quản lý trạng thái trực tuyến và xử lý khi một Client mất kết nối.",
    )
    add_body(
        doc,
        "Các chương sau sử dụng lại toàn bộ hạ tầng này: sảnh và phòng chờ gửi thông điệp qua cùng phiên TCP, bộ xử lý trò chơi phát trạng thái qua cùng cơ chế ghi, còn ứng dụng Client gửi mọi lệnh qua cùng kết nối được thiết lập ở đây. Bảng 3.1 liệt kê các chức năng đã triển khai và thành phần cài đặt tương ứng.",
    )
    add_table(
        doc,
        "Bảng 3.1. Các chức năng thuộc phần kết nối và xác thực",
        ["ID", "Chức năng", "Vai trò trong giao tiếp mạng"],
        [
            ["KX-01", "Mở cổng và chấp nhận kết nối", "Bind cổng 8081, nhận socket mới"],
            ["KX-02", "Quản lý phiên kết nối", "Mỗi socket một phiên, định danh bằng UUID"],
            ["KX-03", "Đóng khung và mã hóa thông điệp", "Tách thông điệp trên dòng byte TCP"],
            ["KX-04", "Ghi dữ liệu an toàn đa luồng", "Không để hai khung ghi xen kẽ"],
            ["KX-05", "Định tuyến thông điệp", "Chuyển thông điệp tới đúng bộ xử lý"],
            ["KX-06", "Đăng ký tài khoản", "REGISTER_REQUEST / REGISTER_RESPONSE"],
            ["KX-07", "Đăng nhập", "LOGIN_REQUEST / LOGIN_RESPONSE"],
            ["KX-08", "Đăng xuất và mất kết nối", "LOGOUT; phát hiện socket đóng"],
            ["KX-09", "Trạng thái trực tuyến", "Chặn một tài khoản ở hai phiên"],
            ["KX-10", "Kết nối phía Client", "Mở socket, vòng đọc, gửi khung"],
            ["KX-11", "Ghép yêu cầu - phản hồi", "Theo requestId, giới hạn chờ 5 giây"],
            ["KX-12", "Giao diện đăng nhập", "Thu thập và kiểm tra dữ liệu gửi đi"],
        ],
        [900, 3830, 4950],
    )
    add_heading(doc, "3.2 Giao thức truyền tin", "CTDT-H2")
    add_heading(doc, "3.2.1 Cấu trúc khung thông điệp", "CTDT-H3")
    add_body(
        doc,
        "TCP truyền dữ liệu dưới dạng dòng byte liên tục và không giữ ranh giới giữa các lần gửi. Vì vậy hai phía cần một quy ước để biết một thông điệp bắt đầu và kết thúc ở đâu. Hệ thống sử dụng kỹ thuật tiền tố độ dài: trước mỗi thông điệp là một số nguyên 4 byte theo thứ tự big-endian cho biết số byte của phần nội dung phía sau. Phần nội dung là đối tượng NetworkMessage được tuần tự hóa thành JSON UTF-8.",
    )
    add_body(
        doc,
        "Đối tượng NetworkMessage chỉ có ba trường. Trường type xác định loại thông điệp theo danh mục MessageType trong module common. Trường requestId do Client sinh ngẫu nhiên dạng UUID cho mỗi lệnh; Server giữ nguyên giá trị này trong phản hồi trực tiếp và để trống với các thông điệp chủ động phát đi. Trường payload chứa dữ liệu riêng của từng loại thông điệp và có thể trống. Hình 3.1 thể hiện cấu trúc khung và các bước mã hóa, giải mã ở hai phía.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-3-1-khung-thong-diep.png",
        "Hình 3.1. Cấu trúc khung thông điệp và quá trình gửi, nhận",
        "Khung gồm độ dài và nội dung JSON; bên gửi chuyển thông điệp thành JSON, đóng khung và gửi qua TCP; bên nhận xác định độ dài, đọc nội dung và giải mã.",
    )
    add_heading(doc, "3.2.2 Các thông điệp thuộc phần kết nối và xác thực", "CTDT-H3")
    add_body(
        doc,
        "Bảng 3.2 liệt kê các thông điệp do phần này xử lý. Các yêu cầu đăng ký và đăng nhập có kiểu phản hồi riêng mang cùng requestId; đăng xuất không có phản hồi riêng vì Client chủ động xóa trạng thái đăng nhập ngay sau khi gửi.",
    )
    add_table(
        doc,
        "Bảng 3.2. Thông điệp thuộc phần kết nối và xác thực",
        ["Thông điệp", "Chiều"],
        [
            ["REGISTER_REQUEST", "Client → Server"],
            ["REGISTER_RESPONSE", "Server → Client"],
            ["LOGIN_REQUEST", "Client → Server"],
            ["LOGIN_RESPONSE", "Server → Client"],
            ["LOGOUT", "Client → Server"],
        ],
        [4730, 4950],
    )
    add_heading(doc, "3.3 Tổ chức kết nối phía Server", "CTDT-H2")
    add_body(
        doc,
        "Server mở một cổng TCP và chờ các Client kết nối. Khi có một kết nối mới, Server tạo một phiên riêng để quản lý Client đó.",
    )
    add_body(
        doc,
        "Mỗi phiên chịu trách nhiệm nhận thông điệp, xác định loại yêu cầu và chuyển yêu cầu tới bộ xử lý tương ứng như xác thực, sảnh, phòng hoặc trò chơi. Các Client có thể cùng kết nối tới một cổng Server nhưng vẫn được quản lý độc lập thông qua các phiên kết nối riêng.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-3-2-xu-ly-ket-noi-server.png",
        "Hình 3.2. Mô hình xử lý kết nối phía Server",
        "Nhiều Client kết nối tới Server, mỗi Client có một phiên riêng; yêu cầu được phân loại và chuyển tới xác thực, sảnh, phòng, trò chơi, lịch sử và xếp hạng.",
    )
    add_heading(doc, "3.4 Tổ chức kết nối phía Client", "CTDT-H2")
    add_body(
        doc,
        "Client thiết lập kết nối tới địa chỉ Server đã cấu hình và duy trì kết nối này trong quá trình sử dụng.",
    )
    add_body(
        doc,
        "Khi người dùng thực hiện một chức năng cần phản hồi, Client gửi yêu cầu kèm mã định danh. Khi Server trả kết quả, Client sử dụng mã này để xác định phản hồi tương ứng với yêu cầu nào. Dữ liệu nhận được sau đó được chuyển tới phần xử lý giao diện.",
    )
    add_body(doc, "Nếu Server không phản hồi hoặc kết nối bị gián đoạn, Client thông báo lỗi cho người dùng.")
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-3-3-ket-noi-phan-hoi.png",
        "Hình 3.3. Trình tự kết nối và nhận phản hồi",
        "Người dùng thao tác, Client kết nối và gửi yêu cầu kèm mã, Server xử lý và phản hồi, Client xác định phản hồi tương ứng và cập nhật giao diện.",
    )
    add_heading(doc, "3.5 Đăng ký và đăng nhập", "CTDT-H2")
    add_heading(doc, "3.5.1 Đăng ký tài khoản", "CTDT-H3")
    add_body(
        doc,
        "Người dùng nhập tên đăng nhập và mật khẩu tại Client. Client gửi yêu cầu đăng ký tới Server.",
    )
    add_body(
        doc,
        "Server kiểm tra tính hợp lệ của dữ liệu và xác định tên đăng nhập đã tồn tại hay chưa. Nếu hợp lệ, mật khẩu được băm trước khi lưu vào cơ sở dữ liệu. Server sau đó gửi kết quả đăng ký trở lại Client để hiển thị cho người dùng.",
    )
    add_heading(doc, "3.5.2 Đăng nhập và gắn người dùng với phiên", "CTDT-H3")
    add_body(
        doc,
        "Khi người dùng đăng nhập, Client gửi tên đăng nhập và mật khẩu tới Server. Server tìm tài khoản tương ứng, kiểm tra mật khẩu và xác định tài khoản có đang được sử dụng ở một phiên khác hay không.",
    )
    add_body(
        doc,
        "Nếu thông tin hợp lệ, Server gắn tài khoản với phiên TCP hiện tại. Từ thời điểm này, các yêu cầu tiếp theo được xác định dựa trên phiên kết nối mà không cần gửi lại thông tin tài khoản.",
    )
    add_body(
        doc,
        "Sau khi đăng nhập thành công, Client chuyển tới màn hình chính và Server cập nhật trạng thái trực tuyến của người dùng.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-3-4-dang-nhap.png",
        "Hình 3.4. Trình tự đăng nhập",
        "Người dùng nhập tài khoản, Server kiểm tra tài khoản và trạng thái trực tuyến, tạo phiên người dùng, trả kết quả; Client chuyển sang màn hình chính.",
    )
    add_heading(doc, "3.6 Kiểm tra kết nối, đăng xuất và mất kết nối", "CTDT-H2")
    add_body(
        doc,
        "Khi người dùng đăng xuất, Client gửi yêu cầu đăng xuất tới Server. Server xóa trạng thái đăng nhập của người dùng và cập nhật danh sách người trực tuyến.",
    )
    add_body(
        doc,
        "Nếu kết nối bị mất đột ngột, Server phát hiện phiên đã ngắt và thực hiện dọn trạng thái liên quan. Nếu người chơi đang ở phòng chờ, người chơi được đưa ra khỏi phòng. Nếu đang tham gia trận đấu, sự kiện mất kết nối được chuyển tới phần xử lý trò chơi để xử lý theo quy tắc của trận.",
    )
    add_body(
        doc,
        "Sau đó Server xóa trạng thái trực tuyến và thông báo các thay đổi cần thiết tới những Client liên quan.",
    )
    add_body(
        doc,
        "Ở phía Client, khi phát hiện mất kết nối, ứng dụng xóa trạng thái phiên hiện tại, chuyển người dùng về màn hình đăng nhập và hiển thị thông báo mất kết nối.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-3-5-mat-ket-noi.png",
        "Hình 3.5. Luồng xử lý mất kết nối",
        "Server phát hiện mất kết nối, xác định người dùng, xử lý phòng và trận, xóa trạng thái trực tuyến và cập nhật các Client liên quan.",
    )
    add_heading(doc, "3.7 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Giao diện của phần này là màn hình đăng nhập và đăng ký. Người dùng nhập tên và mật khẩu, chọn đăng nhập hoặc tạo tài khoản, đồng thời có thể mở hộp thoại để đổi địa chỉ Server trước khi kết nối. Hình 3.6 đến Hình 3.8 minh họa các trạng thái chính.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c3-login.png",
        "Hình 3.6. Màn hình đăng nhập và đăng ký",
        "Màn hình đăng nhập của Client JavaFX.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c3-server-address.png",
        "Hình 3.7. Hộp thoại chọn địa chỉ Server",
        "Hộp thoại nhập host:port của Server.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c3-login-error.png",
        "Hình 3.8. Thông báo lỗi khi đăng nhập",
        "Thông báo lỗi sai mật khẩu hoặc tài khoản đang trực tuyến.",
    )
    add_heading(doc, "3.8 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(
        doc,
        "Bảng 3.3 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng mà phần kết nối và xác thực đã thực hiện.",
    )
    add_table(
        doc,
        "Bảng 3.3. Ánh xạ hình vẽ với chức năng phần kết nối và xác thực",
        ["Mã hình", "Hình vẽ / giao diện", "Chức năng được minh họa"],
        [
            ["Hình 3.1", "Cấu trúc khung thông điệp và quá trình gửi, nhận", "Đóng khung, gửi và nhận thông điệp qua TCP"],
            ["Hình 3.2", "Mô hình xử lý kết nối phía Server", "Phục vụ nhiều Client đồng thời, quản lý phiên và chuyển yêu cầu tới chức năng phù hợp"],
            ["Hình 3.3", "Trình tự kết nối và nhận phản hồi", "Thiết lập kết nối, gửi yêu cầu và ghép phản hồi theo mã yêu cầu"],
            ["Hình 3.4", "Trình tự đăng nhập", "Xác thực người dùng và tạo phiên người dùng"],
            ["Hình 3.5", "Luồng xử lý mất kết nối", "Phát hiện mất kết nối và dọn trạng thái người dùng"],
            ["Hình 3.6", "Màn hình đăng nhập và đăng ký", "Thu thập thông tin xác thực, gửi LOGIN_REQUEST / REGISTER_REQUEST"],
            ["Hình 3.7", "Hộp thoại chọn địa chỉ Server", "Cấu hình địa chỉ TCP của Server trước khi kết nối"],
            ["Hình 3.8", "Thông báo lỗi khi đăng nhập", "Hiển thị mã kết quả xác thực từ LOGIN_RESPONSE"],
        ],
        [1100, 2600, 5980],
    )
    add_heading(doc, "3.9 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 3 đã trình bày giao thức đóng khung thông điệp bằng tiền tố độ dài, mô hình một luồng acceptor kết hợp một virtual thread cho mỗi phiên ở Server, cơ chế ghi tuần tự bằng khóa, tầng kết nối phía Client với luồng đọc, luồng ghi và cơ chế ghép phản hồi theo requestId. Trên nền đó, các chức năng đăng ký, đăng nhập, đăng xuất và xử lý mất kết nối bảo đảm mỗi phiên TCP gắn với đúng một người dùng. Chương tiếp theo sử dụng các phiên đã xác thực này để tổ chức sảnh và phòng chờ.",
    )
