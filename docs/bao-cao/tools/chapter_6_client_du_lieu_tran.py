"""Chương 6 - Kiến trúc Desktop Client và dữ liệu trận đấu (phần cá nhân D)."""
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
    orthogonal_arrow,
    rgb,
    round_box,
    save_image,
    text_centered,
)
from report_diagrams import canvas, flow_diagram, sequence_diagram


def diagram_client_layers() -> Path:
    image, draw, top = canvas(
        "KIẾN TRÚC PHÂN TẦNG CỦA DESKTOP CLIENT",
        "Màn hình chỉ gọi lệnh và đọc ClientState; dữ liệu từ Server đi qua dispatcher trước khi tới giao diện",
        1130,
    )
    screens = (60, top + 20, 1540, top + 130)
    controller = (60, top + 230, 520, top + 350)
    navigator = (570, top + 230, 1030, top + 350)
    state = (1080, top + 230, 1540, top + 350)
    pending = (570, top + 450, 960, top + 560)
    dispatcher = (1080, top + 450, 1540, top + 560)
    network = (60, top + 660, 1540, top + 780)
    server = (560, top + 880, 1040, top + 960)
    labels = [
        (screens, "MÀN HÌNH JAVAFX\nLogin · Home · RoomBrowser · RoomLobby · Game · Result · History · Leaderboard", 24),
        (controller, "GameClientController\nfacade gửi lệnh, sinh requestId", 22),
        (pending, "PendingRequests\nchờ phản hồi theo requestId, 5 giây", 22),
        (state, "ClientState\ntrạng thái hiển thị, listener,\nsnapshot trận (AtomicReference)", 22),
        (navigator, "ScreenNavigator / AppShell\nchuyển màn hình", 22),
        (dispatcher, "ClientMessageDispatcher\nphân loại, chuyển sang luồng JavaFX", 22),
        (network, "GameNetworkClient - socket TCP\nluồng ghi client-network-writer · luồng đọc server-listener  (Chương 3)", 23),
        (server, "SERVER\nTCP 8081", 24),
    ]
    for box, label, size in labels:
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=16, width=4 if box in (screens, network, server) else 3)
        text_centered(draw, box, label, font(size, True), rgb(BLACK), spacing=4)
    arrow(draw, (290, screens[3]), (290, controller[1] - 4), BLACK, width=4, head=16)
    text_centered(draw, (300, top + 150, 520, top + 210), "gọi lệnh", font(21), rgb(BLACK))
    arrow(draw, (800, navigator[1]), (800, screens[3] + 4), BLACK, width=4, head=16)
    text_centered(draw, (810, top + 150, 1060, top + 210), "hiển thị màn hình", font(21), rgb(BLACK))
    arrow(draw, (1310, state[1]), (1310, screens[3] + 4), BLACK, width=4, head=16)
    text_centered(draw, (1320, top + 150, 1540, top + 210), "thông báo listener", font(21), rgb(BLACK))
    arrow(draw, (290, controller[3]), (290, network[1] - 4), BLACK, width=4, head=16)
    text_centered(draw, (70, top + 480, 280, top + 540), "gửi khung", font(21), rgb(BLACK))
    orthogonal_arrow(draw, [(430, controller[3]), (430, top + 505), (pending[0] - 4, top + 505)], BLACK, width=3, head=14)
    text_centered(draw, (430, top + 512, 570, top + 552), "đăng ký", font(20), rgb(BLACK))
    arrow(draw, (1310, network[1]), (1310, dispatcher[3] + 4), BLACK, width=4, head=16)
    text_centered(draw, (1320, top + 590, 1540, top + 640), "thông điệp nhận", font(21), rgb(BLACK))
    arrow(draw, (1310, dispatcher[1]), (1310, state[3] + 4), BLACK, width=4, head=16)
    text_centered(draw, (1320, top + 375, 1540, top + 430), "cập nhật", font(21), rgb(BLACK))
    orthogonal_arrow(draw, [(1130, dispatcher[1]), (1130, top + 400), (900, top + 400), (900, navigator[3] + 4)], BLACK, width=3, head=14)
    text_centered(draw, (910, top + 355, 1120, top + 395), "show()", font(20), rgb(BLACK))
    arrow(draw, (dispatcher[0], top + 505), (pending[2] + 4, top + 505), BLACK, width=3, head=14)
    text_centered(draw, (956, top + 512, 1084, top + 552), "complete()", font(19), rgb(BLACK))
    arrow(draw, (760, network[3]), (760, server[1] - 4), BLACK, width=4, head=16)
    arrow(draw, (840, server[1]), (840, network[3] + 4), BLACK, width=4, head=16)
    return save_image(image, "hinh-6-1-kien-truc-client.png")


def diagram_dispatch_flow() -> Path:
    return flow_diagram(
        "hinh-6-2-phan-phoi-thong-diep.png",
        "PHÂN PHỐI THÔNG ĐIỆP TỚI GIAO DIỆN",
        "Chỉ GAME_STATE được xử lý ngay trên luồng mạng; các thông điệp khác được chuyển sang luồng JavaFX",
        [
            ("start", "Luồng server-listener nhận một NetworkMessage"),
            ("decision", "type = GAME_STATE?", "Giải mã trên luồng mạng, ghi AtomicReference; nếu cần thì chuyển sang màn hình GAME", "Không", "Có"),
            ("step", "Platform.runLater(): chuyển sang luồng JavaFX"),
            ("step", "switch theo type: cập nhật ClientState và điều hướng (LOGIN_RESPONSE → HOME, ROOM_STATE → ROOM_LOBBY / GAME / HOME)"),
            ("decision", "type = ERROR và yêu cầu không ở chế độ im lặng?", "Hiển thị thông báo lỗi, trừ lỗi thao tác trong trận", "Không", "Có"),
            ("end", "PendingRequests.complete(): hoàn thành future theo requestId"),
        ],
    )


def diagram_navigation() -> Path:
    image, draw, top = canvas(
        "ĐIỀU HƯỚNG GIỮA CÁC MÀN HÌNH",
        "Chuyển màn hình do thao tác người dùng hoặc do thông điệp Server; tên màn hình theo ScreenId",
        820,
    )
    y1, y2 = top + 110, top + 400
    boxes = {
        "LOGIN": (60, y1, 300, y1 + 100),
        "HOME": (420, y1, 680, y1 + 100),
        "ROOM_BROWSER": (800, y1, 1080, y1 + 100),
        "ROOM_LOBBY": (1220, y1, 1500, y1 + 100),
        "LEADERBOARD": (300, y2, 560, y2 + 100),
        "HISTORY": (620, y2, 880, y2 + 100),
        "RESULT": (950, y2, 1170, y2 + 100),
        "GAME": (1220, y2, 1500, y2 + 100),
    }
    for name, box in boxes.items():
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=16, width=4)
        text_centered(draw, box, name, font(24, True), rgb(BLACK))
    arrow(draw, (300, y1 + 35), (416, y1 + 35), BLACK, width=4, head=14)
    text_centered(draw, (300, y1 - 50, 420, y1 + 30), "LOGIN_\nRESPONSE", font(19), rgb(BLACK), spacing=0)
    arrow(draw, (420, y1 + 70), (304, y1 + 70), BLACK, width=3, head=14)
    text_centered(draw, (270, y1 + 74, 450, y1 + 150), "đăng xuất,\nmất kết nối", font(19), rgb(BLACK), spacing=0)
    arrow(draw, (680, y1 + 50), (796, y1 + 50), BLACK, width=4, head=14)
    text_centered(draw, (680, y1 + 55, 800, y1 + 95), "ROOMS", font(19), rgb(BLACK))
    arrow(draw, (1080, y1 + 50), (1216, y1 + 50), BLACK, width=4, head=14)
    text_centered(draw, (1080, y1 - 45, 1220, y1 + 45), "ROOM_STATE\n(member)", font(19), rgb(BLACK), spacing=0)
    orthogonal_arrow(draw, [(550, y1), (550, y1 - 60), (1360, y1 - 60), (1360, y1 - 4)], BLACK, width=3, head=14)
    text_centered(draw, (700, y1 - 105, 1200, y1 - 65), "PLAY (chơi nhanh) → ROOM_STATE", font(20), rgb(BLACK))
    arrow(draw, (1360, y1 + 100), (1360, y2 - 4), BLACK, width=4, head=14)
    text_centered(draw, (1370, y1 + 150, 1560, y1 + 230), "ROOM_STATE\nPLAYING", font(19), rgb(BLACK), spacing=0)
    arrow(draw, (1220, y2 + 50), (1174, y2 + 50), BLACK, width=4, head=14)
    text_centered(draw, (1060, y2 + 110, 1360, y2 + 170), "GAME_OVER + 1,2 giây", font(19), rgb(BLACK))
    orthogonal_arrow(draw, [(1060, y2), (1060, y1 + 180), (1290, y1 + 180), (1290, y1 + 104)], BLACK, width=3, head=14)
    text_centered(draw, (1070, y1 + 190, 1290, y1 + 230), "PLAY AGAIN", font(19), rgb(BLACK))
    arrow(draw, (500, y1 + 100), (430, y2 - 4), BLACK, width=3, head=14)
    arrow(draw, (600, y1 + 100), (750, y2 - 4), BLACK, width=3, head=14)
    text_centered(
        draw,
        (60, y2 + 160, 1540, y2 + 240),
        "Các màn hình LEADERBOARD, HISTORY, RESULT và ROOM_BROWSER có nút quay về HOME;\nrời phòng (ROOM_STATE member = false) đưa về HOME; mất kết nối luôn đưa về LOGIN.",
        font(21),
        rgb(BLACK),
        spacing=6,
    )
    return save_image(image, "hinh-6-3-dieu-huong-man-hinh.png")


def diagram_persist_sequence() -> Path:
    return sequence_diagram(
        "hinh-6-4-luu-ket-qua-tran.png",
        "LƯU KẾT QUẢ TRẬN ĐẤU",
        "Kết quả và điểm của mọi người chơi được ghi trong một giao dịch trước khi Server gửi GAME_OVER",
        ["RoomGameLoop", "GameSession\nManager", "MatchPersistence\nService", "MatchScoring", "Repository\n(JPA, MySQL)", "Client trong\nphòng"],
        [
            ("msg", 0, 1, "onGameOver(roomId, game, outcome)"),
            ("msg", 1, 2, "recordCompletedMatch(roomId, startedAt, endedAt, participants, outcome)"),
            ("note", "@Transactional: các bước dưới đây cùng thành công hoặc cùng bị hủy", 2, 4),
            ("msg", 2, 4, "findAllById(userIds)"),
            ("msg", 2, 3, "playerResult(), scoreUnits() cho từng người"),
            ("self", 2, "tạo Match, MatchPlayer; user.recordMatch(result, score)"),
            ("msg", 2, 4, "saveAndFlush(match): ghi matches, match_players, cập nhật users"),
            ("reply", 2, 1, "Match đã lưu (nếu lỗi: ghi log và tiếp tục)"),
            ("reply", 1, 5, "GAME_OVER {winnerUserId, matchResult, players[]}"),
            ("self", 5, "ClientState.setGameOver(); sau 1,2 giây hiển thị RESULT"),
        ],
    )


def diagram_query_sequence() -> Path:
    return sequence_diagram(
        "hinh-6-5-lich-su-xep-hang.png",
        "TẢI BẢNG XẾP HẠNG VÀ LỊCH SỬ TRẬN",
        "Mỗi lần mở màn hình, Client gửi yêu cầu mới; Server truy vấn cơ sở dữ liệu và trả kết quả cùng requestId",
        ["Leaderboard /\nHistory Screen", "Client mạng\n(Controller,\nDispatcher)", "Ranking / History\nMessageHandler", "RankingService", "MatchHistory\nService", "Repository\n(JPA, MySQL)"],
        [
            ("msg", 0, 1, "requestRanking() khi mở màn hình"),
            ("msg", 1, 2, "RANKING_REQUEST"),
            ("msg", 2, 3, "getRanking()"),
            ("msg", 3, 5, "sắp xếp theo điểm, số trận thắng, tên"),
            ("self", 3, "hạng = vị trí + 1"),
            ("reply", 2, 1, "RANKING_RESPONSE {entries[]} cùng requestId"),
            ("reply", 1, 0, "setRankingEntries(): vẽ bục top 3 và bảng"),
            ("msg", 0, 1, "requestHistory() khi mở màn hình"),
            ("msg", 1, 2, "HISTORY_REQUEST"),
            ("msg", 2, 4, "getHistory(userId của phiên)"),
            ("msg", 4, 5, "findMatchesByUserId(): trận mới nhất trước"),
            ("reply", 2, 1, "HISTORY_RESPONSE {matches[]} cùng requestId"),
            ("reply", 1, 0, "setMatchHistory(): hiển thị theo giờ địa phương"),
        ],
    )


def write(doc) -> FigureLog:
    figures = FigureLog(6)

    add_heading(doc, "CHƯƠNG 6. KIẾN TRÚC DESKTOP CLIENT VÀ DỮ LIỆU TRẬN ĐẤU", "CTDT-H1")
    add_heading(doc, "6.1 Phạm vi chức năng", "CTDT-H2")
    add_body(
        doc,
        "Phần này gồm hai nhóm nội dung. Nhóm thứ nhất là kiến trúc ứng dụng Desktop Client viết bằng JavaFX: cách ứng dụng khởi tạo, cách các màn hình gửi lệnh, cách thông điệp từ Server được phân phối tới giao diện theo đúng mô hình luồng của JavaFX và cách điều hướng giữa các màn hình. Các màn hình của từng tính năng đã được trình bày trong chương tương ứng; chương này tập trung vào khung ứng dụng mà các màn hình đó dùng chung.",
    )
    add_body(
        doc,
        "Nhóm thứ hai là dữ liệu trận đấu: lưu kết quả trận và tính điểm trên Server, truy vấn lịch sử và bảng xếp hạng qua mạng, hiển thị kết quả, lịch sử, bảng xếp hạng ở Client. Bảng 6.1 liệt kê các chức năng đã triển khai.",
    )
    add_table(
        doc,
        "Bảng 6.1. Các chức năng thuộc phần Desktop Client và dữ liệu trận đấu",
        ["ID", "Chức năng", "Thành phần cài đặt", "Giao tiếp mạng"],
        [
            ["CD-01", "Khởi tạo ứng dụng Client", "Launcher, BombermanApp.start()", "Nối dispatcher vào kết nối TCP"],
            ["CD-02", "Facade gửi lệnh", "GameClientController", "Tạo NetworkMessage, requestId"],
            ["CD-03", "Phân phối thông điệp nhận", "ClientMessageDispatcher", "Xử lý mọi thông điệp Server gửi"],
            ["CD-04", "Trạng thái hiển thị", "ClientState, ClientStateListener", "Lưu dữ liệu nhận được"],
            ["CD-05", "Điều hướng màn hình", "ScreenNavigator, AppShell, ScreenId", "Chuyển màn hình theo thông điệp"],
            ["CD-06", "Lưu kết quả trận", "MatchPersistenceService.recordCompletedMatch()", "Thực hiện trước khi gửi GAME_OVER"],
            ["CD-07", "Tính điểm", "MatchScoring, User.recordMatch()", "Điểm trong GAME_OVER và xếp hạng"],
            ["CD-08", "Màn hình kết quả", "ResultScreen, MatchTracker", "Hiển thị GAME_OVER, gửi PLAY_AGAIN"],
            ["CD-09", "Lịch sử trận", "HistoryMessageHandler, MatchHistoryService, HistoryScreen", "HISTORY_REQUEST / RESPONSE"],
            ["CD-10", "Bảng xếp hạng", "RankingMessageHandler, RankingService, LeaderboardScreen", "RANKING_REQUEST / RESPONSE"],
            ["CD-11", "Cài đặt và tài nguyên", "UserPreferences, SettingsPopup, SvgAssets, theme", "Lưu địa chỉ Server đã chọn"],
            ["CD-12", "Đóng gói ứng dụng", "Tác vụ Gradle packageApp (jpackage)", "Bản chạy độc lập trên Windows"],
        ],
        [900, 2300, 3700, 2400],
    )

    add_heading(doc, "6.2 Kiến trúc Desktop Client", "CTDT-H2")
    add_body(
        doc,
        "Điểm vào của ứng dụng là Launcher.main(), gọi Application.launch() với lớp BombermanApp. Trong phương thức start(), BombermanApp tạo và nối các thành phần theo thứ tự: nạp phông chữ và tùy chọn người dùng, tạo ClientState, khung cửa sổ AppShell với vùng thiết kế 1280 × 720 được co giãn theo kích thước cửa sổ, ScreenNavigator, PendingRequests, GameNetworkClient cùng ClientMessageDispatcher, GameClientController, rồi đăng ký tám màn hình theo ScreenId. Các thành phần nhận nhau qua hàm khởi tạo, nên phụ thuộc giữa chúng thể hiện rõ trong mã nguồn.",
    )
    add_body(
        doc,
        "Kiến trúc Client được chia thành các tầng. Tầng vận chuyển (Chương 3) chỉ biết gửi, nhận khung byte. Tầng ứng dụng gồm GameClientController cho chiều gửi và ClientMessageDispatcher cho chiều nhận. ClientState là nơi duy nhất lưu dữ liệu hiển thị và thông báo cho các màn hình đang lắng nghe khi dữ liệu thay đổi. Các màn hình không đọc socket và không tự sửa dữ liệu nhận từ Server: chúng gọi phương thức của controller khi người dùng thao tác và vẽ lại khi ClientState thay đổi. Hình 6.1 và Bảng 6.2 mô tả các tầng này.",
    )
    figures.figure(
        doc,
        "6.1",
        diagram_client_layers(),
        "Kiến trúc phân tầng của Desktop Client",
        "Màn hình gọi GameClientController, controller gửi qua GameNetworkClient; thông điệp nhận đi qua ClientMessageDispatcher tới ClientState, ScreenNavigator và PendingRequests.",
        "Tổ chức Client: gửi lệnh, nhận thông điệp, cập nhật trạng thái và giao diện",
        "BombermanApp, GameClientController, ClientMessageDispatcher, ClientState, ScreenNavigator",
    )
    add_table(
        doc,
        "Bảng 6.2. Các tầng của Desktop Client",
        ["Tầng", "Thành phần", "Trách nhiệm"],
        [
            ["Giao diện", "LoginScreen, HomeScreen, RoomBrowserScreen, RoomLobbyScreen, GameScreen, ResultScreen, HistoryScreen, LeaderboardScreen", "Nhận thao tác, hiển thị ClientState"],
            ["Điều hướng", "ScreenNavigator, AppShell", "Chuyển màn hình, gọi onShow/onHide, hiển thị popup và thông báo"],
            ["Trạng thái", "ClientState, ClientStateListener, Feedback", "Lưu dữ liệu hiển thị, thông báo thay đổi"],
            ["Ứng dụng - chiều gửi", "GameClientController, PendingRequests", "Tạo thông điệp, gửi theo thứ tự, chờ phản hồi"],
            ["Ứng dụng - chiều nhận", "ClientMessageDispatcher", "Giải mã payload, cập nhật trạng thái, điều hướng"],
            ["Vận chuyển", "GameNetworkClient, MessageEncoder/Decoder", "Socket TCP, luồng đọc, khóa ghi (Chương 3)"],
        ],
        [2100, 4200, 3000],
    )

    add_heading(doc, "6.3 Xử lý thông điệp và cập nhật giao diện", "CTDT-H2")
    add_body(
        doc,
        "JavaFX yêu cầu mọi thay đổi trên cây giao diện phải thực hiện trên JavaFX Application Thread, trong khi dữ liệu mạng được đọc trên virtual thread server-listener. ClientMessageDispatcher giải quyết ràng buộc này bằng cách chuyển hầu hết thông điệp sang luồng giao diện qua Platform.runLater(). Tại đó, câu lệnh switch theo loại thông điệp gọi phương thức tương ứng của ClientState, sau đó gọi PendingRequests.complete() để hoàn thành yêu cầu đang chờ cùng requestId.",
    )
    add_body(
        doc,
        "GAME_STATE là trường hợp đặc biệt vì tới 10 lần mỗi giây. Thông điệp này được giải mã ngay trên luồng mạng và ghi vào một AtomicReference trong ClientState; vòng vẽ của GameScreen đọc bản mới nhất ở mỗi khung hình. Cách làm này không tạo tác vụ trên luồng giao diện cho mỗi snapshot, và vòng vẽ luôn đọc một đối tượng đã giải mã xong. Đoạn mã sau thể hiện điểm phân nhánh.",
    )
    add_code(
        doc,
        """@Override
public void onMessage(NetworkMessage message) {           // gọi trên luồng server-listener
    if (message.type() == MessageType.GAME_STATE) {
        handleGameStateFromNetworkThread(message);          // giải mã, ghi AtomicReference
        return;
    }
    uiThread.execute(() -> dispatchOnUiThread(message));    // Platform.runLater
}""",
    )
    figures.figure(
        doc,
        "6.2",
        diagram_dispatch_flow(),
        "Luồng phân phối thông điệp tới giao diện",
        "GAME_STATE được ghi trực tiếp vào AtomicReference; các thông điệp khác chuyển sang luồng JavaFX, cập nhật ClientState, xử lý lỗi và hoàn thành yêu cầu đang chờ.",
        "Chuyển dữ liệu mạng sang giao diện đúng mô hình luồng của JavaFX",
        "ClientMessageDispatcher.onMessage(), dispatchOnUiThread(), handleGameStateFromNetworkThread()",
    )
    add_body(
        doc,
        "Bảng 6.3 tổng hợp hành động của Client với từng loại thông điệp nhận được. Lỗi được hiển thị dưới dạng thông báo ngắn, trừ hai trường hợp: yêu cầu được đánh dấu im lặng vì màn hình tự xử lý lỗi, như chơi nhanh và chơi lại, và lỗi thao tác khi đang ở màn hình trận đấu.",
    )
    add_table(
        doc,
        "Bảng 6.3. Hành động của Client với các thông điệp nhận được",
        ["Thông điệp", "Cập nhật ClientState", "Điều hướng / hiển thị"],
        [
            ["REGISTER_RESPONSE", "Không", "Thông báo tạo tài khoản thành công hoặc lỗi"],
            ["LOGIN_RESPONSE", "login(userId, username)", "Thành công: HOME; thất bại: thông báo lỗi"],
            ["ONLINE_USERS_UPDATE", "setOnlineUsers()", "Home, Room Browser vẽ lại"],
            ["ROOM_LIST_UPDATE", "setRooms()", "Home, Room Browser vẽ lại"],
            ["ROOM_STATE", "setRoom(); beginGame() khi PLAYING", "member = false: HOME; WAITING: ROOM_LOBBY; PLAYING: GAME"],
            ["GAME_STATE", "setLatestGameState() trên luồng mạng", "Vòng vẽ GameScreen đọc ở khung hình kế tiếp"],
            ["GAME_OVER", "setGameOver()", "GameScreen hiển thị người thắng, sau 1,2 giây: RESULT"],
            ["RANKING_RESPONSE", "setRankingEntries()", "Leaderboard vẽ lại"],
            ["HISTORY_RESPONSE", "setMatchHistory()", "History vẽ lại"],
            ["ERROR", "setFeedback()", "Thông báo lỗi (trừ yêu cầu im lặng và lỗi thao tác trong trận)"],
        ],
        [2500, 3200, 3600],
    )

    add_heading(doc, "6.4 Điều hướng màn hình", "CTDT-H2")
    add_body(
        doc,
        "ScreenNavigator giữ một bảng ánh xạ từ ScreenId tới đối tượng màn hình và chỉ chuyển khi màn hình đích khác màn hình hiện tại. Một số lần chuyển do người dùng chủ động, như mở danh sách phòng, bảng xếp hạng hoặc lịch sử. Các lần chuyển còn lại do thông điệp Server quyết định: đăng nhập thành công chuyển sang HOME, ROOM_STATE đưa người chơi vào phòng chờ hoặc vào trận, GAME_OVER dẫn tới màn hình kết quả, còn mất kết nối luôn đưa về LOGIN. Dispatcher không kéo người chơi đang xem kết quả về phòng chờ khi một người khác chọn chơi lại, để mỗi người tự quyết định thời điểm rời màn hình kết quả. Hình 6.3 thể hiện sơ đồ điều hướng.",
    )
    figures.figure(
        doc,
        "6.3",
        diagram_navigation(),
        "Sơ đồ điều hướng giữa các màn hình",
        "Tám màn hình LOGIN, HOME, ROOM_BROWSER, ROOM_LOBBY, GAME, RESULT, LEADERBOARD, HISTORY và các thông điệp hoặc thao tác gây chuyển màn hình.",
        "Điều hướng màn hình theo thao tác người dùng và thông điệp Server",
        "ScreenNavigator.show(), ClientMessageDispatcher.handleRoomState(), ScreenId",
    )

    add_heading(doc, "6.5 Lưu kết quả trận đấu và tính điểm", "CTDT-H2")
    add_body(
        doc,
        "Khi Game Engine xác định được kết quả (mục 5.6), GameSessionManager.handleGameOver() gọi MatchPersistenceService.recordCompletedMatch() trước khi gửi GAME_OVER. Phương thức này được đánh dấu @Transactional. Nó nạp tài khoản của mọi người tham gia, tạo bản ghi Match gồm mã phòng, thời điểm bắt đầu, kết thúc, người thắng và kết quả chung, sau đó với từng người tạo một MatchPlayer và cập nhật bộ đếm trong User. Toàn bộ dữ liệu được ghi bằng một lần saveAndFlush(); nếu một bước thất bại, giao dịch bị hủy và không có trận nào được lưu dở dang.",
    )
    add_body(
        doc,
        "Điểm được lưu theo đơn vị nửa điểm để biểu diễn bằng số nguyên: thắng nhận 2 đơn vị, tương ứng 1 điểm; hòa nhận 1 đơn vị, tương ứng 0,5 điểm; thua nhận 0. Bảng users lưu tổng điểm và số trận thắng, thua, hòa ngay trong tài khoản, nên truy vấn bảng xếp hạng chỉ cần sắp xếp một bảng mà không phải tổng hợp lại toàn bộ lịch sử. Cơ sở dữ liệu là MySQL, truy cập qua Spring Data JPA; lược đồ ba bảng users, matches và match_players đã được mô tả ở Hình 2.5. Bảng 6.4 tóm tắt quy tắc tính điểm, Hình 6.4 thể hiện trình tự lưu trận.",
    )
    add_table(
        doc,
        "Bảng 6.4. Quy tắc xác định kết quả và điểm của từng người",
        ["Kết quả trận", "Người chơi", "Kết quả cá nhân", "Điểm (đơn vị)", "Bộ đếm được tăng"],
        [
            ["WIN", "Người thắng", "WIN", "2 (1 điểm)", "totalWins"],
            ["WIN", "Người còn lại", "LOSS", "0", "totalLosses"],
            ["DRAW", "Mọi người", "DRAW", "1 (0,5 điểm)", "totalDraws"],
        ],
        [1700, 1900, 1900, 1800, 2000],
    )
    add_code(
        doc,
        """for (GameParticipant participant : participants) {             // trích từ recordCompletedMatch()
    GameResult playerResult = MatchScoring.playerResult(
            participant.userId(), outcome.result(), outcome.winnerUserId());
    int scoreUnits = MatchScoring.scoreUnits(playerResult);       // 2 / 1 / 0
    match.addPlayer(new MatchPlayer(participant.userId(), participant.username(),
            playerResult, scoreUnits));
    usersById.get(participant.userId()).recordMatch(playerResult, scoreUnits);
}
return matchRepository.saveAndFlush(match);""",
    )
    figures.figure(
        doc,
        "6.4",
        diagram_persist_sequence(),
        "Trình tự lưu kết quả trận đấu",
        "RoomGameLoop báo kết thúc, GameSessionManager gọi recordCompletedMatch trong một giao dịch, sau đó gửi GAME_OVER cho các Client.",
        "Lưu trận, cập nhật thống kê xếp hạng và thông báo kết quả",
        "GameSessionManager.handleGameOver(), MatchPersistenceService, MatchScoring, User.recordMatch()",
    )
    add_body(
        doc,
        "Nếu việc lưu trữ gặp lỗi, ví dụ cơ sở dữ liệu tạm thời không truy cập được, lỗi được ghi log và Server vẫn gửi GAME_OVER. Người chơi vẫn thấy kết quả và có thể tiếp tục chơi, chỉ trận đó không xuất hiện trong lịch sử.",
    )

    add_heading(doc, "6.6 Kết quả, lịch sử và bảng xếp hạng", "CTDT-H2")
    add_body(
        doc,
        "Thông điệp GAME_OVER chứa người thắng, kết quả chung và danh sách kết quả, điểm của từng người. ResultScreen kết hợp dữ liệu này với MatchTracker, thành phần ghi lại thứ tự và màu của người chơi trong suốt trận, để hiển thị biến thể VICTORY, DEFEAT hoặc DRAW cùng bảng xếp hạng của trận. Từ màn hình kết quả, người chơi chọn HOME để rời phòng hoặc PLAY AGAIN để gửi PLAY_AGAIN (mục 4.5).",
    )
    add_body(
        doc,
        "Bảng xếp hạng và lịch sử được tải lại mỗi khi mở màn hình. Với RANKING_REQUEST, RankingService lấy toàn bộ tài khoản sắp xếp giảm dần theo tổng điểm, sau đó theo số trận thắng, cuối cùng theo tên, rồi gán hạng theo vị trí. Với HISTORY_REQUEST, Server lấy mã người dùng từ phiên TCP, không nhận từ payload, nên mỗi người chỉ xem được lịch sử của chính mình. MatchHistoryService truy vấn các trận có người đó tham gia, trận mới nhất đứng trước và nạp kèm danh sách người chơi, sau đó bổ sung kết quả, điểm của người xem vào từng mục. Cả hai yêu cầu đều trả lỗi NOT_AUTHENTICATED nếu phiên chưa đăng nhập.",
    )
    add_body(
        doc,
        "Ở Client, LeaderboardScreen hiển thị bục vinh danh ba người đứng đầu và bảng chi tiết với hạng do Server gán; HistoryScreen hiển thị các trận theo múi giờ của máy người dùng từ các mốc thời gian dạng epoch millisecond. Hình 6.5 thể hiện trình tự tải hai loại dữ liệu này.",
    )
    figures.figure(
        doc,
        "6.5",
        diagram_query_sequence(),
        "Trình tự tải bảng xếp hạng và lịch sử trận",
        "Màn hình gửi RANKING_REQUEST hoặc HISTORY_REQUEST, Server truy vấn qua RankingService hoặc MatchHistoryService và trả phản hồi cùng requestId.",
        "Truy vấn dữ liệu đã lưu qua mạng và hiển thị",
        "RankingMessageHandler, RankingService.getRanking(), HistoryMessageHandler, MatchHistoryService.getHistory()",
    )

    add_heading(doc, "6.7 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Các hình dưới đây minh họa những màn hình trình bày dữ liệu trận đấu. Toàn bộ số liệu trên các màn hình đều lấy từ thông điệp Server gửi về, Client không tự tính điểm hoặc thứ hạng.",
    )
    figures.screenshot(
        doc,
        "6.6",
        "c6-result.png",
        "Màn hình kết quả trận",
        "Màn hình kết quả với tiêu đề VICTORY, DEFEAT hoặc DRAW.",
        "Màn hình RESULT sau một trận: tiêu đề VICTORY / DEFEAT / DRAW, bảng MATCH RANKING, nút HOME và PLAY AGAIN",
        "Hiển thị GAME_OVER, gửi PLAY_AGAIN",
        "ResultScreen, MatchTracker, GameOverDto",
    )
    figures.screenshot(
        doc,
        "6.7",
        "c6-history.png",
        "Màn hình lịch sử trận đấu",
        "Danh sách các trận đã chơi, mới nhất ở trên.",
        "Màn hình HISTORY với ít nhất hai trận đã chơi: thời gian, người tham gia, kết quả và điểm",
        "Hiển thị HISTORY_RESPONSE",
        "HistoryScreen, MatchHistoryService",
    )
    figures.screenshot(
        doc,
        "6.8",
        "c6-leaderboard.png",
        "Màn hình bảng xếp hạng",
        "Bục vinh danh top 3 và bảng xếp hạng chi tiết.",
        "Màn hình LEADERBOARD với bục top 3 và bảng hạng, điểm, số trận thắng, thua, hòa",
        "Hiển thị RANKING_RESPONSE",
        "LeaderboardScreen, RankRow, RankingService",
    )

    add_heading(doc, "6.8 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(doc, "Bảng 6.5 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng của phần Desktop Client và dữ liệu trận đấu.")
    figures.mapping_table(doc, "Bảng 6.5. Ánh xạ hình vẽ với chức năng phần Desktop Client và dữ liệu trận đấu")

    add_heading(doc, "6.9 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 6 đã trình bày kiến trúc phân tầng của Desktop Client: màn hình chỉ gửi lệnh qua một facade và hiển thị ClientState, dispatcher chuyển dữ liệu mạng sang luồng JavaFX, còn trạng thái trận được truyền qua AtomicReference để phục vụ vòng vẽ. Ở phía dữ liệu, kết quả trận được lưu cùng bộ đếm xếp hạng trong một giao dịch trước khi thông báo cho người chơi, lịch sử và bảng xếp hạng được truy vấn qua cùng kết nối TCP và hiển thị trên các màn hình tương ứng.",
    )
    return figures
