"""Chương 3 - Kết nối và xác thực (phần cá nhân A)."""
from __future__ import annotations

from pathlib import Path

from report_common import (
    BLACK,
    WHITE,
    FigureLog,
    add_body,
    add_bullet,
    add_code,
    add_heading,
    add_table,
    arrow,
    font,
    rgb,
    round_box,
    save_image,
    text_centered,
)
from report_diagrams import canvas, flow_diagram, sequence_diagram


def diagram_frame() -> Path:
    image, draw, top = canvas(
        "CẤU TRÚC KHUNG THÔNG ĐIỆP TCP",
        "Mỗi thông điệp gồm 4 byte độ dài và phần nội dung JSON; bên nhận đọc đủ số byte trước khi phân tích",
        980,
    )
    length_box = (80, top + 20, 470, top + 190)
    body_box = (470, top + 20, 1520, top + 190)
    for box in (length_box, body_box):
        draw.rectangle(box, fill=rgb(WHITE), outline=rgb(BLACK), width=4)
    text_centered(draw, length_box, "4 byte\nĐộ dài N\n(int, big-endian)", font(26, True), rgb(BLACK))
    text_centered(
        draw,
        body_box,
        "N byte: JSON UTF-8 của NetworkMessage\n{ \"type\": ..., \"requestId\": ..., \"payload\": { ... } }",
        font(26, True),
        rgb(BLACK),
    )
    text_centered(draw, (80, top + 200, 1520, top + 240), "1 ≤ N ≤ 1 MiB", font(24), rgb(BLACK))

    rows = [
        (
            top + 300,
            "BÊN GỬI - MessageEncoder.encode()",
            ["NetworkMessage", "Jackson: đối tượng → byte UTF-8", "Kiểm tra độ dài", "writeInt(N)\nwrite(N byte), flush"],
        ),
        (
            top + 560,
            "BÊN NHẬN - MessageDecoder.decode()",
            ["readInt() → N", "Kiểm tra 1 ≤ N ≤ 1 MiB", "readFully(N byte)", "JSON → NetworkMessage\n(lỗi: ProtocolException)"],
        ),
    ]
    for y, label, steps in rows:
        text_centered(draw, (80, y, 1520, y + 40), label, font(26, True), rgb(BLACK))
        boxes = []
        x = 80
        for _ in steps:
            boxes.append((x, y + 60, x + 320, y + 190))
            x += 373
        for box, step in zip(boxes, steps):
            round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=14, width=3)
            text_centered(draw, box, step, font(23), rgb(BLACK))
        for first, second in zip(boxes, boxes[1:]):
            arrow(draw, (first[2] + 4, y + 125), (second[0] - 4, y + 125), BLACK, width=4, head=14)
    return save_image(image, "hinh-3-1-khung-thong-diep.png")


def diagram_server_threads() -> Path:
    image, draw, top = canvas(
        "MÔ HÌNH XỬ LÝ KẾT NỐI PHÍA SERVER",
        "Một luồng chấp nhận kết nối; mỗi Client có một ClientSession chạy trên virtual thread riêng",
        1000,
    )
    clients = [(60, top + 40, 290, top + 150), (60, top + 250, 290, top + 360), (60, top + 460, 290, top + 570)]
    for index, box in enumerate(clients, 1):
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=14, width=3)
        text_centered(draw, box, f"Client {index if index < 3 else 'n'}\nSocket TCP", font(23, True), rgb(BLACK))
    acceptor = (360, top + 200, 620, top + 410)
    round_box(draw, acceptor, rgb(WHITE), rgb(BLACK), radius=16, width=4)
    text_centered(draw, acceptor, "Luồng acceptor\ntcp-game-acceptor\n\nServerSocket\n.accept()", font(22, True), rgb(BLACK))
    sessions = [(700, top + 30, 1020, top + 160), (700, top + 240, 1020, top + 370), (700, top + 450, 1020, top + 580)]
    for index, box in enumerate(sessions, 1):
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=14, width=3)
        text_centered(
            draw,
            box,
            f"ClientSession {index if index < 3 else 'n'}\n(virtual thread)\ndecode → dispatch",
            font(22, True),
            rgb(BLACK),
        )
    dispatcher = (1090, top + 200, 1300, top + 410)
    round_box(draw, dispatcher, rgb(WHITE), rgb(BLACK), radius=16, width=4)
    text_centered(draw, dispatcher, "Message\nDispatcher\n\ntheo\nMessageType", font(22, True), rgb(BLACK))
    handlers = ["Auth", "Lobby", "Room", "Game", "Ranking /\nHistory"]
    for index, name in enumerate(handlers):
        box = (1360, top + 20 + index * 118, 1540, top + 118 + index * 118)
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=12, width=3)
        text_centered(draw, box, name + "\nHandler", font(20, True), rgb(BLACK), spacing=2)
        arrow(draw, (1300, top + 305), (1356, (box[1] + box[3]) / 2), BLACK, width=3, head=12)
    for client, session in zip(clients, sessions):
        arrow(draw, (client[2], (client[1] + client[3]) / 2), (acceptor[0] - 4, top + 305), BLACK, width=3, head=12)
        arrow(draw, (acceptor[2], top + 305), (session[0] - 4, (session[1] + session[3]) / 2), BLACK, width=3, head=12)
        arrow(draw, (session[2], (session[1] + session[3]) / 2), (dispatcher[0] - 4, top + 305), BLACK, width=3, head=12)
    registry = (360, top + 660, 930, top + 790)
    writer = (990, top + 660, 1540, top + 790)
    for box in (registry, writer):
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=14, width=3)
    text_centered(draw, registry, "ConnectionManager\nConcurrentHashMap: sessionId → ClientSession", font(22, True), rgb(BLACK))
    text_centered(draw, writer, "SessionWriter\nReentrantLock: mỗi lần ghi trọn một khung", font(22, True), rgb(BLACK))
    arrow(draw, (860, top + 580), (700, top + 656), BLACK, width=3, head=12)
    arrow(draw, (900, top + 580), (1150, top + 656), BLACK, width=3, head=12)
    return save_image(image, "hinh-3-2-mo-hinh-ket-noi-server.png")


def diagram_connect_sequence() -> Path:
    return sequence_diagram(
        "hinh-3-3-thiet-lap-ket-noi.png",
        "THIẾT LẬP KẾT NỐI VÀ GỬI YÊU CẦU ĐẦU TIÊN",
        "Client chỉ mở socket khi có lệnh đầu tiên; phản hồi được ghép với yêu cầu bằng requestId",
        ["Màn hình\nJavaFX", "GameClient\nController", "Pending\nRequests", "GameNetwork\nClient", "TcpGame\nServer", "ClientSession\n(Server)"],
        [
            ("msg", 0, 1, "login(username, password)"),
            ("msg", 1, 2, "register(requestId), trả về CompletableFuture"),
            ("self", 1, "chuyển việc gửi sang luồng client-network-writer"),
            ("msg", 1, 3, "connect(host, port) nếu chưa kết nối"),
            ("msg", 3, 4, "TCP connect, timeout 3 giây"),
            ("self", 4, "accept(), tạo ClientSession"),
            ("msg", 4, 5, "chạy trên virtual thread, đăng ký vào ConnectionManager"),
            ("self", 3, "bật TCP_NODELAY, khởi động luồng đọc server-listener"),
            ("msg", 3, 5, "khung LOGIN_REQUEST (requestId)"),
            ("reply", 5, 3, "khung LOGIN_RESPONSE cùng requestId"),
            ("note", "ClientMessageDispatcher cập nhật ClientState trên luồng JavaFX, sau đó gọi PendingRequests.complete()", 1, 3),
            ("reply", 2, 0, "hoàn thành future; nếu quá 5 giây: TimeoutException"),
        ],
    )


def diagram_login_sequence() -> Path:
    return sequence_diagram(
        "hinh-3-4-dang-nhap.png",
        "TRÌNH TỰ ĐĂNG NHẬP",
        "Server kiểm tra thông tin tài khoản, chặn đăng nhập trùng và gắn người dùng vào phiên TCP hiện tại",
        ["LoginScreen", "Client mạng\n(Controller,\nDispatcher)", "ClientSession", "AuthMessage\nHandler", "Authentication\nService", "OnlineUser\nRegistry"],
        [
            ("self", 0, "InputValidation kiểm tra tên và mật khẩu trước khi gửi"),
            ("msg", 0, 1, "login(username, password)"),
            ("msg", 1, 2, "LOGIN_REQUEST {username, password}"),
            ("msg", 2, 3, "handleLogin(session, message)"),
            ("msg", 3, 4, "login(username, password, sessionId)"),
            ("self", 4, "findByUsername, so khớp mật khẩu BCrypt"),
            ("msg", 4, 5, "markOnline(userId, username, sessionId)"),
            ("reply", 5, 4, "false nếu tài khoản đang trực tuyến"),
            ("reply", 4, 3, "LoginResult"),
            ("msg", 3, 2, "attachAuthenticatedUser(user)"),
            ("reply", 2, 1, "LOGIN_RESPONSE {success, result, userId, username, status}"),
            ("note", "Nếu thành công, Server phát danh sách người trực tuyến và danh sách phòng tới các phiên đang ở sảnh"),
            ("reply", 1, 0, "thành công: chuyển sang HOME; thất bại: thông báo lỗi"),
        ],
    )


def diagram_disconnect_flow() -> Path:
    return flow_diagram(
        "hinh-3-5-xu-ly-mat-ket-noi.png",
        "XỬ LÝ MẤT KẾT NỐI PHÍA SERVER",
        "Vòng đọc kết thúc là tín hiệu duy nhất để Server dọn phiên, phòng và trạng thái trực tuyến",
        [
            ("start", "Vòng đọc của ClientSession kết thúc: EOF, SocketException hoặc ProtocolException"),
            ("step", "ClientSession.close(): đóng socket (chỉ thực hiện một lần)"),
            ("step", "MessageDispatcher.onDisconnect() gọi AuthMessageHandler.handleDisconnect()"),
            ("decision", "Phiên đã đăng nhập?", "Bỏ qua các bước dọn người dùng, chỉ xóa phiên khỏi ConnectionManager", "Có", "Không"),
            ("step", "RoomMessageHandler.handleSessionExit(): rời phòng; nếu đang chơi thì gửi lệnh ngắt kết nối vào trận"),
            ("step", "detachAuthenticatedUser() và markOffline(userId, sessionId)"),
            ("step", "broadcastLobbyUpdates() tới các phiên đang ở sảnh"),
            ("end", "ConnectionManager.remove(sessionId)"),
        ],
    )


def write(doc) -> FigureLog:
    figures = FigureLog(3)

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
        ["ID", "Chức năng", "Thành phần cài đặt", "Vai trò trong giao tiếp mạng"],
        [
            ["KX-01", "Mở cổng và chấp nhận kết nối", "TcpGameServer.start(), acceptConnections()", "Bind cổng 8081, nhận socket mới"],
            ["KX-02", "Quản lý phiên kết nối", "ClientSession, ConnectionManager", "Mỗi socket một phiên, định danh bằng UUID"],
            ["KX-03", "Đóng khung và mã hóa thông điệp", "NetworkMessage, MessageEncoder, MessageDecoder", "Tách thông điệp trên dòng byte TCP"],
            ["KX-04", "Ghi dữ liệu an toàn đa luồng", "SessionWriter", "Không để hai khung ghi xen kẽ"],
            ["KX-05", "Định tuyến thông điệp", "MessageDispatcher.dispatch()", "Chuyển thông điệp tới đúng bộ xử lý"],
            ["KX-06", "Đăng ký tài khoản", "AuthMessageHandler.handleRegister(), AuthenticationService.register()", "REGISTER_REQUEST / REGISTER_RESPONSE"],
            ["KX-07", "Đăng nhập", "AuthMessageHandler.handleLogin(), AuthenticationService.login()", "LOGIN_REQUEST / LOGIN_RESPONSE"],
            ["KX-08", "Đăng xuất và mất kết nối", "handleLogout(), handleDisconnect(), cleanupSession()", "LOGOUT; phát hiện socket đóng"],
            ["KX-09", "Trạng thái trực tuyến", "OnlineUserRegistry", "Chặn một tài khoản ở hai phiên"],
            ["KX-10", "Kết nối phía Client", "GameNetworkClient, ClientNetworkConfig", "Mở socket, vòng đọc, gửi khung"],
            ["KX-11", "Ghép yêu cầu - phản hồi", "PendingRequests", "Theo requestId, giới hạn chờ 5 giây"],
            ["KX-12", "Giao diện đăng nhập", "LoginScreen, ServerAddressPopup, InputValidation", "Thu thập và kiểm tra dữ liệu gửi đi"],
        ],
        [900, 2400, 3500, 2500],
    )

    add_heading(doc, "3.2 Giao thức truyền tin", "CTDT-H2")
    add_heading(doc, "3.2.1 Cấu trúc khung thông điệp", "CTDT-H3")
    add_body(
        doc,
        "TCP truyền dữ liệu dưới dạng dòng byte liên tục và không giữ ranh giới giữa các lần gửi. Vì vậy hai phía cần một quy ước để biết một thông điệp bắt đầu và kết thúc ở đâu. Hệ thống sử dụng kỹ thuật tiền tố độ dài: trước mỗi thông điệp là một số nguyên 4 byte theo thứ tự big-endian cho biết số byte của phần nội dung phía sau. Phần nội dung là đối tượng NetworkMessage được tuần tự hóa thành JSON UTF-8.",
    )
    add_body(
        doc,
        "Đối tượng NetworkMessage chỉ có ba trường. Trường type xác định loại thông điệp theo danh mục MessageType trong module common. Trường requestId do Client sinh ngẫu nhiên dạng UUID cho mỗi lệnh; Server giữ nguyên giá trị này trong phản hồi trực tiếp và để trống với các thông điệp chủ động phát đi. Trường payload chứa dữ liệu riêng của từng loại thông điệp và có thể vắng mặt. Hình 3.1 thể hiện cấu trúc khung và các bước mã hóa, giải mã ở hai phía.",
    )
    figures.figure(
        doc,
        "3.1",
        diagram_frame(),
        "Cấu trúc khung thông điệp và quá trình mã hóa, giải mã",
        "Khung gồm 4 byte độ dài và nội dung JSON; bên gửi ghi độ dài rồi nội dung, bên nhận đọc độ dài, kiểm tra rồi đọc đủ nội dung.",
        "Đóng khung, mã hóa và giải mã thông điệp TCP",
        "MessageEncoder.encode(), MessageDecoder.decode(), NetworkMessage",
    )
    add_body(doc, "Bảng 3.2 mô tả kiểu dữ liệu và ý nghĩa của từng thành phần trong một khung.")
    add_table(
        doc,
        "Bảng 3.2. Các trường của một khung thông điệp",
        ["Thành phần", "Kiểu dữ liệu", "Ý nghĩa"],
        [
            ["Độ dài", "int 4 byte, big-endian", "Số byte của phần JSON; hợp lệ trong khoảng 1 byte đến 1 MiB."],
            ["type", "MessageType (chuỗi)", "Loại thông điệp, dùng để định tuyến tới bộ xử lý tương ứng."],
            ["requestId", "Chuỗi UUID hoặc null", "Ghép phản hồi với yêu cầu; null với thông điệp Server chủ động phát."],
            ["payload", "Đối tượng JSON hoặc null", "Dữ liệu riêng của thông điệp, ví dụ tên đăng nhập và mật khẩu."],
        ],
        [1800, 2600, 4900],
    )

    add_heading(doc, "3.2.2 Mã hóa và giải mã", "CTDT-H3")
    add_body(
        doc,
        "Ở phía gửi, MessageEncoder dùng Jackson chuyển NetworkMessage thành mảng byte, kiểm tra kích thước không vượt quá 1 MiB, sau đó ghi độ dài bằng writeInt() và ghi toàn bộ nội dung trước khi flush. Ở phía nhận, MessageDecoder đọc 4 byte độ dài, từ chối giá trị không nằm trong khoảng cho phép rồi dùng readFully() để đọc đúng số byte đã khai báo. Nhờ readFully(), việc giải mã không phụ thuộc cách hệ điều hành chia nhỏ hoặc gộp các gói TCP; nhiều lần gọi decode() liên tiếp trên cùng một dòng sẽ đọc lần lượt từng khung mà không lấn sang khung kế tiếp.",
    )
    add_body(
        doc,
        "Bộ giải mã bật tùy chọn FAIL_ON_TRAILING_TOKENS, do đó phần JSON có dữ liệu thừa hoặc sai cú pháp đều sinh ProtocolException. Server coi đây là vi phạm giao thức và đóng phiên tương ứng thay vì cố gắng đọc tiếp một dòng byte đã mất đồng bộ. Đoạn mã dưới đây trích từ phương thức giải mã dùng chung cho Server và Client.",
    )
    add_code(
        doc,
        """public NetworkMessage decode(InputStream input) throws IOException {
    DataInputStream dataInput = new DataInputStream(input);
    int payloadLength = dataInput.readInt();
    validatePayloadLength(payloadLength);          // 1..1 MiB, nếu sai: ProtocolException
    byte[] payload = new byte[payloadLength];
    dataInput.readFully(payload);                  // đọc đủ N byte, không phụ thuộc gói TCP
    try {
        return messageReader.readValue(payload);   // JSON -> NetworkMessage
    } catch (JsonProcessingException exception) {
        throw new ProtocolException("Malformed JSON payload", exception);
    }
}""",
    )

    add_heading(doc, "3.2.3 Các thông điệp thuộc phần kết nối và xác thực", "CTDT-H3")
    add_body(
        doc,
        "Bảng 3.3 liệt kê các thông điệp do phần này xử lý. Các yêu cầu đăng ký và đăng nhập có kiểu phản hồi riêng mang cùng requestId; đăng xuất không có phản hồi riêng vì Client chủ động xóa trạng thái đăng nhập ngay sau khi gửi.",
    )
    add_table(
        doc,
        "Bảng 3.3. Thông điệp thuộc phần kết nối và xác thực",
        ["Thông điệp", "Chiều", "Dữ liệu", "Xử lý"],
        [
            ["REGISTER_REQUEST", "Client → Server", "username, password", "AuthMessageHandler.handleRegister()"],
            ["REGISTER_RESPONSE", "Server → Client", "success, result, userId, username", "Client hiển thị kết quả đăng ký"],
            ["LOGIN_REQUEST", "Client → Server", "username, password", "AuthMessageHandler.handleLogin()"],
            ["LOGIN_RESPONSE", "Server → Client", "success, result, userId, username, status", "Client lưu danh tính, chuyển sang màn hình chính"],
            ["LOGOUT", "Client → Server", "Không có", "Dọn phòng và trạng thái trực tuyến, giữ socket"],
            ["PING / PONG", "Client → Server / ngược lại", "Không có", "Server trả PONG cùng requestId"],
            ["ERROR", "Server → Client", "code, message", "Client hiển thị thông báo hoặc kết thúc yêu cầu đang chờ"],
        ],
        [2200, 1900, 2500, 2700],
    )

    add_heading(doc, "3.3 Tổ chức kết nối phía Server", "CTDT-H2")
    add_body(
        doc,
        "TcpGameServer là một thành phần SmartLifecycle của Spring, vì vậy cổng TCP được mở tự động khi ứng dụng Server khởi động và được đóng khi ứng dụng dừng. Khi khởi động, Server tạo ServerSocket, bật SO_REUSEADDR và bind vào cổng cấu hình bomberman.tcp.port, mặc định là 8081. Một luồng nền tảng duy nhất tên tcp-game-acceptor lặp lại lời gọi accept() để nhận kết nối mới.",
    )
    add_body(
        doc,
        "Với mỗi socket được chấp nhận, Server tạo một ClientSession và giao cho bộ thực thi virtual thread. Mỗi phiên có mã định danh UUID, bật TCP_NODELAY để các thông điệp nhỏ được gửi ngay mà không chờ gộp gói, đăng ký vào ConnectionManager rồi chạy vòng lặp đọc: giải mã một khung, chuyển cho MessageDispatcher và tiếp tục khung kế tiếp. Vì virtual thread có chi phí thấp, Server có thể dùng mô hình một luồng cho mỗi kết nối với mã đọc dạng chặn đơn giản mà vẫn phục vụ được nhiều Client.",
    )
    add_body(
        doc,
        "Việc ghi dữ liệu có đặc điểm khác với việc đọc. Một phiên có thể nhận dữ liệu gửi đi từ nhiều luồng khác nhau, chẳng hạn luồng của chính phiên đó khi trả phản hồi, luồng của phiên khác khi phát trạng thái phòng và luồng vòng lặp trận đấu khi phát trạng thái trò chơi. SessionWriter dùng ReentrantLock để mỗi lần ghi là trọn vẹn một khung, tránh trường hợp byte của hai thông điệp xen vào nhau. Hình 3.2 tổng hợp mô hình luồng của phía Server.",
    )
    figures.figure(
        doc,
        "3.2",
        diagram_server_threads(),
        "Mô hình xử lý kết nối phía Server",
        "Luồng acceptor nhận socket, tạo ClientSession trên virtual thread, các phiên chuyển thông điệp cho MessageDispatcher và các handler.",
        "Chấp nhận nhiều kết nối, một phiên cho mỗi Client, định tuyến thông điệp",
        "TcpGameServer, ClientSession, ConnectionManager, SessionWriter, MessageDispatcher",
    )
    add_body(
        doc,
        "Đoạn mã sau thể hiện vòng đời của một phiên. Khối finally bảo đảm phần dọn dẹp luôn được thực hiện, bất kể vòng đọc kết thúc do Client đóng kết nối, do lỗi mạng hay do vi phạm giao thức.",
    )
    add_code(
        doc,
        """public void run() {                               // trích rút gọn từ ClientSession
    boolean registered = false;
    try {
        connectionManager.add(this);
        registered = true;
        while (!closed.get()) {
            NetworkMessage message = decoder.decode(input);
            dispatcher.dispatch(this, message);
        }
    } catch (ProtocolException exception) { ... }       // vi phạm giao thức
      catch (IOException exception) { ... }             // Client đóng kết nối, lỗi mạng
    finally {
        close();                                         // đóng socket
        try {
            dispatcher.onDisconnect(this);               // dọn phòng, trạng thái trực tuyến
        } finally {
            if (registered) connectionManager.remove(sessionId);
        }
    }
}""",
    )
    add_body(
        doc,
        "MessageDispatcher là điểm định tuyến duy nhất. Dựa trên trường type, thông điệp được chuyển tới AuthMessageHandler, LobbyMessageHandler, RoomMessageHandler, GameMessageHandler, RankingMessageHandler hoặc HistoryMessageHandler. Riêng PING được trả lời ngay bằng PONG mang cùng requestId. Nhờ vậy lớp mạng không phụ thuộc vào nghiệp vụ, còn các bộ xử lý nghiệp vụ không cần biết thông điệp đã được đọc từ socket như thế nào.",
    )

    add_heading(doc, "3.4 Tổ chức kết nối phía Client", "CTDT-H2")
    add_body(
        doc,
        "Địa chỉ Server được ClientNetworkConfig xác định theo thứ tự ưu tiên: thuộc tính hệ thống, biến môi trường BOMBERMAN_SERVER_HOST và BOMBERMAN_TCP_PORT, địa chỉ người chơi đã lưu từ hộp thoại chọn Server, cuối cùng là tệp client.properties đi kèm ứng dụng. Cơ chế này cho phép cùng một bản đóng gói kết nối tới Server trên máy cục bộ hoặc trên một máy khác trong mạng LAN.",
    )
    add_body(
        doc,
        "Client không mở kết nối ngay khi khởi động. GameNetworkClient chỉ kết nối khi lệnh đầu tiên được gửi, với thời gian chờ kết nối 3 giây, sau đó bật TCP_NODELAY và khởi động một virtual thread tên server-listener để đọc dữ liệu từ Server. Mọi thao tác ghi được GameClientController đưa vào một bộ thực thi đơn luồng client-network-writer. Cách tổ chức này có hai tác dụng: giao diện JavaFX không bị chặn khi mạng chậm, và các lệnh được gửi đúng thứ tự người chơi thao tác.",
    )
    add_body(
        doc,
        "Các lệnh cần phản hồi được đăng ký vào PendingRequests theo requestId trước khi gửi. Khi phản hồi mang cùng requestId tới, ClientMessageDispatcher cập nhật trạng thái rồi hoàn thành CompletableFuture tương ứng trên luồng JavaFX, nhờ đó màn hình có thể kết thúc trạng thái chờ và hiển thị kết quả. Nếu sau 5 giây chưa có phản hồi, yêu cầu bị hủy với TimeoutException và người chơi nhận thông báo Server không phản hồi. Hình 3.3 mô tả trình tự từ lúc người chơi thao tác tới lúc nhận phản hồi đầu tiên.",
    )
    figures.figure(
        doc,
        "3.3",
        diagram_connect_sequence(),
        "Trình tự thiết lập kết nối và ghép phản hồi theo requestId",
        "Màn hình gọi controller, controller đăng ký yêu cầu, mở socket khi cần, Server accept và tạo phiên, phản hồi được ghép theo requestId.",
        "Kết nối TCP phía Client, gửi yêu cầu và nhận phản hồi",
        "GameClientController, GameNetworkClient, PendingRequests, TcpGameServer",
    )

    add_heading(doc, "3.5 Đăng ký và đăng nhập", "CTDT-H2")
    add_heading(doc, "3.5.1 Đăng ký tài khoản", "CTDT-H3")
    add_body(
        doc,
        "Khi nhận REGISTER_REQUEST, AuthenticationService cắt khoảng trắng hai đầu tên đăng nhập, kiểm tra tên có từ 1 đến 50 ký tự và mật khẩu không rỗng. Nếu tên chưa tồn tại, mật khẩu được băm bằng BCrypt và tài khoản được lưu vào cơ sở dữ liệu; mật khẩu gốc không bao giờ được lưu. Trường hợp hai Client đăng ký cùng một tên gần như đồng thời, ràng buộc duy nhất của cơ sở dữ liệu sinh DataIntegrityViolationException và bộ xử lý chuyển lỗi này thành mã USERNAME_ALREADY_EXISTS thay vì làm hỏng phiên.",
    )
    add_heading(doc, "3.5.2 Đăng nhập và gắn người dùng với phiên", "CTDT-H3")
    add_body(
        doc,
        "Trước khi gửi, LoginScreen dùng InputValidation kiểm tra dữ liệu theo cùng quy tắc với Server để phát hiện lỗi sớm. Ở phía Server, AuthMessageHandler từ chối yêu cầu nếu phiên đã đăng nhập, sau đó AuthenticationService tìm tài khoản và so khớp mật khẩu với giá trị băm. Nếu thông tin đúng, OnlineUserRegistry.markOnline() dùng thao tác putIfAbsent trên ConcurrentHashMap để ghi nhận người dùng trực tuyến; thao tác này thất bại khi tài khoản đang hoạt động ở một phiên khác, nhờ đó một tài khoản không thể đăng nhập đồng thời trên hai máy.",
    )
    add_body(
        doc,
        "Khi đăng nhập thành công, người dùng được gắn vào ClientSession bằng thao tác so sánh và gán nguyên tử. Từ thời điểm này, các yêu cầu sau trên cùng kết nối không cần gửi lại thông tin xác thực vì Server xác định người gửi từ phiên TCP. Server trả LOGIN_RESPONSE, sau đó phát cập nhật sảnh để những người chơi khác thấy người vừa trực tuyến. Hình 3.4 thể hiện trình tự đầy đủ.",
    )
    figures.figure(
        doc,
        "3.4",
        diagram_login_sequence(),
        "Trình tự đăng nhập",
        "LoginScreen kiểm tra dữ liệu, gửi LOGIN_REQUEST; Server kiểm tra mật khẩu, đánh dấu trực tuyến, gắn người dùng vào phiên và trả LOGIN_RESPONSE.",
        "Xác thực người dùng và gắn danh tính với phiên TCP",
        "LoginScreen, AuthMessageHandler.handleLogin(), AuthenticationService.login(), OnlineUserRegistry.markOnline()",
    )
    add_body(
        doc,
        "Kết quả xác thực được biểu diễn bằng AuthResultCode để Client hiển thị thông báo phù hợp. Bảng 3.4 liệt kê các mã kết quả, điều kiện phát sinh và thông báo tương ứng ở Client.",
    )
    add_table(
        doc,
        "Bảng 3.4. Mã kết quả xác thực và cách Client phản hồi",
        ["Mã kết quả", "Điều kiện phát sinh", "Thông báo ở Client"],
        [
            ["SUCCESS", "Đăng ký hoặc đăng nhập hợp lệ", "Tạo tài khoản thành công / chuyển sang màn hình chính"],
            ["INVALID_REQUEST", "Thiếu hoặc sai định dạng payload", "Invalid request. Please try again."],
            ["INVALID_USERNAME", "Tên rỗng hoặc dài hơn 50 ký tự", "Username must be 1–50 characters."],
            ["INVALID_PASSWORD", "Mật khẩu rỗng", "Password is required."],
            ["USERNAME_ALREADY_EXISTS", "Tên đăng nhập đã được sử dụng", "That username is already taken."],
            ["INVALID_CREDENTIALS", "Sai tên đăng nhập hoặc mật khẩu", "Wrong username or password."],
            ["ACCOUNT_ALREADY_ONLINE", "Tài khoản đang trực tuyến ở phiên khác", "This account is already online on another device."],
            ["SESSION_ALREADY_AUTHENTICATED", "Phiên hiện tại đã đăng nhập", "You are already logged in."],
        ],
        [3000, 3100, 3200],
    )

    add_heading(doc, "3.6 Kiểm tra kết nối, đăng xuất và mất kết nối", "CTDT-H2")
    add_body(
        doc,
        "Server hỗ trợ cặp thông điệp PING và PONG để kiểm tra một kết nối còn hoạt động: mỗi PING được trả lời bằng PONG mang cùng requestId. Client JavaFX hiện không gửi PING định kỳ; việc phát hiện mất kết nối ở phía Client dựa vào vòng đọc của socket.",
    )
    add_body(
        doc,
        "Khi người chơi đăng xuất, Client gửi LOGOUT và xóa trạng thái đăng nhập cục bộ nhưng vẫn giữ socket để có thể đăng nhập lại ngay. Server xử lý LOGOUT bằng cùng thủ tục cleanupSession() như khi mất kết nối, chỉ khác là socket không bị đóng.",
    )
    add_body(
        doc,
        "Khi kết nối bị đóng đột ngột, vòng đọc của ClientSession kết thúc với ngoại lệ và chuyển sang khối dọn dẹp. Thủ tục cleanupSession() trước hết yêu cầu phần sảnh và phòng chờ đưa người dùng ra khỏi phòng; nếu phòng đang thi đấu, một lệnh ngắt kết nối được đưa vào vòng lặp trận đấu để người chơi bị loại theo đúng thứ tự xử lý của trận. Chi tiết của hai bước này được trình bày ở mục 4.4 và mục 5.6. Sau đó danh tính được tách khỏi phiên và OnlineUserRegistry xóa người dùng khỏi danh sách trực tuyến. Việc xóa chỉ thực hiện khi sessionId khớp, tránh trường hợp phiên cũ đóng muộn làm mất trạng thái của một phiên mới. Hình 3.5 mô tả luồng xử lý này.",
    )
    figures.figure(
        doc,
        "3.5",
        diagram_disconnect_flow(),
        "Luồng xử lý mất kết nối phía Server",
        "Vòng đọc kết thúc, đóng socket, gọi onDisconnect, nếu phiên đã đăng nhập thì rời phòng, đánh dấu ngoại tuyến, phát cập nhật sảnh và xóa phiên.",
        "Phát hiện mất kết nối và dọn phiên, trạng thái trực tuyến",
        "ClientSession.run(), MessageDispatcher.onDisconnect(), AuthMessageHandler.cleanupSession()",
    )
    add_body(
        doc,
        "Ở phía Client, khi vòng đọc kết thúc, ClientMessageDispatcher.onDisconnected() chuyển sang luồng JavaFX, xóa trạng thái đăng nhập, hủy mọi yêu cầu đang chờ trong PendingRequests, đưa người chơi về màn hình đăng nhập và hiển thị hộp thoại thông báo mất kết nối tới địa chỉ Server hiện tại.",
    )

    add_heading(doc, "3.7 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Giao diện của phần này là màn hình đăng nhập và đăng ký. Người dùng nhập tên và mật khẩu, chọn đăng nhập hoặc tạo tài khoản, đồng thời có thể mở hộp thoại để đổi địa chỉ Server trước khi kết nối. Hình 3.6 đến Hình 3.8 minh họa các trạng thái chính.",
    )
    figures.screenshot(
        doc,
        "3.6",
        "c3-login.png",
        "Màn hình đăng nhập và đăng ký",
        "Màn hình đăng nhập của Client JavaFX.",
        "Màn hình LoginScreen: tab đăng nhập với ô USERNAME, PASSWORD và nút LOGIN; nếu được, chụp thêm tab đăng ký có ô CONFIRM PASSWORD",
        "Thu thập thông tin xác thực, gửi LOGIN_REQUEST / REGISTER_REQUEST",
        "LoginScreen, InputValidation, GameClientController.login()",
    )
    figures.screenshot(
        doc,
        "3.7",
        "c3-server-address.png",
        "Hộp thoại chọn địa chỉ Server",
        "Hộp thoại nhập host:port của Server.",
        "Hộp thoại Server Address mở từ màn hình đăng nhập, đang hiển thị host:port",
        "Cấu hình địa chỉ TCP của Server trước khi kết nối",
        "ServerAddressPopup, ClientNetworkConfig, UserPreferences",
    )
    figures.screenshot(
        doc,
        "3.8",
        "c3-login-error.png",
        "Thông báo lỗi khi đăng nhập",
        "Thông báo lỗi sai mật khẩu hoặc tài khoản đang trực tuyến.",
        "Thông báo lỗi sau khi đăng nhập sai mật khẩu hoặc đăng nhập một tài khoản đang trực tuyến ở máy khác",
        "Hiển thị mã kết quả xác thực từ LOGIN_RESPONSE",
        "ClientMessageDispatcher.describe(), AuthResultCode",
    )

    add_heading(doc, "3.8 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(
        doc,
        "Bảng 3.5 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng mà phần kết nối và xác thực đã thực hiện.",
    )
    figures.mapping_table(doc, "Bảng 3.5. Ánh xạ hình vẽ với chức năng phần kết nối và xác thực")

    add_heading(doc, "3.9 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 3 đã trình bày giao thức đóng khung thông điệp bằng tiền tố độ dài, mô hình một luồng acceptor kết hợp một virtual thread cho mỗi phiên ở Server, cơ chế ghi tuần tự bằng khóa, tầng kết nối phía Client với luồng đọc, luồng ghi và cơ chế ghép phản hồi theo requestId. Trên nền đó, các chức năng đăng ký, đăng nhập, đăng xuất và xử lý mất kết nối bảo đảm mỗi phiên TCP gắn với đúng một người dùng. Chương tiếp theo sử dụng các phiên đã xác thực này để tổ chức sảnh và phòng chờ.",
    )
    return figures
