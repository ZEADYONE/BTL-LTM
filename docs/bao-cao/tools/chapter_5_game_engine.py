"""Chương 5 - Game Engine và đồng bộ thời gian thực (phần cá nhân C)."""
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


def diagram_components() -> Path:
    image, draw, top = canvas(
        "THÀNH PHẦN CỦA GAME ENGINE",
        "Luồng mạng chỉ kiểm tra và xếp lệnh; mọi thay đổi trạng thái trận diễn ra trên luồng vòng lặp riêng của phòng",
        1000,
    )
    handler = (60, top + 40, 420, top + 200)
    manager = (520, top + 40, 1000, top + 200)
    loop = (1100, top + 40, 1540, top + 200)
    for box in (handler, manager, loop):
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=16, width=4)
    text_centered(draw, handler, "GameMessageHandler\n\nnhận MOVE, PLACE_BOMB\nkiểm tra phiên, phòng, còn sống", font(22, True), rgb(BLACK))
    text_centered(draw, manager, "GameSessionManager\n\nphòng → vòng lặp; người chơi → phòng\nphòng → danh sách phiên", font(22, True), rgb(BLACK))
    text_centered(draw, loop, "RoomGameLoop (mỗi phòng)\n\nhàng đợi tối đa 512 lệnh\n20 tick/giây trên một luồng", font(22, True), rgb(BLACK))
    arrow(draw, (handler[2] + 4, top + 120), (manager[0] - 4, top + 120), BLACK, width=4, head=16)
    arrow(draw, (manager[2] + 4, top + 120), (loop[0] - 4, top + 120), BLACK, width=4, head=16)
    text_centered(draw, (handler[2], top + 70, manager[0], top + 110), "enqueue", font(20), rgb(BLACK))
    text_centered(draw, (manager[2], top + 70, loop[0], top + 110), "offer", font(20), rgb(BLACK))

    game = (360, top + 290, 1540, top + 560)
    round_box(draw, game, rgb(WHITE), rgb(BLACK), radius=18, width=4)
    text_centered(draw, (380, top + 300, 1520, top + 350), "BombermanGame - luật chơi, bảo vệ bằng stateLock", font(24, True), rgb(BLACK))
    parts = [
        ("GameMap\nbản đồ 13 × 11", 400),
        ("BomberPlayer\nvị trí, còn sống,\nsố bom, tầm nổ", 690),
        ("Bomb\nchủ bom, vị trí,\nthời điểm nổ", 980),
        ("Explosion\nvùng ảnh hưởng,\nthời điểm hết", 1260),
    ]
    for label, x in parts:
        box = (x, top + 380, x + 250, top + 530)
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=12, width=3)
        text_centered(draw, box, label, font(21), rgb(BLACK), spacing=2)
    arrow(draw, (1320, loop[3] + 4), (1320, game[1] - 4), BLACK, width=4, head=16)
    text_centered(draw, (1330, top + 215, 1540, top + 275), "execute(), tick()", font(20), rgb(BLACK))

    snapshot = (60, top + 650, 520, top + 790)
    broadcast = (620, top + 650, 1060, top + 790)
    over = (1140, top + 650, 1540, top + 790)
    for box in (snapshot, broadcast, over):
        round_box(draw, box, rgb(WHITE), rgb(BLACK), radius=16, width=3)
    text_centered(draw, snapshot, "BombermanSnapshot\n→ GameStateMapper\n→ GameStateDto", font(22, True), rgb(BLACK))
    text_centered(draw, broadcast, "GAME_STATE\ntới các phiên của phòng\n(10 lần/giây)", font(22, True), rgb(BLACK))
    text_centered(draw, over, "GameOutcome\n→ handleGameOver()\nlưu trận, GAME_OVER", font(22, True), rgb(BLACK))
    orthogonal_arrow(draw, [(500, game[3]), (500, top + 605), (290, top + 605), (290, snapshot[1] - 4)], BLACK, width=4, head=16)
    arrow(draw, (snapshot[2] + 4, top + 720), (broadcast[0] - 4, top + 720), BLACK, width=4, head=16)
    orthogonal_arrow(draw, [(1400, game[3]), (1400, top + 605), (1340, top + 605), (1340, over[1] - 4)], BLACK, width=4, head=16)
    return save_image(image, "hinh-5-1-thanh-phan-game-engine.png")


def diagram_move_sequence() -> Path:
    return sequence_diagram(
        "hinh-5-2-xu-ly-move.png",
        "XỬ LÝ THAO TÁC DI CHUYỂN",
        "Client chỉ gửi hướng di chuyển; vị trí mới do Server quyết định và được mọi Client nhận qua GAME_STATE",
        ["GameScreen\nInputController", "Client mạng\n(Controller,\nDispatcher)", "GameMessage\nHandler", "GameSession\nManager", "RoomGameLoop", "BombermanGame"],
        [
            ("self", 0, "phím W/A/S/D hoặc mũi tên: pressDirection()"),
            ("msg", 0, 1, "move(direction)"),
            ("msg", 1, 2, "MOVE {direction} qua TCP"),
            ("self", 2, "validateGameplaySession(): đúng phiên và phòng, phòng PLAYING, người chơi còn sống"),
            ("msg", 2, 3, "enqueueMove(userId, direction)"),
            ("msg", 3, 4, "offer(MoveGameCommand)"),
            ("reply", 3, 2, "ACCEPTED: không gửi phản hồi riêng"),
            ("note", "Tick kế tiếp của phòng, tối đa 50 ms sau", 4, 5),
            ("msg", 4, 5, "movePlayer(userId, direction)"),
            ("reply", 5, 4, "MoveResult (chỉ dùng nội bộ)"),
            ("msg", 4, 3, "onGameState(snapshot), mỗi 2 tick"),
            ("reply", 3, 1, "GAME_STATE tới mọi phiên trong phòng"),
            ("reply", 1, 0, "lưu snapshot, vẽ vị trí mới"),
        ],
    )


def diagram_tick_flow() -> Path:
    return flow_diagram(
        "hinh-5-3-xu-ly-mot-tick.png",
        "XỬ LÝ MỘT TICK CỦA PHÒNG",
        "Mỗi phòng có một ScheduledExecutor đơn luồng; lệnh của người chơi chỉ được thực thi ở đầu mỗi tick",
        [
            ("start", "runTickSafely() được gọi mỗi 50 ms"),
            ("step", "drainCommands(): thực thi lần lượt các lệnh MOVE, PLACE_BOMB và ngắt kết nối trong hàng đợi"),
            ("step", "game.tick(now): kích nổ bom đến hạn, lan lửa, phá tường, loại người chơi"),
            ("step", "Tăng bộ đếm tick"),
            ("decision", "Tick chẵn hoặc trận vừa có kết quả?", "Không gửi ở tick này, chuyển sang kiểm tra kết quả", "Có", "Không"),
            ("step", "createSnapshot() và phát GAME_STATE tới các phiên của phòng"),
            ("decision", "Trận đã có kết quả?", "onGameOver(): lưu trận, gửi GAME_OVER, dừng vòng lặp", "Chưa", "Có"),
            ("end", "Chờ tick kế tiếp"),
        ],
        loop=(7, 0, "Lặp lại"),
    )


def diagram_bomb_sequence() -> Path:
    return sequence_diagram(
        "hinh-5-4-dat-bom-no-bom.png",
        "ĐẶT BOM VÀ BOM PHÁT NỔ",
        "Thời điểm nổ được tính trên Server; Client chỉ hiển thị thời gian còn lại nhận trong trạng thái trận",
        ["Client A", "GameMessage\nHandler", "RoomGameLoop", "BombermanGame", "GameSession\nManager", "Các Client\ntrong phòng"],
        [
            ("msg", 0, 1, "PLACE_BOMB"),
            ("msg", 1, 2, "enqueue PlaceBombGameCommand"),
            ("msg", 2, 3, "placeBomb(userId, tickTime)"),
            ("self", 3, "còn sống, ô chưa có bom, chưa vượt số bom; tạo Bomb nổ sau 3 giây"),
            ("msg", 2, 4, "onGameState(snapshot có bom mới)"),
            ("reply", 4, 5, "GAME_STATE: bombs[] với remainingFuseMillis"),
            ("note", "Khoảng 60 tick sau (3 giây)", 2, 3),
            ("msg", 2, 3, "tick(now)"),
            ("self", 3, "kích nổ, lan lửa, nổ dây chuyền, phá tường, loại người chơi"),
            ("reply", 3, 2, "GameTickResult, có thể kèm GameOutcome"),
            ("msg", 2, 4, "onGameState(snapshot có vụ nổ)"),
            ("reply", 4, 5, "GAME_STATE: map, players[].alive, explosions[]"),
        ],
    )


def diagram_blast_flow() -> Path:
    return flow_diagram(
        "hinh-5-5-lan-lua.png",
        "THUẬT TOÁN LAN LỬA VÀ NỔ DÂY CHUYỀN",
        "Các bom đến hạn và bom bị kích nổ dây chuyền được xử lý bằng một hàng đợi trong cùng một tick",
        [
            ("start", "Bom đến hạn được đưa vào hàng đợi kích nổ"),
            ("step", "Lấy bom khỏi hàng đợi, xóa khỏi bản đồ, trả lại lượt đặt bom cho chủ bom"),
            ("step", "Thêm ô đặt bom vào vùng nổ; xét 4 hướng, mỗi hướng tối đa bằng tầm nổ"),
            ("decision", "Ô kế tiếp ngoài bản đồ hoặc là tường cứng?", "Dừng hướng này, ô không bị ảnh hưởng", "Không", "Có"),
            ("decision", "Ô là tường phá được?", "Phá tường, thêm ô vào vùng nổ, dừng hướng này", "Không", "Có"),
            ("decision", "Ô có bom khác?", "Thêm ô vào vùng nổ, đưa bom đó vào hàng đợi (nổ dây chuyền), dừng hướng này", "Không", "Có"),
            ("step", "Thêm ô vào vùng nổ"),
            ("end", "Hết 4 hướng: loại người chơi đứng trong vùng nổ; vụ nổ được hiển thị 500 ms"),
        ],
        loop=(6, 3, "Ô kế tiếp"),
    )


def diagram_outcome_flow() -> Path:
    return flow_diagram(
        "hinh-5-6-xac-dinh-ket-qua.png",
        "XÁC ĐỊNH KẾT QUẢ TRẬN",
        "Kết quả chỉ được tính lại khi có người chơi bị loại trong tick hoặc khi có người ngắt kết nối",
        [
            ("start", "Có người chơi bị loại do vụ nổ hoặc ngắt kết nối"),
            ("step", "Đếm số người chơi còn sống"),
            ("decision", "Còn đúng một người?", "GameOutcome.win(userId): người đó thắng", "Không", "Có"),
            ("decision", "Không còn ai?", "GameOutcome.draw(): trận hòa", "Không", "Có"),
            ("end", "Chưa có kết quả, trận tiếp tục"),
        ],
    )


def diagram_client_frame() -> Path:
    return flow_diagram(
        "hinh-5-7-khung-hinh-client.png",
        "XỬ LÝ MỘT KHUNG HÌNH PHÍA CLIENT",
        "Luồng mạng ghi snapshot mới; vòng vẽ JavaFX chỉ đọc snapshot đã hoàn chỉnh và nội suy chuyển động",
        [
            ("start", "AnimationTimer gọi frame(now) ở mỗi khung hình"),
            ("decision", "Có snapshot mới trong ClientState?", "Tiếp tục vẽ snapshot hiện tại", "Có", "Không"),
            ("step", "MatchTracker tạo sự kiện hiển thị; GameRenderer nhận snapshot; cập nhật HUD"),
            ("step", "InputController.update(): gửi tối đa một MOVE nếu đang giữ phím (140 ms một lần)"),
            ("decision", "Đã nhận GAME_OVER quá 1,2 giây?", "Chuyển sang màn hình RESULT", "Chưa", "Rồi"),
            ("step", "PlayerVisual nội suy vị trí; GameRenderer vẽ bản đồ, bom, lửa, nhân vật"),
            ("end", "Chờ khung hình kế tiếp"),
        ],
        loop=(6, 0, "Lặp lại"),
    )


def write(doc) -> FigureLog:
    figures = FigureLog(5)

    add_heading(doc, "CHƯƠNG 5. GAME ENGINE VÀ ĐỒNG BỘ THỜI GIAN THỰC", "CTDT-H1")
    add_heading(doc, "5.1 Phạm vi chức năng", "CTDT-H2")
    add_body(
        doc,
        "Phần Game Engine xử lý toàn bộ diễn biến trận đấu sau khi phòng chuyển sang trạng thái thi đấu. Hệ thống áp dụng mô hình Server có thẩm quyền (authoritative server): Client chỉ gửi ý định điều khiển gồm hướng di chuyển và yêu cầu đặt bom, còn Server kiểm tra, cập nhật bản đồ, người chơi, bom, vụ nổ và kết quả, sau đó gửi trạng thái hoàn chỉnh tới mọi người trong phòng.",
    )
    add_body(
        doc,
        "Phạm vi gồm tiếp nhận thao tác qua mạng, vòng lặp xử lý theo nhịp cố định cho từng phòng, luật di chuyển, đặt bom, nổ bom và nổ dây chuyền, xác định thắng hoặc hòa, xử lý người chơi mất kết nối giữa trận, đồng bộ trạng thái tới nhiều Client và phần thu nhận phím, hiển thị trận đấu ở Client. Bảng 5.1 liệt kê các chức năng đã triển khai.",
    )
    add_table(
        doc,
        "Bảng 5.1. Các chức năng thuộc phần Game Engine và đồng bộ thời gian thực",
        ["ID", "Chức năng", "Thành phần cài đặt", "Thông điệp liên quan"],
        [
            ["GE-01", "Khởi tạo trận cho phòng", "GameSessionManager.startGame(), BombermanGame.createDefault()", "Nhận từ START_GAME (Chương 4)"],
            ["GE-02", "Tiếp nhận và kiểm tra thao tác", "GameMessageHandler.handleMove(), handlePlaceBomb()", "MOVE, PLACE_BOMB, ERROR"],
            ["GE-03", "Hàng đợi lệnh theo phòng", "RoomGameLoop.enqueue(), drainCommands()", "Không có phản hồi khi chấp nhận"],
            ["GE-04", "Vòng lặp 20 tick/giây", "RoomGameLoop.runTickSafely()", "Nhịp phát GAME_STATE"],
            ["GE-05", "Di chuyển và va chạm", "BombermanGame.movePlayer()", "Kết quả thể hiện trong GAME_STATE"],
            ["GE-06", "Đặt bom", "BombermanGame.placeBomb()", "bombs[] trong GAME_STATE"],
            ["GE-07", "Nổ bom, phá tường, nổ dây chuyền", "BombermanGame.tick(), createExplosion()", "map, explosions[] trong GAME_STATE"],
            ["GE-08", "Loại người chơi, xác định kết quả", "killPlayersIn(), determineOutcome()", "players[].alive, GAME_OVER"],
            ["GE-09", "Mất kết nối giữa trận", "DisconnectGameCommand, disconnectPlayer()", "Nhận từ phần dọn phiên (Chương 3, 4)"],
            ["GE-10", "Phát trạng thái theo phòng", "GameSessionManager.broadcastGameState(), GameStateMapper", "GAME_STATE"],
            ["GE-11", "Kết thúc trận", "GameSessionManager.handleGameOver()", "GAME_OVER"],
            ["GE-12", "Thu phím và hiển thị trận", "GameScreen, InputController, GameRenderer, PlayerVisual", "Gửi MOVE, PLACE_BOMB; nhận GAME_STATE"],
        ],
        [900, 2500, 3600, 2300],
    )

    add_heading(doc, "5.2 Thiết kế Game Engine", "CTDT-H2")
    add_body(
        doc,
        "Game Engine được tách thành các lớp có trách nhiệm riêng. GameMessageHandler là cửa vào từ mạng, chỉ kiểm tra ngữ cảnh của phiên và xếp lệnh. GameSessionManager quản lý các trận đang chạy, gồm ánh xạ phòng tới vòng lặp, người chơi tới phòng và phòng tới danh sách phiên TCP cần nhận trạng thái. RoomGameLoop là vòng lặp của một phòng, gồm một hàng đợi lệnh có giới hạn và một ScheduledExecutor đơn luồng. BombermanGame chứa toàn bộ luật chơi cùng dữ liệu bản đồ, người chơi, bom và vụ nổ.",
    )
    add_body(
        doc,
        "Nguyên tắc quan trọng nhất của thiết kế là luồng mạng không bao giờ trực tiếp thay đổi trạng thái trận. Các phiên TCP chạy trên nhiều virtual thread khác nhau chỉ đưa lệnh vào hàng đợi; luồng vòng lặp của phòng là nơi duy nhất lấy lệnh ra và thực thi. Nhờ vậy thứ tự xử lý rõ ràng, không có hai luồng cùng sửa vị trí người chơi hoặc danh sách bom. BombermanGame vẫn dùng thêm stateLock để việc đọc snapshot từ luồng khác luôn thấy dữ liệu nhất quán. Hình 5.1 thể hiện các thành phần và quan hệ giữa chúng.",
    )
    figures.figure(
        doc,
        "5.1",
        diagram_components(),
        "Thành phần của Game Engine",
        "GameMessageHandler xếp lệnh qua GameSessionManager vào RoomGameLoop; vòng lặp điều khiển BombermanGame; snapshot được chuyển thành GAME_STATE; kết quả dẫn tới handleGameOver.",
        "Tổ chức Server authoritative với vòng lặp riêng cho từng phòng",
        "GameMessageHandler, GameSessionManager, RoomGameLoop, BombermanGame",
    )
    add_body(
        doc,
        "Khi Chương 4 chuyển phòng sang PLAYING, GameSessionManager.startGame() tạo BombermanGame với bản đồ mặc định và đặt người chơi vào các góc theo thứ tự vào phòng. Bản đồ 13 × 11 có tường cứng ở viền và tại các ô có cả chỉ số cột và hàng đều chẵn. Tường phá được sinh theo công thức cố định, sau đó vùng xung quanh bốn điểm xuất phát được dọn trống để người chơi có chỗ di chuyển ban đầu. Bảng 5.2 tổng hợp các thông số chính.",
    )
    add_table(
        doc,
        "Bảng 5.2. Thông số luật chơi và đồng bộ",
        ["Thông số", "Giá trị", "Thành phần cài đặt"],
        [
            ["Kích thước bản đồ", "13 cột × 11 hàng", "GameMap.COLUMNS, ROWS"],
            ["Số người mỗi trận", "2 đến 4", "BombermanGame.MIN_PLAYERS, MAX_PLAYERS"],
            ["Điểm xuất phát", "(1,1), (11,1), (1,9), (11,9)", "GameMap.SPAWN_POSITIONS"],
            ["Số bom đặt đồng thời", "1 mỗi người", "BomberPlayer.DEFAULT_BOMB_CAPACITY"],
            ["Tầm nổ", "2 ô mỗi hướng", "BomberPlayer.DEFAULT_BOMB_RANGE"],
            ["Thời gian chờ nổ", "3 giây", "BombermanGame.BOMB_FUSE"],
            ["Thời gian hiển thị vụ nổ", "500 ms", "BombermanGame.EXPLOSION_DURATION"],
            ["Nhịp xử lý", "20 tick/giây (50 ms)", "RoomGameLoop.TICKS_PER_SECOND"],
            ["Nhịp gửi trạng thái", "Mỗi 2 tick (10 lần/giây) và khi có kết quả", "RoomGameLoop.runTickSafely()"],
            ["Hàng đợi lệnh", "Tối đa 512 lệnh mỗi phòng", "RoomGameLoop.MAX_QUEUED_COMMANDS"],
            ["Lặp lệnh di chuyển ở Client", "140 ms khi giữ phím", "InputController.MOVE_REPEAT_NANOS"],
        ],
        [2900, 3200, 3200],
    )

    add_heading(doc, "5.3 Tiếp nhận thao tác người chơi", "CTDT-H2")
    add_body(
        doc,
        "Client gửi MOVE kèm hướng UP, DOWN, LEFT hoặc RIGHT, và PLACE_BOMB không có dữ liệu. Khi nhận thông điệp, GameMessageHandler xác định người gửi từ phiên TCP rồi kiểm tra theo thứ tự: người chơi đang ở một phòng với đúng phiên hiện tại, phòng đang ở trạng thái PLAYING, trận đang chạy và người chơi còn sống. Kiểm tra phiên ngăn trường hợp một kết nối cũ gửi lệnh thay cho người chơi.",
    )
    add_body(
        doc,
        "Lệnh hợp lệ được bọc thành MoveGameCommand hoặc PlaceBombGameCommand và đưa vào ArrayBlockingQueue của phòng. Server không gửi xác nhận khi lệnh được chấp nhận; hiệu lực của lệnh được Client quan sát qua GAME_STATE kế tiếp. Chỉ khi kiểm tra thất bại hoặc hàng đợi đầy, Server mới trả ERROR với mã GameCommandResultCode được liệt kê trong Bảng 5.3. Kết quả chi tiết của thao tác như bị tường, bom hoặc người chơi khác chặn được biểu diễn bằng MoveResult và chỉ dùng nội bộ trên Server. Hình 5.2 mô tả trình tự xử lý một thao tác di chuyển từ bàn phím tới khi hiển thị.",
    )
    figures.figure(
        doc,
        "5.2",
        diagram_move_sequence(),
        "Trình tự xử lý thao tác di chuyển",
        "Phím được chuyển thành MOVE, Server kiểm tra và xếp lệnh, vòng lặp phòng thực thi movePlayer ở tick kế tiếp và phát GAME_STATE tới mọi Client.",
        "Nhận dữ liệu điều khiển và cập nhật vị trí có thẩm quyền",
        "InputController, GameMessageHandler.handleMove(), GameSessionManager.enqueueMove(), BombermanGame.movePlayer()",
    )
    add_table(
        doc,
        "Bảng 5.3. Mã lỗi của thao tác trong trận",
        ["Mã lỗi", "Điều kiện phát sinh"],
        [
            ["INVALID_REQUEST", "MOVE thiếu hoặc sai dữ liệu hướng"],
            ["NOT_AUTHENTICATED", "Phiên chưa đăng nhập"],
            ["NOT_IN_ROOM", "Người chơi không ở phòng nào, hoặc phiên gửi không phải phiên đã vào phòng"],
            ["ROOM_NOT_PLAYING", "Phòng không ở trạng thái thi đấu"],
            ["PLAYER_DEAD", "Người chơi đã bị loại"],
            ["GAME_NOT_RUNNING", "Không tìm thấy vòng lặp đang chạy của phòng"],
            ["COMMAND_QUEUE_FULL", "Hàng đợi của phòng đã có 512 lệnh chưa xử lý"],
        ],
        [3000, 6300],
    )
    add_body(
        doc,
        "Trên màn hình trận đấu, Client bỏ qua các lỗi thuộc nhóm này để không làm gián đoạn người chơi. Ví dụ điển hình là người chơi vừa bị loại nhưng vẫn giữ phím di chuyển.",
    )

    add_heading(doc, "5.4 Vòng lặp xử lý và đồng bộ trạng thái", "CTDT-H2")
    add_body(
        doc,
        "Mỗi phòng đang thi đấu có một RoomGameLoop chạy trên một luồng riêng tên game-loop-<roomId>, được lập lịch cố định mỗi 50 ms. Ở đầu mỗi tick, vòng lặp lấy toàn bộ lệnh đang có trong hàng đợi và thực thi theo thứ tự đến. Sau đó BombermanGame.tick() xử lý các sự kiện phụ thuộc thời gian như bom đến hạn nổ và vụ nổ hết thời gian hiển thị. Mỗi tick chẵn, hoặc ngay khi trận vừa có kết quả, vòng lặp tạo snapshot và thông báo cho GameSessionManager. Hình 5.3 thể hiện luồng xử lý của một tick.",
    )
    figures.figure(
        doc,
        "5.3",
        diagram_tick_flow(),
        "Luồng xử lý một tick của phòng",
        "Mỗi 50 ms: thực thi lệnh trong hàng đợi, xử lý thời gian, tăng tick, gửi snapshot mỗi 2 tick, kiểm tra kết quả, lặp lại.",
        "Vòng lặp thời gian thực theo nhịp cố định cho từng phòng",
        "RoomGameLoop.start(), runTickSafely(), drainCommands()",
    )
    add_code(
        doc,
        """private void runTickSafely() {                         // trích rút gọn từ RoomGameLoop
    Instant tickTime = clock.instant();
    drainCommands(tickTime);                        // thực thi lệnh theo thứ tự đến
    GameTickResult result = game.tick(tickTime);    // bom, vụ nổ, loại người chơi
    tick++;
    if (tick % 2 == 0 || result.outcome() != null) {
        listener.onGameState(roomId, game.createSnapshot(tick, tickTime));
    }
    if (result.outcome() != null && running.compareAndSet(true, false)) {
        listener.onGameOver(roomId, game, result.outcome());
        executor.shutdown();
    }
}""",
    )
    add_body(
        doc,
        "GameSessionManager chuyển snapshot thành GameStateDto qua GameStateMapper, bọc trong NetworkMessage loại GAME_STATE với requestId rỗng và gửi tới từng phiên trong danh sách phiên của phòng. Danh sách này được chốt khi trận bắt đầu, vì vậy trạng thái của một phòng không bao giờ được gửi sang phòng khác. Các phòng có luồng và hàng đợi riêng nên nhiều trận có thể diễn ra song song mà không ảnh hưởng lẫn nhau. Bảng 5.4 mô tả nội dung của một thông điệp GAME_STATE.",
    )
    add_table(
        doc,
        "Bảng 5.4. Nội dung thông điệp GAME_STATE",
        ["Trường", "Nội dung"],
        [
            ["tick", "Số thứ tự tick tại thời điểm tạo snapshot"],
            ["gameStatus", "RUNNING hoặc FINISHED"],
            ["map", "Ma trận 11 × 13 loại ô: EMPTY, HARD_WALL, BREAKABLE_WALL"],
            ["players[]", "userId, username, position, alive, bombCapacity, activeBombs, bombRange"],
            ["bombs[]", "bombId, ownerUserId, position, blastRange, remainingFuseMillis"],
            ["explosions[]", "origin, affectedPositions, remainingMillis"],
            ["remainingPlayers", "Số người chơi còn sống"],
        ],
        [2400, 6900],
    )
    add_body(
        doc,
        "Server gửi toàn bộ trạng thái thay vì chỉ gửi phần thay đổi. Mỗi thông điệp lớn hơn nhưng Client luôn có thể thay thế trạng thái cũ bằng bản mới mà không phụ thuộc thông điệp trước đó, nên việc bỏ lỡ hoặc nhận chậm một bản cập nhật không làm sai lệch hiển thị. Với bản đồ 13 × 11 và tối đa 4 người chơi, kích thước thông điệp vẫn nhỏ so với giới hạn 1 MiB của một khung.",
    )

    add_heading(doc, "5.5 Di chuyển, đặt bom và nổ bom", "CTDT-H2")
    add_heading(doc, "5.5.1 Di chuyển và va chạm", "CTDT-H3")
    add_body(
        doc,
        "Mỗi lệnh MOVE làm người chơi di chuyển đúng một ô nếu ô đích nằm trong bản đồ, là ô trống, không có bom và không có người chơi còn sống khác đứng. Vì chỉ ô đích được kiểm tra, người chơi vừa đặt bom tại chỗ vẫn có thể bước ra khỏi ô bom, nhưng không thể bước lại vào ô đó. Người chơi đã bị loại hoặc trận đã có kết quả thì không di chuyển được.",
    )
    add_heading(doc, "5.5.2 Đặt bom", "CTDT-H3")
    add_body(
        doc,
        "PLACE_BOMB tạo một quả bom tại ô hiện tại của người chơi nếu người chơi còn sống, ô chưa có bom và số bom đang hoạt động chưa đạt giới hạn. Bom lưu người đặt, tầm nổ và thời điểm nổ bằng thời điểm của tick cộng 3 giây. Thời điểm này được tính trên Server; Client chỉ nhận thời gian còn lại trong trường remainingFuseMillis để hiển thị. Hình 5.4 thể hiện trình tự từ lúc đặt bom tới lúc bom phát nổ.",
    )
    figures.figure(
        doc,
        "5.4",
        diagram_bomb_sequence(),
        "Trình tự đặt bom và bom phát nổ",
        "Client A gửi PLACE_BOMB, vòng lặp tạo bom; sau 3 giây tick kích nổ, cập nhật bản đồ và người chơi, các Client nhận GAME_STATE có vụ nổ.",
        "Đặt bom, hẹn giờ nổ và phát trạng thái vụ nổ",
        "BombermanGame.placeBomb(), tick(), RoomGameLoop, GameSessionManager.broadcastGameState()",
    )
    add_heading(doc, "5.5.3 Nổ bom và nổ dây chuyền", "CTDT-H3")
    add_body(
        doc,
        "Trong mỗi tick, các bom đã đến hạn được đưa vào một hàng đợi kích nổ. Với từng bom, Server xóa bom, trả lại lượt đặt bom cho chủ bom và tính vùng nổ gồm ô đặt bom cùng tối đa hai ô theo mỗi hướng. Lửa dừng khi gặp mép bản đồ hoặc tường cứng. Khi gặp tường phá được, tường bị phá, ô đó bị ảnh hưởng và lửa dừng. Khi gặp một quả bom khác, bom đó được đưa vào cùng hàng đợi để nổ ngay trong tick hiện tại, tạo hiệu ứng nổ dây chuyền. Một tập mã bom đã xếp hàng bảo đảm mỗi quả bom chỉ nổ một lần.",
    )
    add_body(
        doc,
        "Người chơi còn sống đứng trong vùng nổ bị loại tại thời điểm bom nổ. Vụ nổ được giữ trong danh sách activeExplosions thêm 500 ms chỉ để Client hiển thị hiệu ứng; trong khoảng thời gian này, người chơi bước vào ô vừa có lửa không bị loại thêm. Hình 5.5 mô tả thuật toán lan lửa.",
    )
    figures.figure(
        doc,
        "5.5",
        diagram_blast_flow(),
        "Thuật toán lan lửa và nổ dây chuyền",
        "Bom được lấy khỏi hàng đợi, lửa lan theo 4 hướng, dừng ở tường cứng, phá tường mềm, kích nổ bom khác, sau đó loại người chơi trong vùng nổ.",
        "Xử lý vụ nổ, phá tường, nổ dây chuyền và va chạm với người chơi",
        "BombermanGame.tick(), createExplosion(), enqueueBomb(), killPlayersIn()",
    )

    add_heading(doc, "5.6 Kết thúc trận và mất kết nối giữa trận", "CTDT-H2")
    add_body(
        doc,
        "Kết quả trận được tính lại mỗi khi có người chơi bị loại. Nếu chỉ còn một người sống, người đó thắng; nếu không còn ai, chẳng hạn khi hai người cuối cùng cùng bị một vụ nổ loại, trận hòa. Trong các trường hợp còn lại, trận tiếp tục. Hình 5.6 thể hiện quy tắc này.",
    )
    figures.figure(
        doc,
        "5.6",
        diagram_outcome_flow(),
        "Luồng xác định kết quả trận",
        "Sau khi có người bị loại, đếm người còn sống: một người thì thắng, không còn ai thì hòa, còn lại thì trận tiếp tục.",
        "Xác định thắng, hòa sau mỗi lần có người bị loại",
        "BombermanGame.determineOutcome(), GameOutcome",
    )
    add_body(
        doc,
        "Khi một người chơi rời phòng hoặc mất kết nối giữa trận, phần phòng chờ (mục 4.4.3) gọi GameSessionManager.enqueueDisconnect(). Lệnh DisconnectGameCommand được xếp vào cùng hàng đợi như các thao tác khác, và ở tick kế tiếp disconnectPlayer() đánh dấu người chơi bị loại rồi tính lại kết quả. Cách này giữ đúng nguyên tắc chỉ luồng vòng lặp được thay đổi trạng thái trận, đồng thời bảo đảm trận hai người sẽ kết thúc với người còn lại thắng.",
    )
    add_body(
        doc,
        "Khi có kết quả, vòng lặp gửi snapshot cuối cùng, dừng bộ lập lịch và gọi handleGameOver(). Phương thức này yêu cầu lưu kết quả vào cơ sở dữ liệu (mục 6.5), sau đó gửi GAME_OVER gồm người thắng, kết quả chung và kết quả, điểm của từng người tới các phiên của phòng. Tiếp theo, Server đưa trạng thái người chơi về IN_ROOM, chuyển phòng sang FINISHED, xóa dữ liệu trận khỏi bộ nhớ và phát cập nhật sảnh. Nếu việc lưu trữ gặp lỗi, lỗi được ghi log và GAME_OVER vẫn được gửi để người chơi không bị kẹt trong trận.",
    )

    add_heading(doc, "5.7 Thu nhận thao tác và hiển thị phía Client", "CTDT-H2")
    add_body(
        doc,
        "GameScreen ánh xạ phím W, A, S, D và các phím mũi tên thành hướng di chuyển, phím Space thành đặt bom, ESC mở menu trong trận và F3 bật thông tin chẩn đoán. InputController giữ danh sách các hướng đang được nhấn, hướng nhấn sau cùng được ưu tiên. Khi vừa nhấn phím, lệnh MOVE được gửi ngay; khi tiếp tục giữ phím, lệnh được lặp lại mỗi 140 ms và mỗi khung hình gửi tối đa một lệnh. Đặt bom chỉ gửi một lần cho mỗi lần nhấn phím. Nhờ vậy lượng lệnh gửi lên Server có giới hạn dù người chơi giữ phím liên tục.",
    )
    add_body(
        doc,
        "Việc hiển thị dựa hoàn toàn vào snapshot nhận từ Server. Luồng mạng giải mã GAME_STATE và lưu vào một AtomicReference trong ClientState; vòng vẽ AnimationTimer của JavaFX chỉ đọc bản mới nhất đã hoàn chỉnh. Để chuyển động không bị giật khi Server chỉ gửi 10 trạng thái mỗi giây, PlayerVisual nội suy vị trí hiển thị về vị trí do Server gửi với tốc độ 130 ms mỗi ô, tăng gấp đôi khi bị chậm hơn một ô và nhảy thẳng tới đích khi lệch quá 2,5 ô. Client không tự dự đoán vị trí vượt trước Server. FlameClassifier chọn hình phần giữa, thân và đầu của ngọn lửa theo vùng ảnh hưởng. Khi nhận GAME_OVER, màn hình hiển thị người thắng và sau 1,2 giây chuyển sang màn hình kết quả. Hình 5.7 tóm tắt xử lý của một khung hình.",
    )
    figures.figure(
        doc,
        "5.7",
        diagram_client_frame(),
        "Xử lý một khung hình phía Client",
        "Mỗi khung hình: nhận snapshot mới nếu có, cập nhật HUD, gửi MOVE khi giữ phím, kiểm tra GAME_OVER, nội suy và vẽ.",
        "Thu phím, hiển thị trạng thái nhận từ Server và làm mượt chuyển động",
        "GameScreen.frame(), InputController.update(), PlayerVisual, GameRenderer",
    )

    add_heading(doc, "5.8 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Các hình dưới đây minh họa màn hình trận đấu. Mọi đối tượng trên bản đồ đều được vẽ từ trạng thái Server gửi về, nên các Client trong cùng phòng quan sát cùng một diễn biến.",
    )
    figures.screenshot(
        doc,
        "5.8",
        "c5-game.png",
        "Màn hình trận đấu và HUD người chơi",
        "Màn hình trận đấu với bản đồ và thẻ người chơi.",
        "Màn hình trận đấu có 2-4 người chơi: bản đồ 13 × 11, tường, thùng và các thẻ HUD người chơi ở phía trên",
        "Hiển thị GAME_STATE: bản đồ, người chơi, trạng thái còn sống",
        "GameScreen, GameRenderer, HudPlayerCard",
    )
    figures.screenshot(
        doc,
        "5.9",
        "c5-explosion.png",
        "Bom và vụ nổ",
        "Bom đang đếm giờ và vụ nổ lan theo bốn hướng.",
        "Khoảnh khắc có bom đang chờ nổ và một vụ nổ hình chữ thập, có tường mềm vừa bị phá",
        "Hiển thị bombs[], explosions[] và bản đồ sau khi phá tường",
        "GameRenderer, FlameClassifier, BombermanGame.tick()",
    )
    figures.screenshot(
        doc,
        "5.10",
        "c5-eliminated.png",
        "Người chơi bị loại",
        "Thông báo người chơi đã bị loại và tiếp tục theo dõi trận.",
        "Màn hình của người vừa bị loại với dòng YOU'RE OUT · watching the match",
        "Hiển thị players[].alive = false, chặn gửi thao tác",
        "GameScreen (outBanner), InputController",
    )
    figures.screenshot(
        doc,
        "5.11",
        "c5-menu.png",
        "Menu trong trận",
        "Menu ESC với các nút RESUME, HELP, LEAVE MATCH.",
        "Menu ESC trong trận với các nút RESUME, HELP và LEAVE MATCH",
        "Rời trận (LEAVE_ROOM) mà không tạm dừng Server",
        "MatchMenuPopup, GameClientController.leaveRoom()",
    )

    add_heading(doc, "5.9 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(doc, "Bảng 5.5 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng của phần Game Engine.")
    figures.mapping_table(doc, "Bảng 5.5. Ánh xạ hình vẽ với chức năng phần Game Engine và đồng bộ thời gian thực")

    add_heading(doc, "5.10 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 5 đã trình bày Game Engine theo mô hình Server có thẩm quyền: thao tác từ mạng được kiểm tra và xếp vào hàng đợi, mỗi phòng có một vòng lặp đơn luồng 20 tick/giây thực hiện toàn bộ thay đổi trạng thái, trạng thái đầy đủ được gửi 10 lần mỗi giây tới đúng các phiên của phòng. Các luật di chuyển, đặt bom, lan lửa, nổ dây chuyền, loại người chơi và xác định kết quả đều chạy trên Server, còn Client chỉ thu phím, nội suy và hiển thị. Kết quả trận được chuyển sang phần lưu trữ và hiển thị ở chương tiếp theo.",
    )
    return figures
