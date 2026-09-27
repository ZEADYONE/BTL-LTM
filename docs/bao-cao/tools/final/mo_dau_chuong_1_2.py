"""Mở đầu, Chương 1 và Chương 2 (phần nhóm).

Sinh bởi docx_to_python.py từ bao_cao_nhom_bomberman-final.docx, sau đó có thể sửa tay.
Chạy lại docx_to_python.py sẽ ghi đè file này.
"""
from __future__ import annotations

from report_common import (
    ASSET_DIR,
    IMAGE_DIR,
    add_body,
    add_bullet,
    add_figure,
    add_heading,
    add_table,
)


def write(doc):
    add_heading(doc, "MỞ ĐẦU", "CTDT-H0")
    add_body(
        doc,
        "Sự phát triển của các ứng dụng tương tác qua mạng đặt ra yêu cầu không chỉ truyền được dữ liệu mà còn phải duy trì trạng thái thống nhất giữa nhiều người dùng. Với trò chơi đối kháng trực tuyến, yêu cầu này thể hiện rõ ở việc mọi người chơi phải nhìn thấy cùng một bản đồ, vị trí, bom, vụ nổ và kết quả trong cùng một thời điểm hợp lý.",
    )
    add_body(
        doc,
        "Đề tài Bomberman Online Mini xây dựng một trò chơi desktop cho từ hai đến bốn người trong một phòng và cho phép nhiều phòng hoạt động đồng thời. Hệ thống áp dụng mô hình một Server phục vụ nhiều Client. Client tiếp nhận thao tác và hiển thị, trong khi Server kiểm tra yêu cầu, cập nhật trạng thái và gửi kết quả trở lại các máy đang tham gia.",
    )
    add_body(
        doc,
        "Báo cáo gồm hai phần. Phần chung, gồm Chương 1 và Chương 2, trình bày tổng quan, kiến trúc chung và thiết kế chung của hệ thống. Phần cá nhân, gồm Chương 3 đến Chương 6, trình bày chi tiết từng module theo bảng phân công: kết nối và xác thực, sảnh và phòng chờ, Game Engine và đồng bộ thời gian thực, kiến trúc Desktop Client và dữ liệu trận đấu. Mỗi chương cá nhân mô tả chức năng bằng biểu đồ, giao diện và có bảng ánh xạ giữa hình vẽ với chức năng đã thực hiện.",
    )
    add_heading(doc, "CHƯƠNG 1. TỔNG QUAN HỆ THỐNG", "CTDT-H1")
    add_heading(doc, "1.1 Giới thiệu bài toán", "CTDT-H2")
    add_body(
        doc,
        "Bomberman Online Mini là trò chơi đối kháng trực tuyến, trong đó người chơi di chuyển trên bản đồ dạng ô, đặt bom để phá vật cản và loại đối thủ. Khác với trò chơi chạy trên một máy, phiên bản trực tuyến phải tiếp nhận thao tác từ nhiều máy và bảo đảm tất cả người chơi cùng theo dõi một trạng thái thống nhất.",
    )
    add_body(
        doc,
        "Bài toán được giải quyết bằng một máy chủ trung tâm. Máy khách chỉ gửi ý định, chẳng hạn hướng di chuyển hoặc yêu cầu đặt bom. Máy chủ kiểm tra điều kiện hợp lệ, cập nhật trận đấu và gửi trạng thái mới cho các máy trong cùng phòng. Nhờ đó, người chơi không tự thay đổi vị trí hay kết quả ở phía máy của mình.",
    )
    add_heading(doc, "1.2 Mục tiêu của hệ thống", "CTDT-H2")
    add_body(doc, "Hệ thống được xây dựng hướng tới các mục tiêu chính sau:")
    add_bullet(doc, "Cho phép nhiều người chơi kết nối, đăng ký, đăng nhập và sử dụng hệ thống qua mạng TCP.")
    add_bullet(doc, "Tổ chức sảnh chờ và nhiều phòng chơi độc lập, mỗi phòng có từ hai đến bốn người.")
    add_bullet(doc, "Duy trì một trạng thái trận đấu thống nhất do Server quản lý.")
    add_bullet(doc, "Cập nhật kịp thời trạng thái phòng và trạng thái trò chơi tới đúng người chơi liên quan.")
    add_bullet(doc, "Lưu kết quả đã hoàn thành để cung cấp lịch sử và bảng xếp hạng.")
    add_bullet(doc, "Giữ kiến trúc đủ rõ để có thể thay đổi giao diện Client mà không làm thay đổi luật xử lý phía Server.")
    add_heading(doc, "1.3 Phạm vi chức năng", "CTDT-H2")
    add_heading(doc, "1.3.1 Chức năng phía người chơi", "CTDT-H3")
    add_bullet(doc, "Đăng ký tài khoản, đăng nhập và đăng xuất.")
    add_bullet(doc, "Xem người chơi đang trực tuyến và danh sách phòng.")
    add_bullet(doc, "Tạo phòng, tham gia phòng, rời phòng và thay đổi trạng thái sẵn sàng.")
    add_bullet(doc, "Di chuyển nhân vật, đặt bom và theo dõi diễn biến trận đấu.")
    add_bullet(doc, "Xem kết quả, lịch sử các trận đã chơi và bảng xếp hạng.")
    add_heading(doc, "1.3.2 Chức năng phía Server", "CTDT-H3")
    add_bullet(doc, "Quản lý kết nối, phiên đăng nhập và trạng thái trực tuyến.")
    add_bullet(doc, "Quản lý danh sách phòng, thành viên, chủ phòng và trạng thái sẵn sàng.")
    add_bullet(doc, "Kiểm tra thao tác của người chơi và điều khiển diễn biến trận đấu.")
    add_bullet(doc, "Gửi trạng thái phòng, trạng thái trận đấu và kết quả tới đúng Client.")
    add_bullet(doc, "Lưu tài khoản, kết quả trận và số liệu phục vụ xếp hạng.")
    add_heading(doc, "1.4 Công nghệ sử dụng", "CTDT-H2")
    add_body(
        doc,
        "Các công nghệ được lựa chọn theo yêu cầu của một ứng dụng desktop có giao tiếp mạng liên tục. Bảng 1.1 trình bày vai trò của từng nhóm công nghệ ở mức tổng quát.",
    )
    add_table(
        doc,
        "Bảng 1.1. Công nghệ chính của hệ thống",
        ["Nhóm công nghệ", "Công nghệ", "Vai trò trong hệ thống"],
        [
            ["Ngôn ngữ và xây dựng", "Java 21, Gradle", "Phát triển và quản lý bốn module của dự án."],
            ["Server", "Spring Boot", "Khởi tạo ứng dụng Server và quản lý các thành phần xử lý."],
            ["Giao diện", "JavaFX", "Xây dựng giao diện desktop chính cho người chơi."],
            ["Giao tiếp mạng", "TCP Socket, JSON", "Duy trì kết nối và truyền thông điệp hai chiều."],
            ["Lưu trữ", "MySQL, JPA", "Lưu tài khoản, kết quả trận và số liệu xếp hạng."],
            ["Bảo mật mật khẩu", "BCrypt", "Lưu mật khẩu dưới dạng đã mã hóa một chiều."],
        ],
        [2250, 2150, 4900],
    )
    add_heading(doc, "1.5 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 1 đã giới thiệu bài toán, mục tiêu, phạm vi chức năng và các công nghệ chính của Bomberman Online Mini. Trọng tâm của hệ thống là duy trì trạng thái chung cho nhiều người chơi thông qua một Server trung tâm. Chương tiếp theo trình bày cách tổ chức các thành phần và thiết kế tổng thể để thực hiện mục tiêu đó.",
    )
    add_heading(doc, "CHƯƠNG 2. KIẾN TRÚC VÀ THIẾT KẾ HỆ THỐNG", "CTDT-H1")
    add_heading(doc, "2.1 Kiến trúc tổng thể", "CTDT-H2")
    add_body(
        doc,
        "Hệ thống được tổ chức theo mô hình Client-Server. Nhiều Desktop Client kết nối tới một Server thông qua TCP. Client chịu trách nhiệm giao tiếp với người dùng, thu nhận thao tác và trình bày dữ liệu. Server quản lý tài khoản, sảnh, phòng chơi, diễn biến trận đấu và dữ liệu lưu trữ.",
    )
    add_body(
        doc,
        "Trong thời gian thi đấu, Server là nơi giữ trạng thái chính. Mỗi Client chỉ gửi thao tác điều khiển; kết quả sau khi kiểm tra được Server gửi lại dưới dạng trạng thái hoàn chỉnh của trận. Cách tổ chức này giúp các máy không phải tự tính toán theo những bản dữ liệu riêng và hạn chế chênh lệch giữa người chơi.",
    )
    add_body(
        doc,
        "Dự án được chia thành bốn module. Module dùng chung chứa định dạng thông điệp; module Server chứa xử lý trung tâm; module JavaFX là Client chính; module libGDX là Client tương thích với cùng giao thức. Hình 2.1 thể hiện các khối chính và quan hệ giữa chúng.",
    )
    add_figure(
        doc,
        ASSET_DIR / "hinh-2-1-kien-truc-tong-the.png",
        "Hình 2.1. Kiến trúc tổng thể của hệ thống",
        "Sơ đồ người chơi sử dụng Desktop Client, trao đổi TCP với Server và Server lưu dữ liệu vào cơ sở dữ liệu.",
    )
    add_heading(doc, "2.2 Các thành phần chính", "CTDT-H2")
    add_body(
        doc,
        "Mỗi thành phần có một trách nhiệm rõ ràng. Bảng 2.1 chỉ trình bày các khối có ảnh hưởng trực tiếp đến kiến trúc chung; các thành phần hiển thị nhỏ và chi tiết xử lý nội bộ không được liệt kê.",
    )
    add_table(
        doc,
        "Bảng 2.1. Các thành phần chính của hệ thống",
        ["Thành phần", "Trách nhiệm", "Dữ liệu nhận", "Kết quả tạo ra"],
        [
            ["Desktop Client", "Hiển thị giao diện và nhận thao tác người chơi.", "Thao tác người dùng; dữ liệu từ Server.", "Yêu cầu mạng; giao diện được cập nhật."],
            ["Kết nối TCP", "Duy trì kênh trao đổi hai chiều giữa Client và Server.", "Thông điệp cần gửi; dữ liệu từ socket.", "Thông điệp đã gửi hoặc đã nhận."],
            ["Quản lý phiên", "Theo dõi từng kết nối và người dùng đã đăng nhập.", "Kết nối mới; thông tin đăng nhập.", "Phiên hoạt động hoặc thông báo lỗi."],
            ["Sảnh và phòng", "Quản lý người online, danh sách phòng, thành viên và trạng thái sẵn sàng.", "Yêu cầu tạo, tham gia, rời phòng.", "Trạng thái sảnh và phòng mới."],
            ["Bộ xử lý trò chơi", "Kiểm tra thao tác, cập nhật bản đồ, người chơi, bom và kết quả.", "Lệnh của người chơi; thời gian xử lý.", "Trạng thái trận đấu mới."],
            ["Lưu trữ dữ liệu", "Lưu tài khoản và kết quả sau trận.", "Dữ liệu tài khoản; kết quả hoàn thành.", "Lịch sử và bảng xếp hạng."],
        ],
        [1900, 2850, 2200, 2350],
    )
    add_heading(doc, "2.3 Kiến trúc giao tiếp Client-Server", "CTDT-H2")
    add_body(
        doc,
        "Server mở cổng TCP 8081 theo cấu hình mặc định. Mỗi Client tạo một kết nối và duy trì kết nối này trong suốt phiên sử dụng. Nhờ kết nối liên tục, Server có thể chủ động gửi trạng thái mới mà không cần Client tạo lại kết nối cho từng thao tác.",
    )
    add_body(
        doc,
        "Mỗi thông điệp gồm phần cho biết độ dài và phần nội dung JSON. Phần độ dài giúp bên nhận xác định chính xác điểm kết thúc của một thông điệp trong dòng dữ liệu TCP. Nội dung JSON gồm loại thông điệp, mã yêu cầu và dữ liệu đi kèm. Mã yêu cầu giúp Client biết phản hồi tương ứng với thao tác nào.",
    )
    add_body(
        doc,
        "Khi có nhiều Client, Server duy trì riêng thông tin của từng kết nối. Dữ liệu phòng chỉ được gửi cho thành viên trong phòng; dữ liệu trận đấu chỉ được gửi cho người tham gia trận đó. Hình 2.2 mô tả cách nhiều Client cùng trao đổi với Server và cách Server tách xử lý theo từng phòng.",
    )
    add_figure(
        doc,
        ASSET_DIR / "hinh-2-2-giao-tiep-client-server.png",
        "Hình 2.2. Kiến trúc giao tiếp giữa Client và Server",
        "Sơ đồ nhiều Client kết nối TCP tới Server, Server xử lý trung tâm và quản lý nhiều phòng độc lập.",
    )
    add_heading(doc, "2.4 Luồng dữ liệu tổng quát", "CTDT-H2")
    add_body(
        doc,
        "Luồng dữ liệu bắt đầu từ thao tác của người chơi. Client chuyển thao tác thành yêu cầu và gửi qua kết nối TCP. Server xác định người gửi, kiểm tra trạng thái phòng và điều kiện của trò chơi trước khi chấp nhận. Sau khi trạng thái được cập nhật, kết quả mới được gửi tới các Client liên quan để cập nhật giao diện.",
    )
    add_body(
        doc,
        "Client không gửi tọa độ đích và không tự thông báo rằng một người chơi đã thắng. Ví dụ, với thao tác di chuyển, Client chỉ gửi hướng. Server kiểm tra ô đích có nằm trong bản đồ, có bị tường, bom hoặc người chơi khác chiếm giữ hay không. Chỉ sau bước này vị trí mới được thay đổi.",
    )
    add_figure(
        doc,
        ASSET_DIR / "hinh-2-3-luong-du-lieu.png",
        "Hình 2.3. Luồng dữ liệu tổng quát trong hệ thống",
        "Chuỗi từ thao tác người chơi qua Client, TCP, Server, cập nhật trạng thái rồi quay lại giao diện Client.",
    )
    add_heading(doc, "2.5 Thiết kế module", "CTDT-H2")
    add_body(
        doc,
        "Việc chia module giúp tách phần giao tiếp chung khỏi giao diện và phần xử lý Server. Bảng 2.2 mô tả vai trò và quan hệ chính của từng module mà không đi sâu vào các tệp hoặc hàm cụ thể.",
    )
    add_table(
        doc,
        "Bảng 2.2. Thiết kế các module của dự án",
        ["Module", "Vai trò", "Quan hệ chính"],
        [
            ["common", "Định nghĩa thông điệp, dữ liệu trao đổi và cách đóng gói dữ liệu TCP.", "Được Server và cả hai Client sử dụng."],
            ["server", "Quản lý kết nối, tài khoản, phòng, trò chơi và lưu trữ.", "Nhận yêu cầu từ Client và kết nối tới cơ sở dữ liệu."],
            ["client-fx", "Giao diện desktop chính bằng JavaFX.", "Gửi thao tác tới Server và hiển thị trạng thái nhận được."],
            ["client", "Giao diện libGDX tương thích với giao thức hiện tại.", "Dùng chung module common và kết nối cùng Server."],
        ],
        [1800, 4100, 3400],
    )
    add_body(
        doc,
        "Bên trong module Server, mã nguồn tiếp tục được chia theo các nhóm mạng, xác thực, sảnh, phòng, trò chơi, kết quả và lưu trữ. Đây là cách phân lớp theo trách nhiệm trong cùng một ứng dụng, không phải kiến trúc nhiều dịch vụ độc lập.",
    )
    add_heading(doc, "2.6 Thiết kế lớp ở mức tổng quát", "CTDT-H2")
    add_body(
        doc,
        "Sơ đồ lớp trong phần nhóm chỉ lựa chọn các lớp đại diện. Mục tiêu là cho thấy đường đi từ giao diện tới kết nối, từ kết nối tới quản lý phòng và trò chơi, sau đó tới lưu trữ. Các lớp hỗ trợ hiển thị, hiệu ứng và kiểm tra nhỏ không được đưa vào để sơ đồ dễ đọc.",
    )
    add_body(
        doc,
        "Phía Client có ba nhóm chính: giao diện, điều khiển/trạng thái và kết nối mạng. Phía Server bắt đầu từ quản lý kết nối, sau đó chuyển yêu cầu tới nhóm xác thực, sảnh, phòng hoặc trò chơi. Khi trận kết thúc, nhóm kết quả chuyển dữ liệu cần lưu tới cơ sở dữ liệu.",
    )
    add_figure(
        doc,
        IMAGE_DIR / "hinh-2-4-thiet-ke-cac-nhom-lop-chinh.png",
        "Hình 2.4. Thiết kế các nhóm lớp chính",
        "Thiết kế các nhóm lớp chính",
        width=6.57,
    )
    add_heading(doc, "2.7 Thiết kế dữ liệu", "CTDT-H2")
    add_body(
        doc,
        "Dữ liệu của hệ thống được chia thành dữ liệu lưu lâu dài và dữ liệu đang hoạt động. Tài khoản, kết quả trận và số liệu xếp hạng được lưu trong MySQL. Danh sách người online, phòng đang có và trạng thái trận đang chạy được giữ trong bộ nhớ Server để đáp ứng nhanh.",
    )
    add_body(
        doc,
        "Thiết kế lưu trữ gồm ba nhóm dữ liệu chính. Bảng tài khoản lưu thông tin đăng nhập và thống kê. Bảng trận đấu lưu thời gian, phòng và kết quả chung. Bảng người tham gia lưu kết quả và điểm của từng người trong mỗi trận.",
    )
    add_figure(
        doc,
        IMAGE_DIR / "hinh-2-5-mo-hinh-du-lieu-luu-tru.png",
        "Hình 2.5. Mô hình dữ liệu lưu trữ",
        "Mô hình dữ liệu lưu trữ",
        width=6.57,
    )
    add_body(
        doc,
        "Trạng thái đang chơi không được ghi liên tục xuống cơ sở dữ liệu. Khi trận kết thúc, Server mới lưu kết quả và cập nhật thống kê. Lựa chọn này làm giảm thao tác lưu trữ trong lúc trận đang diễn ra, nhưng trận chưa hoàn thành sẽ không được phục hồi nếu Server dừng đột ngột.",
    )
    add_heading(doc, "2.8 Thiết kế giao thức trao đổi dữ liệu", "CTDT-H2")
    add_body(
        doc,
        "Thông điệp trao đổi giữa Client và Server được phân nhóm theo chức năng. Mỗi nhóm có thể bao gồm nhiều loại thông điệp cụ thể nhưng cùng phục vụ một nhóm nghiệp vụ. Ở mức thiết kế tổng quát, báo cáo chỉ trình bày chiều giao tiếp, mục đích và nội dung chính thay vì mô tả chi tiết cấu trúc JSON của từng loại thông điệp.",
    )
    add_table(
        doc,
        "Bảng 2.3. Các nhóm thông điệp Client-Server",
        ["Nhóm", "Chiều chính", "Nội dung tiêu biểu", "Kết quả"],
        [
            ["Kết nối", "Hai chiều", "Kiểm tra kết nối và thông báo lỗi.", "Xác định kết nối còn hoạt động hoặc có lỗi."],
            ["Tài khoản", "Client yêu cầu, Server trả lời", "Đăng ký, đăng nhập, đăng xuất.", "Tạo hoặc xác nhận phiên người dùng."],
            ["Sảnh", "Hai chiều", "Danh sách người online và danh sách phòng.", "Cập nhật màn hình sảnh."],
            ["Phòng", "Hai chiều", "Tạo, tham gia, rời phòng, sẵn sàng, bắt đầu.", "Cập nhật trạng thái phòng cho thành viên."],
            ["Điều khiển trận", "Client gửi", "Hướng di chuyển và yêu cầu đặt bom.", "Được kiểm tra trước khi cập nhật."],
            ["Trạng thái trận", "Server gửi", "Bản đồ, người chơi, bom, vụ nổ và kết quả.", "Các Client trong phòng hiển thị cùng dữ liệu."],
            ["Sau trận", "Hai chiều", "Chơi lại, lịch sử và bảng xếp hạng.", "Chuẩn bị vòng mới hoặc hiển thị dữ liệu đã lưu."],
        ],
        [1550, 2200, 3100, 2450],
    )
    add_heading(doc, "2.9 Các luồng xử lý chính", "CTDT-H2")
    add_heading(doc, "2.9.1 Đăng nhập", "CTDT-H3")
    add_body(
        doc,
        "Client gửi tên đăng nhập và mật khẩu tới Server. Server kiểm tra dữ liệu tài khoản, đối chiếu mật khẩu đã mã hóa và xác định tài khoản có đang online ở nơi khác hay không. Nếu hợp lệ, người dùng được gắn với kết nối hiện tại và Client chuyển tới màn hình chính.",
    )
    add_heading(doc, "2.9.2 Tạo hoặc tham gia phòng", "CTDT-H3")
    add_body(
        doc,
        "Khi tạo phòng, người dùng trở thành chủ phòng. Khi tham gia, Server kiểm tra phòng còn tồn tại, đang chờ và chưa đủ bốn người. Sau mỗi thay đổi, trạng thái phòng đầy đủ được gửi lại cho các thành viên, nhờ đó danh sách người chơi và trạng thái sẵn sàng được cập nhật đồng nhất.",
    )
    add_heading(doc, "2.9.3 Bắt đầu và vận hành trận đấu", "CTDT-H3")
    add_body(
        doc,
        "Chủ phòng chỉ có thể bắt đầu khi phòng có ít nhất hai người và tất cả thành viên đã sẵn sàng. Server tạo trạng thái ban đầu, đặt người chơi tại các vị trí xuất phát và mở bộ xử lý riêng cho phòng. Các thao tác sau đó được tiếp nhận, kiểm tra và xử lý theo thứ tự.",
    )
    add_body(
        doc,
        "Trận đấu được cập nhật theo nhịp đều đặn. Trạng thái mới được gửi nhiều lần trong một giây để Client hiển thị chuyển động, bom và vụ nổ. Mỗi phòng có bộ xử lý riêng, vì vậy một trận ở phòng này không sử dụng trạng thái của phòng khác.",
    )
    add_figure(
        doc,
        ASSET_DIR / "hinh-2-6-luong-tran-dau.png",
        "Hình 2.6. Luồng xử lý một trận đấu",
        "Luồng từ phòng chờ, sẵn sàng, khởi tạo trận, xử lý thao tác, gửi trạng thái đến kết thúc và lưu kết quả.",
    )
    add_heading(doc, "2.9.4 Kết thúc trận và mất kết nối", "CTDT-H3")
    add_body(
        doc,
        "Khi chỉ còn một người sống, người đó thắng; nếu mọi người bị loại cùng lúc, trận hòa. Server gửi kết quả cho các Client, chuyển phòng sang trạng thái đã kết thúc và lưu kết quả. Điểm được tính theo đơn vị nguyên để biểu diễn thắng một điểm, hòa nửa điểm và thua không điểm.",
    )
    add_body(
        doc,
        "Nếu một người mất kết nối trong lúc chơi, Server chuyển sự kiện này vào cùng luồng xử lý của trận thay vì sửa trạng thái ngay tại phần kết nối. Người chơi bị đánh dấu đã bị loại, sau đó kết quả được tính theo các quy tắc chung. Cách xử lý này giữ thứ tự và tránh hai nơi cùng thay đổi trạng thái trận.",
    )
    add_heading(doc, "2.10 Đồng bộ và hỗ trợ nhiều phòng", "CTDT-H2")
    add_body(
        doc,
        "Mỗi phòng đang thi đấu có vùng trạng thái và bộ xử lý riêng. Các thao tác của phòng được đưa tới đúng bộ xử lý, còn trạng thái chỉ được gửi tới các kết nối thuộc phòng đó. Nhờ sự tách biệt này, Server có thể vận hành nhiều phòng đồng thời mà không trộn người chơi hoặc dữ liệu giữa các trận.",
    )
    add_body(
        doc,
        "Việc gửi trạng thái hoàn chỉnh thay vì chỉ gửi phần thay đổi làm thông điệp lớn hơn, nhưng giúp Client dễ khôi phục nếu bỏ lỡ một lần cập nhật. Mỗi bản nhận mới có thể thay thế trạng thái hiển thị trước đó, giảm phụ thuộc vào lịch sử thông điệp ở phía Client.",
    )
    add_heading(doc, "2.11 Một số giới hạn của thiết kế hiện tại", "CTDT-H2")
    add_bullet(doc, "Kết nối TCP hiện chưa có lớp mã hóa, do đó chỉ phù hợp cho môi trường học tập hoặc mạng tin cậy nếu chưa bổ sung bảo mật đường truyền.")
    add_bullet(doc, "Trạng thái trận đang diễn ra được giữ trong bộ nhớ và không được phục hồi sau khi Server khởi động lại.")
    add_bullet(doc, "Client chưa có cơ chế tiếp tục phiên thi đấu sau khi mất kết nối.")
    add_bullet(doc, "Cấu hình cơ sở dữ liệu cần được thống nhất với hướng dẫn chạy để tránh đặt thông tin nhạy cảm trực tiếp trong source.")
    add_heading(doc, "2.12 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 2 đã trình bày kiến trúc Client-Server, các thành phần chính, cách trao đổi dữ liệu, thiết kế module, lớp và dữ liệu của Bomberman Online Mini. Thiết kế đặt Server ở vị trí quản lý trạng thái trung tâm, đồng thời tách riêng xử lý của từng phòng. Đây là cơ sở để nhiều Client cùng tham gia nhưng vẫn quan sát một diễn biến thống nhất. Các chương tiếp theo trình bày chi tiết cài đặt của từng module trên nền kiến trúc này.",
    )
