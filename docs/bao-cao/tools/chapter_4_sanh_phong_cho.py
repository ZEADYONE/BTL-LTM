"""Chương 4 - Sảnh và phòng chờ (phần cá nhân B)."""
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


def _state_row(draw, top: int, title: str, states: list[str], forward: list[str], back: list[tuple[int, int, str]], xs):
    text_centered(draw, (60, top, 1540, top + 40), title, font(26, True), rgb(BLACK))
    box_top, box_bottom = top + 110, top + 200
    boxes = [(x, box_top, x + 250, box_bottom) for x in xs]
    for box, label in zip(boxes, states):
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=40, width=4)
        text_centered(draw, box, label, font(24, True), rgb(BLACK))
    for index, label in enumerate(forward):
        first, second = boxes[index], boxes[index + 1]
        y = box_top + 45
        arrow(draw, (first[2] + 4, y), (second[0] - 4, y), BLACK, width=4, head=16)
        text_centered(draw, (first[2], box_top - 58, second[0], y - 6), label, font(21), rgb(BLACK), spacing=2)
    for source, target, label in back:
        sx = (boxes[source][0] + boxes[source][2]) / 2 - 30
        tx = (boxes[target][0] + boxes[target][2]) / 2 + 30
        low = box_bottom + 55
        draw.line((sx, box_bottom, sx, low), fill=rgb(BLACK), width=4)
        draw.line((sx, low, tx, low), fill=rgb(BLACK), width=4)
        arrow(draw, (tx, low), (tx, box_bottom + 4), BLACK, width=4, head=16)
        text_centered(draw, (min(sx, tx), low + 4, max(sx, tx), low + 44), label, font(21), rgb(BLACK))


def diagram_states() -> Path:
    image, draw, top = canvas(
        "TRẠNG THÁI NGƯỜI CHƠI VÀ PHÒNG",
        "Server giữ hai máy trạng thái trong bộ nhớ; mọi chuyển trạng thái đều do Server thực hiện sau khi kiểm tra",
        900,
    )
    _state_row(
        draw,
        top,
        "Trạng thái người chơi - OnlineUserRegistry",
        ["Ngoại tuyến", "FREE", "IN_ROOM", "PLAYING"],
        ["đăng nhập", "tạo / vào phòng", "chủ phòng bắt đầu"],
        [(1, 0, "đăng xuất / mất kết nối"), (2, 1, "rời phòng"), (3, 2, "trận kết thúc")],
        [70, 470, 870, 1270],
    )
    _state_row(
        draw,
        top + 350,
        "Trạng thái phòng - GameRoom / RoomStatus",
        ["WAITING", "PLAYING", "FINISHED"],
        ["START_GAME hợp lệ", "có người thắng hoặc hòa"],
        [(2, 0, "PLAY_AGAIN: đặt lại sẵn sàng")],
        [170, 670, 1170],
    )
    text_centered(
        draw,
        (60, top + 650, 1540, top + 700),
        "Phòng bị xóa khỏi RoomManager khi người cuối cùng rời phòng",
        font(22),
        rgb(BLACK),
    )
    return save_image(image, "hinh-4-1-trang-thai-nguoi-choi-phong.png")


def diagram_lobby_sequence() -> Path:
    return sequence_diagram(
        "hinh-4-2-dong-bo-sanh.png",
        "TẢI VÀ ĐỒNG BỘ SẢNH",
        "Server trả dữ liệu theo yêu cầu và chủ động phát lại toàn bộ danh sách khi sảnh thay đổi",
        ["Client A\n(vừa đăng nhập)", "Client B\n(đang ở sảnh)", "AuthMessage\nHandler", "LobbyMessage\nHandler", "LobbyService", "OnlineUserRegistry\nRoomManager"],
        [
            ("note", "Client A vừa đăng nhập thành công (mục 3.5)"),
            ("msg", 2, 4, "broadcastLobbyUpdates()"),
            ("msg", 4, 5, "đọc danh sách trực tuyến và snapshotRooms()"),
            ("self", 4, "chọn các phiên có trạng thái FREE"),
            ("reply", 4, 1, "ONLINE_USERS_UPDATE, ROOM_LIST_UPDATE (requestId = null)"),
            ("reply", 4, 0, "cùng hai thông điệp tới Client A"),
            ("msg", 0, 3, "ONLINE_USERS_REQUEST, ROOM_LIST_REQUEST"),
            ("msg", 3, 4, "sendOnlineUsers(), sendRoomList()"),
            ("reply", 4, 0, "ONLINE_USERS_UPDATE, ROOM_LIST_UPDATE cùng requestId"),
            ("self", 0, "ClientState cập nhật, màn hình vẽ lại danh sách"),
        ],
    )


def diagram_room_sequence() -> Path:
    return sequence_diagram(
        "hinh-4-3-tao-tham-gia-phong.png",
        "TẠO PHÒNG VÀ THAM GIA PHÒNG",
        "Thành công được thể hiện bằng ROOM_STATE; mọi thành viên nhận lại trạng thái đầy đủ của phòng",
        ["Client A\n(chủ phòng)", "Client B", "RoomMessage\nHandler", "RoomManager", "OnlineUser\nRegistry", "LobbyService"],
        [
            ("msg", 0, 2, "CREATE_ROOM {roomName}"),
            ("msg", 2, 3, "createRoom(user, sessionId, roomName)"),
            ("self", 3, "tên 1-60 ký tự, chưa ở phòng nào; tạo roomId UUID, A là chủ phòng"),
            ("msg", 2, 4, "updateStatus(A, IN_ROOM)"),
            ("reply", 2, 0, "ROOM_STATE {member = true, room} cùng requestId"),
            ("msg", 2, 5, "broadcastLobbyUpdates()"),
            ("msg", 1, 2, "JOIN_ROOM {roomId}"),
            ("msg", 2, 3, "joinRoom(): phòng tồn tại, đang WAITING, chưa đủ 4 người"),
            ("msg", 2, 4, "updateStatus(B, IN_ROOM)"),
            ("reply", 2, 1, "ROOM_STATE cùng requestId"),
            ("reply", 2, 0, "ROOM_STATE (requestId = null)"),
            ("msg", 2, 5, "broadcastLobbyUpdates()"),
        ],
    )


def diagram_quick_play() -> Path:
    return flow_diagram(
        "hinh-4-4-choi-nhanh.png",
        "LUỒNG CHƠI NHANH (QUICK PLAY)",
        "Client chọn phòng dựa trên danh sách phòng đã nhận; Server vẫn là nơi quyết định vào phòng có thành công hay không",
        [
            ("start", "Người chơi nhấn PLAY ở màn hình Home"),
            ("step", "Lọc phòng WAITING còn chỗ, sắp xếp theo số người giảm dần"),
            ("decision", "Còn phòng ứng viên và chưa thử quá 2 phòng?", "Gửi CREATE_ROOM với tên \"<tên>'s Room\"", "Có", "Không"),
            ("step", "Gửi JOIN_ROOM tới phòng kế tiếp, không hiển thị lỗi tự động"),
            ("decision", "Server trả về ERROR?", "Nhận ROOM_STATE: vào phòng chờ", "Có", "Không"),
            ("decision", "Mã lỗi ROOM_FULL, ROOM_NOT_WAITING hoặc ROOM_NOT_FOUND?", "Dừng và báo lỗi", "Có", "Không"),
            ("end", "Chuyển sang phòng ứng viên kế tiếp"),
        ],
        loop=(6, 2, "Thử lại"),
    )


def diagram_start_sequence() -> Path:
    return sequence_diagram(
        "hinh-4-5-san-sang-bat-dau.png",
        "SẴN SÀNG VÀ BẮT ĐẦU TRẬN",
        "Chỉ chủ phòng được bắt đầu; Server kiểm tra lại toàn bộ điều kiện trước khi chuyển phòng sang PLAYING",
        ["Client A\n(chủ phòng)", "Client B", "RoomMessage\nHandler", "RoomManager", "OnlineUser\nRegistry", "GameSession\nManager"],
        [
            ("msg", 1, 2, "READY {ready = true}"),
            ("msg", 2, 3, "setReady(B, true)"),
            ("reply", 2, 1, "ROOM_STATE cùng requestId"),
            ("reply", 2, 0, "ROOM_STATE (requestId = null)"),
            ("note", "Client A cũng gửi READY; LobbyStatus chỉ bật nút START GAME khi đủ điều kiện", 0, 2),
            ("msg", 0, 2, "START_GAME"),
            ("msg", 2, 3, "startGame(A): chủ phòng, WAITING, ít nhất 2 người, tất cả sẵn sàng"),
            ("self", 3, "phòng chuyển sang PLAYING"),
            ("msg", 2, 4, "updateStatus(A, B, PLAYING)"),
            ("msg", 2, 5, "startGame(roomSnapshot): tạo vòng lặp trận (Chương 5)"),
            ("reply", 2, 0, "ROOM_STATE status = PLAYING cùng requestId"),
            ("reply", 2, 1, "ROOM_STATE status = PLAYING"),
            ("self", 1, "beginGame(), chuyển sang màn hình GAME"),
        ],
    )


def write(doc) -> FigureLog:
    figures = FigureLog(4)

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
        ["ID", "Chức năng", "Thành phần cài đặt", "Thông điệp liên quan"],
        [
            ["SP-01", "Danh sách người trực tuyến", "LobbyService.sendOnlineUsers(), OnlineUserRegistry", "ONLINE_USERS_REQUEST / UPDATE"],
            ["SP-02", "Danh sách phòng", "LobbyService.sendRoomList(), RoomManager.snapshotRooms()", "ROOM_LIST_REQUEST / UPDATE"],
            ["SP-03", "Phát cập nhật sảnh", "LobbyService.broadcastLobbyUpdates()", "ONLINE_USERS_UPDATE, ROOM_LIST_UPDATE"],
            ["SP-04", "Tạo phòng", "RoomMessageHandler.handleCreateRoom(), RoomManager.createRoom()", "CREATE_ROOM → ROOM_STATE"],
            ["SP-05", "Tham gia phòng", "handleJoinRoom(), RoomManager.joinRoom()", "JOIN_ROOM → ROOM_STATE"],
            ["SP-06", "Chơi nhanh", "HomeScreen.quickPlay(), QuickPlay", "JOIN_ROOM hoặc CREATE_ROOM"],
            ["SP-07", "Rời phòng, chuyển chủ phòng", "handleLeaveRoom(), GameRoom.removePlayer()", "LEAVE_ROOM → ROOM_STATE"],
            ["SP-08", "Sẵn sàng", "handleReady(), RoomManager.setReady()", "READY → ROOM_STATE"],
            ["SP-09", "Bắt đầu trận", "handleStartGame(), RoomManager.startGame()", "START_GAME → ROOM_STATE"],
            ["SP-10", "Chơi lại", "handlePlayAgain(), RoomManager.prepareRematch()", "PLAY_AGAIN → ROOM_STATE"],
            ["SP-11", "Dọn phòng khi mất kết nối", "RoomMessageHandler.handleSessionExit()", "ROOM_STATE tới người còn lại"],
            ["SP-12", "Giao diện sảnh và phòng", "HomeScreen, RoomBrowserScreen, RoomLobbyScreen, LobbyStatus", "Hiển thị dữ liệu nhận được"],
        ],
        [900, 2300, 3600, 2500],
    )

    add_heading(doc, "4.2 Mô hình dữ liệu và trạng thái", "CTDT-H2")
    add_body(
        doc,
        "Dữ liệu sảnh và phòng chỉ tồn tại trong bộ nhớ Server vì nó thay đổi liên tục và không cần giữ sau khi Server dừng. Mỗi GameRoom gồm mã phòng dạng UUID, tên phòng, mã người chủ phòng, trạng thái và danh sách thành viên. Danh sách thành viên dùng LinkedHashMap để giữ thứ tự vào phòng; thứ tự này được dùng khi cần chọn chủ phòng mới. Mỗi thành viên (RoomPlayer) lưu mã người dùng, tên, mã phiên TCP và cờ sẵn sàng.",
    )
    add_body(
        doc,
        "RoomManager giữ hai bảng băm đồng thời: rooms ánh xạ mã phòng tới phòng và roomByUser ánh xạ người dùng tới phòng đang tham gia. Bảng thứ hai hiện thực quy tắc mỗi người chỉ ở một phòng. Mọi thao tác thay đổi phòng đều chạy dưới một mutationLock, nên việc kiểm tra điều kiện và cập nhật hai bảng diễn ra như một bước nguyên tử. Ví dụ, hai người cùng gửi JOIN_ROOM vào chỗ trống cuối cùng thì chỉ một người thành công, người còn lại nhận ROOM_FULL. Sau mỗi thao tác, RoomManager trả về một RoomSnapshot bất biến để các bước gửi dữ liệu không phải giữ khóa.",
    )
    add_body(
        doc,
        "Song song với trạng thái phòng, OnlineUserRegistry lưu trạng thái của từng người chơi trực tuyến: FREE khi đang ở sảnh, IN_ROOM khi ở phòng chờ và PLAYING khi đang thi đấu. Trạng thái này quyết định ai nhận cập nhật sảnh và được hiển thị trong danh sách người trực tuyến. Hình 4.1 thể hiện hai máy trạng thái và các sự kiện gây chuyển trạng thái.",
    )
    figures.figure(
        doc,
        "4.1",
        diagram_states(),
        "Trạng thái người chơi và trạng thái phòng",
        "Người chơi chuyển giữa ngoại tuyến, FREE, IN_ROOM, PLAYING; phòng chuyển giữa WAITING, PLAYING, FINISHED.",
        "Quản lý trạng thái người chơi và vòng đời phòng",
        "OnlineUserRegistry.updateStatus(), GameRoom.start(), finish(), prepareRematch()",
    )
    add_body(
        doc,
        "Bảng 4.2 liệt kê các thông điệp của phần này. Các lệnh thao tác phòng không có kiểu phản hồi riêng: khi thành công, Server trả ROOM_STATE mang requestId của người yêu cầu; khi thất bại, Server trả ERROR với mã RoomResultCode.",
    )
    add_table(
        doc,
        "Bảng 4.2. Thông điệp thuộc phần sảnh và phòng chờ",
        ["Thông điệp", "Chiều", "Dữ liệu", "Người nhận"],
        [
            ["ONLINE_USERS_REQUEST", "Client → Server", "Không có", "LobbyMessageHandler"],
            ["ONLINE_USERS_UPDATE", "Server → Client", "users[]: userId, username, status", "Người yêu cầu hoặc mọi phiên FREE"],
            ["ROOM_LIST_REQUEST", "Client → Server", "Không có", "LobbyMessageHandler"],
            ["ROOM_LIST_UPDATE", "Server → Client", "rooms[]: roomId, roomName, hostUserId, playerCount, maxPlayers, status", "Người yêu cầu hoặc mọi phiên FREE"],
            ["CREATE_ROOM", "Client → Server", "roomName", "RoomMessageHandler"],
            ["JOIN_ROOM", "Client → Server", "roomId", "RoomMessageHandler"],
            ["LEAVE_ROOM", "Client → Server", "Không có", "RoomMessageHandler"],
            ["READY", "Client → Server", "ready", "RoomMessageHandler"],
            ["START_GAME", "Client → Server", "Không có", "RoomMessageHandler"],
            ["PLAY_AGAIN", "Client → Server", "Không có", "RoomMessageHandler"],
            ["ROOM_STATE", "Server → Client", "member, room: roomId, roomName, hostUserId, players[], maxPlayers, status", "Các thành viên của phòng"],
        ],
        [2300, 1700, 3300, 2000],
    )
    add_body(
        doc,
        "Dữ liệu thành viên gửi cho Client chỉ gồm mã người dùng, tên và cờ sẵn sàng. Mã phiên TCP chỉ được Server dùng nội bộ để tìm kết nối cần gửi và không được đưa ra ngoài.",
    )

    add_heading(doc, "4.3 Đồng bộ sảnh", "CTDT-H2")
    add_body(
        doc,
        "Có hai cách Client nhận dữ liệu sảnh. Cách thứ nhất là yêu cầu trực tiếp: khi hiển thị màn hình Home hoặc Room Browser, Client gửi ONLINE_USERS_REQUEST và ROOM_LIST_REQUEST, Server kiểm tra phiên đã đăng nhập rồi trả ngay hai danh sách mang cùng requestId. Cách thứ hai là Server chủ động phát: mỗi khi sảnh thay đổi, LobbyService.broadcastLobbyUpdates() tạo hai thông điệp một lần rồi gửi tới mọi phiên đang ở trạng thái FREE.",
    )
    add_body(
        doc,
        "Các sự kiện gọi phát cập nhật sảnh gồm đăng nhập, đăng xuất hoặc mất kết nối, tạo phòng, tham gia phòng, rời phòng, bắt đầu trận, chơi lại và kết thúc trận. Người chơi đang ở phòng hoặc đang thi đấu không nhận các cập nhật này, giúp giảm lưu lượng không cần thiết khi nhiều phòng hoạt động cùng lúc. Server luôn gửi toàn bộ danh sách thay vì chỉ gửi phần thay đổi. Kích thước dữ liệu sảnh nhỏ, trong khi cách gửi toàn bộ giúp Client chỉ cần thay thế danh sách cũ và không phải xử lý thứ tự các sự kiện thêm, bớt. Hình 4.2 mô tả hai cách nhận dữ liệu.",
    )
    figures.figure(
        doc,
        "4.2",
        diagram_lobby_sequence(),
        "Trình tự tải và đồng bộ sảnh",
        "Sau khi Client A đăng nhập, Server phát danh sách tới các phiên FREE; Client A cũng yêu cầu trực tiếp và nhận phản hồi cùng requestId.",
        "Đồng bộ danh sách người trực tuyến và danh sách phòng tới nhiều Client",
        "LobbyService.broadcastLobbyUpdates(), LobbyMessageHandler, ClientState.setOnlineUsers(), setRooms()",
    )
    add_body(
        doc,
        "Ở phía Client, ClientMessageDispatcher chuyển thông điệp sang luồng JavaFX và gọi ClientState.setOnlineUsers() hoặc setRooms(). ClientState thông báo cho các màn hình đang đăng ký lắng nghe; Home cập nhật số phòng còn chỗ và số người trực tuyến, còn Room Browser vẽ lại danh sách phòng và bảng người trực tuyến.",
    )

    add_heading(doc, "4.4 Tạo, tham gia và rời phòng", "CTDT-H2")
    add_heading(doc, "4.4.1 Tạo và tham gia phòng", "CTDT-H3")
    add_body(
        doc,
        "Khi nhận CREATE_ROOM, Server cắt khoảng trắng tên phòng, kiểm tra tên có từ 1 đến 60 ký tự và người gửi chưa ở phòng nào. Phòng mới nhận mã UUID, người tạo trở thành chủ phòng và chuyển sang trạng thái IN_ROOM. Với JOIN_ROOM, Server lần lượt kiểm tra phòng còn tồn tại, đang ở trạng thái WAITING và chưa đủ bốn người. Khi thao tác thành công, trạng thái phòng đầy đủ được gửi tới tất cả thành viên và danh sách phòng ở sảnh được cập nhật.",
    )
    add_body(
        doc,
        "Việc gửi trạng thái phòng được thực hiện bởi publishRoomState(): Server duyệt danh sách thành viên trong snapshot, tìm phiên TCP tương ứng qua ConnectionManager và gửi ROOM_STATE. Chỉ bản gửi cho người vừa thực hiện thao tác mang requestId, nhờ đó Client của người này biết yêu cầu đã hoàn thành, còn các thành viên khác nhận bản cập nhật chủ động. Hình 4.3 minh họa trình tự tạo phòng và tham gia phòng giữa hai Client.",
    )
    add_code(
        doc,
        """private void publishRoomState(RoomSnapshot room, String requestingSessionId, String requestId) {
    for (RoomPlayerSnapshot player : room.players()) {
        connectionManager.find(player.sessionId()).ifPresent(session -> sendRoomState(
                session,
                player.sessionId().equals(requestingSessionId) ? requestId : null,
                true,          // member = true
                room));
    }
}""",
    )
    figures.figure(
        doc,
        "4.3",
        diagram_room_sequence(),
        "Trình tự tạo phòng và tham gia phòng",
        "Client A gửi CREATE_ROOM, Server tạo phòng và trả ROOM_STATE; Client B gửi JOIN_ROOM, cả hai Client nhận ROOM_STATE mới; sảnh được cập nhật.",
        "Tạo phòng, tham gia phòng và gửi trạng thái phòng tới thành viên",
        "RoomMessageHandler.handleCreateRoom(), handleJoinRoom(), publishRoomState(), RoomManager",
    )

    add_heading(doc, "4.4.2 Chơi nhanh", "CTDT-H3")
    add_body(
        doc,
        "Nút PLAY ở màn hình Home cho phép vào trận nhanh mà không cần chọn phòng. Client lọc các phòng đang WAITING và còn chỗ từ danh sách phòng đã nhận, ưu tiên phòng đông người nhất để trận sớm đủ người. Client gửi JOIN_ROOM với chế độ không tự hiển thị lỗi. Nếu bị từ chối vì phòng vừa đầy, vừa bắt đầu hoặc không còn tồn tại, Client thử phòng kế tiếp, tối đa hai phòng. Khi không còn phòng phù hợp, Client tạo phòng mới mang tên người chơi. Cách làm này xử lý được tình huống danh sách phòng ở Client đã cũ so với trạng thái thật trên Server. Hình 4.4 mô tả luồng xử lý.",
    )
    figures.figure(
        doc,
        "4.4",
        diagram_quick_play(),
        "Luồng chơi nhanh",
        "Client lọc phòng còn chỗ, thử JOIN_ROOM tối đa hai phòng, thử lại khi lỗi do tranh chấp, nếu không thì tạo phòng mới.",
        "Chọn phòng tự động và xử lý tranh chấp chỗ trống",
        "HomeScreen.quickPlay(), tryJoin(), QuickPlay.candidates(), isRetryable()",
    )

    add_heading(doc, "4.4.3 Rời phòng và dọn phòng khi mất kết nối", "CTDT-H3")
    add_body(
        doc,
        "Khi nhận LEAVE_ROOM, Server xóa người chơi khỏi phòng và đưa trạng thái về FREE. Nếu người rời là chủ phòng, quyền chủ phòng được chuyển cho thành viên vào phòng sớm nhất còn lại. Nếu phòng không còn ai, phòng bị xóa. Người rời nhận ROOM_STATE với member bằng false để Client quay về Home, các thành viên còn lại nhận trạng thái phòng mới và sảnh được cập nhật. Trường hợp phòng đang thi đấu, Server đưa thêm một lệnh ngắt kết nối vào vòng lặp trận để người rời bị loại khỏi trận (mục 5.6).",
    )
    add_body(
        doc,
        "Khi một phiên mất kết nối, thủ tục dọn phiên ở mục 3.6 gọi RoomMessageHandler.handleSessionExit(). Phương thức này thực hiện các bước tương tự LEAVE_ROOM nhưng không gửi gì cho phiên đã đóng, chỉ gửi trạng thái phòng mới cho những người còn lại. Nhờ vậy phòng không giữ lại thành viên không còn kết nối.",
    )

    add_heading(doc, "4.5 Sẵn sàng, bắt đầu trận và chơi lại", "CTDT-H2")
    add_body(
        doc,
        "Trong phòng chờ, mỗi người chơi gửi READY để bật hoặc tắt trạng thái sẵn sàng; thao tác chỉ hợp lệ khi phòng đang WAITING. Khi chủ phòng gửi START_GAME, RoomManager kiểm tra lần lượt: người gửi đang ở phòng, phòng đang WAITING, người gửi là chủ phòng, phòng có ít nhất hai người và tất cả thành viên, kể cả chủ phòng, đều sẵn sàng. Chỉ khi thỏa mãn mọi điều kiện, phòng mới chuyển sang PLAYING.",
    )
    add_body(
        doc,
        "Sau đó RoomMessageHandler cập nhật trạng thái các thành viên thành PLAYING và gọi GameSessionManager.startGame() với snapshot của phòng để khởi tạo vòng lặp trận (Chương 5). Cuối cùng, ROOM_STATE với trạng thái PLAYING được gửi tới các thành viên. Khi nhận thông điệp này, Client khởi tạo dữ liệu trận mới và chuyển sang màn hình GAME. Hình 4.5 thể hiện trình tự từ sẵn sàng tới bắt đầu trận.",
    )
    figures.figure(
        doc,
        "4.5",
        diagram_start_sequence(),
        "Trình tự sẵn sàng và bắt đầu trận",
        "Client B gửi READY, các thành viên nhận ROOM_STATE; chủ phòng gửi START_GAME, Server kiểm tra điều kiện, tạo vòng lặp trận và gửi ROOM_STATE PLAYING.",
        "Đồng bộ trạng thái sẵn sàng và chuyển phòng sang thi đấu",
        "RoomMessageHandler.handleReady(), handleStartGame(), RoomManager.startGame(), GameSessionManager.startGame()",
    )
    add_body(
        doc,
        "Phía Client, LobbyStatus tính số người, số người sẵn sàng và vai trò chủ phòng từ ROOM_STATE theo đúng quy tắc của Server để hiển thị danh sách điều kiện và chỉ bật nút START GAME khi đủ điều kiện. Kiểm tra ở Client chỉ phục vụ trải nghiệm người dùng; Server vẫn kiểm tra lại toàn bộ điều kiện khi nhận yêu cầu.",
    )
    add_body(
        doc,
        "Sau khi trận kết thúc, phòng ở trạng thái FINISHED. Bất kỳ thành viên nào gửi PLAY_AGAIN sẽ đưa phòng về WAITING và đặt lại cờ sẵn sàng của mọi người. Nếu hai người cùng gửi, yêu cầu tới sau nhận mã ROOM_NOT_FINISHED; Client coi đây là trường hợp phòng đã được người khác chuẩn bị và không báo lỗi. Bảng 4.3 tổng hợp các mã kết quả của thao tác phòng.",
    )
    add_table(
        doc,
        "Bảng 4.3. Mã kết quả thao tác phòng",
        ["Mã kết quả", "Điều kiện phát sinh", "Thao tác liên quan"],
        [
            ["NOT_AUTHENTICATED", "Phiên chưa đăng nhập", "Mọi thao tác phòng"],
            ["INVALID_REQUEST", "Thiếu hoặc sai dữ liệu", "CREATE_ROOM, JOIN_ROOM, READY"],
            ["INVALID_ROOM_NAME", "Tên phòng rỗng hoặc dài hơn 60 ký tự", "CREATE_ROOM"],
            ["ALREADY_IN_ROOM", "Người chơi đã ở một phòng khác", "CREATE_ROOM, JOIN_ROOM"],
            ["ROOM_NOT_FOUND", "Mã phòng không tồn tại", "JOIN_ROOM"],
            ["ROOM_NOT_WAITING", "Phòng không ở trạng thái chờ", "JOIN_ROOM, READY, START_GAME"],
            ["ROOM_FULL", "Phòng đã có 4 người", "JOIN_ROOM"],
            ["NOT_IN_ROOM", "Người chơi không ở phòng nào", "LEAVE_ROOM, READY, START_GAME, PLAY_AGAIN"],
            ["NOT_HOST", "Người gửi không phải chủ phòng", "START_GAME"],
            ["NOT_ENOUGH_PLAYERS", "Phòng có ít hơn 2 người", "START_GAME"],
            ["NOT_ALL_PLAYERS_READY", "Còn người chưa sẵn sàng", "START_GAME"],
            ["ROOM_NOT_FINISHED", "Phòng chưa ở trạng thái kết thúc", "PLAY_AGAIN"],
        ],
        [2800, 3500, 3000],
    )

    add_heading(doc, "4.6 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Giao diện của phần này gồm màn hình Home, màn hình danh sách phòng và màn hình phòng chờ. Các màn hình không tự thay đổi dữ liệu phòng mà chỉ hiển thị trạng thái mới nhất nhận từ Server, vì vậy hai Client trong cùng phòng luôn thấy cùng danh sách thành viên và trạng thái sẵn sàng.",
    )
    figures.screenshot(
        doc,
        "4.6",
        "c4-home.png",
        "Màn hình Home",
        "Màn hình Home với nút PLAY và số người trực tuyến.",
        "Màn hình Home sau khi đăng nhập: nút PLAY (chơi nhanh), khu vực ONLINE ROOMS với số phòng còn chỗ và số người trực tuyến",
        "Chơi nhanh, hiển thị tóm tắt sảnh",
        "HomeScreen, QuickPlay, ClientState",
    )
    figures.screenshot(
        doc,
        "4.7",
        "c4-room-browser.png",
        "Màn hình danh sách phòng và người trực tuyến",
        "Danh sách phòng và bảng người trực tuyến.",
        "Màn hình ONLINE ROOMS: danh sách phòng (tên, số người, trạng thái), bảng ONLINE và nút CREATE",
        "Hiển thị ROOM_LIST_UPDATE, ONLINE_USERS_UPDATE; tạo và tham gia phòng",
        "RoomBrowserScreen, RoomCard, LobbyService",
    )
    figures.screenshot(
        doc,
        "4.8",
        "c4-room-lobby.png",
        "Phòng chờ được đồng bộ giữa hai Client",
        "Hai cửa sổ Client cùng hiển thị một phòng chờ.",
        "Hai cửa sổ Client đặt cạnh nhau trong cùng một phòng: danh sách thành viên, trạng thái READY, bảng ROOM STATUS và nút START GAME của chủ phòng",
        "Đồng bộ ROOM_STATE, sẵn sàng và điều kiện bắt đầu",
        "RoomLobbyScreen, SlotCard, LobbyStatus, RoomMessageHandler.publishRoomState()",
    )

    add_heading(doc, "4.7 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(doc, "Bảng 4.4 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng của phần sảnh và phòng chờ.")
    figures.mapping_table(doc, "Bảng 4.4. Ánh xạ hình vẽ với chức năng phần sảnh và phòng chờ")

    add_heading(doc, "4.8 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 4 đã trình bày cách Server quản lý phòng trong bộ nhớ với khóa thay đổi chung và quy tắc mỗi người một phòng, cách đồng bộ sảnh bằng cả yêu cầu trực tiếp và phát chủ động tới các phiên FREE, cách gửi trạng thái phòng có chọn lọc tới thành viên, cùng các điều kiện sẵn sàng và bắt đầu trận. Khi phòng chuyển sang PLAYING, quyền điều khiển được giao cho bộ xử lý trò chơi được trình bày ở chương tiếp theo.",
    )
    return figures
