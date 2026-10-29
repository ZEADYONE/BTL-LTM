# PLAN – GHÉP PHẦN CÁ NHÂN THÀNH CÁC CHƯƠNG TIẾP THEO CỦA BÁO CÁO NHÓM

> **Trạng thái (2026-09-27): đã thực hiện**, có các điều chỉnh sau so với bản plan dưới đây:
>
> - **Bỏ Chương 7** và **bỏ toàn bộ phần kiểm thử** (không bảng test, không dẫn chiếu test class). Bảng ánh xạ hình ↔ chức năng chỉ có trong từng chương.
> - Bảng phân công sau trang bìa **không có caption**; cột họ tên và mã SV để trống để nhóm tự điền.
> - Biểu đồ vẽ bằng **PIL** (`report_diagrams.py`) theo đúng phong cách trắng đen của Chương 2, không dùng Mermaid.
> - Helper dùng chung được tách sang `report_common.py`; Chương 1–2 dựng ra giống hệt bản trước khi tách.
> - Nhịp gửi `GAME_STATE` là **mỗi 2 tick (10 lần/giây)**, không phải mỗi tick như ghi ở mục 4.
> - Ảnh giao diện chưa chụp: script chèn khung giữ chỗ và in danh sách tệp cần chụp vào `docs/bao-cao/screenshots/`.

Nguồn yêu cầu: [docs/bao-cao/prompt_bao_cao_ca_nhan.md](../docs/bao-cao/prompt_bao_cao_ca_nhan.md) (phân công mục 0.2, điểm giao mục 0.3).
Báo cáo đích: `docs/bao-cao/bao_cao_nhom_bomberman.docx`, sinh bằng `docs/bao-cao/tools/build_group_report.py` từ template `docs/bao-cao/doc temp.docx`.

---

## 0. Quyết định chính

| # | Quyết định | Lý do |
|---|---|---|
| Q1 | Mỗi phần cá nhân A/B/C/D là **một chương riêng**, nối tiếp Chương 2: Chương 3, 4, 5, 6. | Báo cáo hiện dừng ở Chương 2; style `CTDT-H1` của template tự đánh số chương nên chỉ cần thêm heading H1 là số chương tự tăng. |
| Q2 | Thêm **Chương 7. Kiểm thử và tổng hợp kết quả** (bảng test + bảng ánh xạ tổng hợp hình ↔ chức năng). | Bảng ánh xạ tổng hợp và bảng test gồm cả 4 phần, không thuộc riêng chương nào. |
| Q3 | **Bảng phân công nhiệm vụ** đặt ngay sau trang bìa, trước MỤC LỤC, trên một trang riêng. | Theo yêu cầu. Bảng chỉ ra thành viên nào viết chương nào; trong thân chương **không** ghi tên người. |
| Q4 | Mỗi chương cá nhân được viết trong **một file Python riêng** mà script chính import. | 4 người sửa song song không bị xung đột trên một file 1.000+ dòng. |
| Q5 | Biểu đồ vẽ bằng **Mermaid → PNG** (`npx @mermaid-js/mermaid-cli`, theme trắng đen); hình kiến trúc Chương 2 giữ nguyên cách vẽ PIL. | Sequence/activity diagram vẽ bằng PIL rất tốn công; Node đã có trên máy (`E:\nodejs`). |
| Q6 | Ảnh giao diện phải **chụp từ app thật** (`:client-fx:run` + server). Ảnh trong `img/` là mockup, không dùng làm ảnh minh họa kết quả. | Theo nguyên tắc "không tạo screenshot giả" trong prompt. |

---

## 1. Cấu trúc báo cáo sau khi ghép

```text
[Trang bìa]                                   (giữ nguyên từ template)
BẢNG PHÂN CÔNG NHIỆM VỤ                        ← MỚI, trang riêng
MỤC LỤC
DANH MỤC CÁC HÌNH VẼ
DANH MỤC CÁC BẢNG BIỂU
MỞ ĐẦU                                         (sửa đoạn 3)
CHƯƠNG 1. TỔNG QUAN HỆ THỐNG                   (giữ nguyên)
CHƯƠNG 2. KIẾN TRÚC VÀ THIẾT KẾ HỆ THỐNG       (giữ nguyên, sửa câu kết chương)
CHƯƠNG 3. KẾT NỐI VÀ XÁC THỰC                  ← Phần A
CHƯƠNG 4. SẢNH VÀ PHÒNG CHỜ                    ← Phần B
CHƯƠNG 5. GAME ENGINE VÀ ĐỒNG BỘ THỜI GIAN THỰC ← Phần C
CHƯƠNG 6. KIẾN TRÚC DESKTOP CLIENT VÀ DỮ LIỆU TRẬN ĐẤU ← Phần D
CHƯƠNG 7. KIỂM THỬ VÀ TỔNG HỢP KẾT QUẢ          ← chung
KẾT LUẬN                                       (viết lại)
TÀI LIỆU THAM KHẢO                              (bổ sung)
```

---

## 2. Trang "Bảng phân công nhiệm vụ" (sau trang bìa)

### 2.1 Vị trí và định dạng

- Chèn trong `build_report()` **ngay sau** `clear_after_cover(doc)` / `replace_cover_text(doc)` và **trước** `add_heading(doc, "MỤC LỤC", "CTDT-H0")`.
- Tiêu đề: `BẢNG PHÂN CÔNG NHIỆM VỤ`, style `CTDT-H0` (căn giữa, in đậm 14pt, xuất hiện trong mục lục).
- Một đoạn dẫn ngắn (style `CTDT-Text`), sau đó là bảng. Dùng `add_table()` sẵn có, caption `Bảng 0.1. Phân công nhiệm vụ của các thành viên` **hoặc** không caption (xem [CẦN XÁC NHẬN] mục 9).
- Kết thúc bằng page break để MỤC LỤC sang trang mới.

### 2.2 Nội dung bảng

| STT | Mã SV | Họ và tên | Module phụ trách | Nội dung lập trình mạng | Chương trong báo cáo |
|---|---|---|---|---|---|
| 1 (trưởng nhóm) | [CẦN BỔ SUNG] | [CẦN BỔ SUNG] | Kết nối và xác thực | TCP Server/Client, phiên làm việc, giao thức đóng khung và mã hóa message, đăng ký/đăng nhập, trạng thái online, phát hiện mất kết nối | Chương 3 |
| 2 | [CẦN BỔ SUNG] | [CẦN BỔ SUNG] | Sảnh và phòng chờ | Đồng bộ danh sách online/phòng, tạo/vào/rời phòng, READY/START/PLAY_AGAIN, broadcast trạng thái phòng | Chương 4 |
| 3 | [CẦN BỔ SUNG] | [CẦN BỔ SUNG] | Game Engine và đồng bộ thời gian thực | Nhận MOVE/PLACE_BOMB, vòng lặp 20 tick/giây, broadcast GAME_STATE/GAME_OVER, thu input và render phía Client | Chương 5 |
| 4 | [CẦN BỔ SUNG] | [CẦN BỔ SUNG] | Kiến trúc Desktop Client và dữ liệu trận đấu | Tầng ứng dụng Client (gửi lệnh, dispatch message, cập nhật UI), lưu trận và tính điểm, HISTORY/RANKING | Chương 6 |
| Chung | Cả nhóm | — | Tổng quan, kiến trúc, kiểm thử | — | Chương 1, 2, 7 |

Độ rộng cột gợi ý (twips, tổng ≈ 9000): `[600, 1100, 1700, 1700, 2900, 1000]`.

---

## 3. Quy ước chung cho các chương cá nhân

### 3.1 Đánh số và style

| Thành phần | Quy ước | Style / hàm |
|---|---|---|
| Heading chương | `CHƯƠNG N. <TÊN>` — script tự bỏ tiền tố, style tự đánh số | `add_heading(doc, ..., "CTDT-H1")` |
| Mục cấp 2/3 | `N.x Tên`, `N.x.y Tên` | `CTDT-H2`, `CTDT-H3` |
| Hình | `Hình N.k. <Tên>` — đánh tay, liên tục trong chương | `add_figure()` → `CTDT-Hinh` |
| Bảng | `Bảng N.k. <Tên>` | `add_table()` → `CTDT-Bang` |
| Đoạn văn | 13pt, Times New Roman | `add_body()`, `add_bullet()` |

### 3.2 Khung chuẩn của một chương cá nhân (áp dụng cho Chương 3–6)

```text
N.1 Phạm vi chức năng            – 1–2 đoạn + Bảng N.1 "Chức năng đã triển khai" (ID | Chức năng | Thành phần cài đặt | Xác minh)
N.2 … N.(k) Các mục kỹ thuật      – theo nội dung riêng từng chương (mục 4 bên dưới)
N.(k+1) Giao diện minh họa        – ảnh chụp thật
N.(k+2) Kết quả và xử lý lỗi      – bảng tình huống lỗi → phản hồi
N.(k+3) Ánh xạ hình vẽ với chức năng – Bảng "STT | Mã hình | Hình | Chức năng | Thành phần cài đặt"
N.(k+4) Kết chương                – 1 đoạn
```

### 3.3 Nguyên tắc nội dung

- **Không lặp lại Chương 2.** Chương 2 đã có kiến trúc tổng thể, ERD, nhóm message. Chương cá nhân dẫn chiếu ("như Hình 2.2") rồi đi sâu vào cài đặt.
- **Khối "Source: File/Class/Method" trong prompt chỉ dùng trong file phân tích.** Trong docx, ghi nguồn bằng cột "Thành phần cài đặt" của bảng (dạng `ClassName.method()`), không dán khối text.
- **Code:** mỗi chương chèn tối đa 2–3 đoạn trích ngắn (≤ 15 dòng), đúng các Code-ID ở mục 9 của prompt. Dùng đoạn văn font Consolas 10pt có viền (cần thêm helper `add_code()`, xem mục 7).
- **Điểm giao liên chương** (mục 0.3 của prompt): mỗi chương chỉ viết phần của mình và có một câu dẫn chiếu, ví dụ: "Việc dọn phòng khi người chơi mất kết nối được trình bày ở mục 4.5."
- Văn phong học thuật theo mục 12 của prompt. Luôn nêu rõ bên gửi, bên nhận, dữ liệu gì, xử lý ở đâu và trạng thái đổi thế nào.
- Dung lượng mục tiêu: **8–12 trang/chương**, 5–8 hình, 3–5 bảng.

---

## 4. Kế hoạch chi tiết từng chương

Thông số dưới đây đã đối chiếu với source. Người viết vẫn phải xác minh lại trước khi đưa vào báo cáo.

### CHƯƠNG 3. KẾT NỐI VÀ XÁC THỰC (Phần A)

**Mục lục chương**

| Mục | Nội dung chính | Căn cứ source |
|---|---|---|
| 3.1 Phạm vi chức năng | Kết nối TCP, phiên, giao thức, đăng ký/đăng nhập/đăng xuất, trạng thái online, mất kết nối, màn hình đăng nhập. Bảng 3.1. | Mục 0.2 của prompt |
| 3.2 Giao thức truyền tin | Frame = 4 byte độ dài (`writeInt`/`readInt`) + JSON `NetworkMessage{type, requestId, payload}`; giới hạn payload 1 MiB; lỗi định dạng → `ProtocolException`; `requestId` để ghép phản hồi. Bảng 3.2 các message của phần (REGISTER_*, LOGIN_*, LOGOUT, PING/PONG, ERROR). | `MessageEncoder`, `MessageDecoder`, `NetworkMessage`, `MessageType` |
| 3.3 Tổ chức kết nối phía Server | `TcpGameServer`: cổng `bomberman.tcp.port` (mặc định 8081), một luồng acceptor, mỗi kết nối chạy trên một virtual thread (`newVirtualThreadPerTaskExecutor`); `ClientSession` (TCP_NODELAY, read loop); `ConnectionManager`; `SessionWriter` ghi tuần tự để tránh lẫn frame khi nhiều luồng cùng gửi; `MessageDispatcher` định tuyến theo `MessageType`. | `TcpGameServer.acceptConnections`, `ClientSession`, `SessionWriter`, `MessageDispatcher.dispatch` |
| 3.4 Tầng kết nối phía Client | `GameNetworkClient`: connect timeout 3 s, read loop trên virtual thread; `PendingRequests`: ghép phản hồi theo `requestId`, timeout 5 s; `ClientNetworkConfig`: host/port lấy từ system property → biến môi trường → `client.properties`; `ServerAddressPopup`. | các class tương ứng |
| 3.5 Đăng ký và đăng nhập | Kiểm tra input ở Client (`InputValidation`) → `LOGIN_REQUEST` → `AuthMessageHandler` → `AuthenticationService` (so khớp `PasswordEncoder`) → `OnlineUserRegistry.markOnline` (chặn đăng nhập trùng: `ACCOUNT_ALREADY_ONLINE`) → `LOGIN_RESPONSE` → broadcast sảnh (dẫn chiếu Chương 4). Bảng 3.3 mã kết quả `AuthResultCode` và thông báo tương ứng ở Client. | `AuthMessageHandler`, `AuthenticationService`, `AuthResultCode`, `ClientMessageDispatcher` (bảng thông báo) |
| 3.6 Heartbeat và xử lý mất kết nối | Server trả PONG cho PING. Khi read loop kết thúc: `MessageDispatcher.onDisconnect` → `AuthMessageHandler.cleanupSession` → (dọn phòng, Chương 4) → `logout` → `markOffline` → `broadcastLobbyUpdates`. | `MessageDispatcher.replyWithPong`, `AuthMessageHandler.cleanupSession` |
| 3.7 Giao diện minh họa | Ảnh màn hình đăng nhập, đăng ký, popup địa chỉ Server, các trạng thái lỗi. | `LoginScreen`, `ServerAddressPopup` |
| 3.8 Kết quả và xử lý lỗi | Bảng: sai mật khẩu / trùng tên / đang online nơi khác / Server không chạy / frame hỏng. | |
| 3.9 Ánh xạ hình ↔ chức năng | | |

**Hình**

| Mã | Tên | Loại | Cách tạo |
|---|---|---|---|
| Hình 3.1 | Cấu trúc một khung thông điệp TCP | Sơ đồ khối (length + JSON) | Mermaid `block-beta` hoặc PIL |
| Hình 3.2 | Mô hình luồng xử lý kết nối phía Server | Component/flow: acceptor → virtual thread/session → dispatcher | Mermaid `flowchart` |
| Hình 3.3 | Trình tự thiết lập kết nối TCP | Sequence | Mermaid |
| Hình 3.4 | Trình tự đăng nhập | Sequence Client UI → GameClientController → GameNetworkClient → ClientSession → AuthMessageHandler → AuthenticationService/DB → OnlineUserRegistry | Mermaid |
| Hình 3.5 | Luồng xử lý mất kết nối | Activity (dừng ở điểm gọi sang Chương 4/5) | Mermaid |
| Hình 3.6 | Màn hình đăng nhập/đăng ký | Ảnh chụp | `screenshots/c3-login.png`, `c3-register.png` |
| Hình 3.7 | Chọn địa chỉ Server | Ảnh chụp | `c3-server-address.png` |
| Hình 3.8 | Thông báo lỗi đăng nhập | Ảnh chụp | `c3-login-error.png` |

**Code trích dẫn:** vòng `accept` trong `TcpGameServer`; `MessageEncoder.encode` (ghi độ dài + payload); xử lý đăng nhập trong `AuthMessageHandler`.

**Test dẫn chiếu:** `TcpGameServerIntegrationTest`, `SessionWriterConcurrencyTest`, `AuthenticationIntegrationTest`, `MessageCodecTest`, `ClientNetworkIntegrationTest`, `PendingRequestsTest`, `ClientNetworkConfigTest`, `InputValidationTest`.

**[CẦN XÁC NHẬN]:** Client JavaFX **không gửi PING** (không tìm thấy trong `client-fx`), chỉ Server có nhánh trả PONG. Cần ghi đúng là "Server hỗ trợ PING/PONG", không viết là "có heartbeat định kỳ".

---

### CHƯƠNG 4. SẢNH VÀ PHÒNG CHỜ (Phần B)

| Mục | Nội dung chính | Căn cứ source |
|---|---|---|
| 4.1 Phạm vi chức năng | Bảng 4.1. | |
| 4.2 Mô hình trạng thái người chơi và phòng | `PlayerStatus` FREE → IN_ROOM → PLAYING; `RoomStatus`; `GameRoom` tối đa 4 người, tên phòng ≤ 60 ký tự; chủ phòng. Bảng 4.2 các message của phần. | `PlayerStatus`, `RoomStatus`, `GameRoom`, `RoomManager` |
| 4.3 Đồng bộ sảnh | Sau đăng nhập, Client gửi `ONLINE_USERS_REQUEST`/`ROOM_LIST_REQUEST`; `LobbyService.broadcastLobbyUpdates` chỉ gửi cho session đang FREE. Nêu rõ các thời điểm gây broadcast: đăng nhập, đăng xuất, tạo/vào/rời phòng, kết thúc trận. | `LobbyService`, `LobbyMessageHandler` |
| 4.4 Tạo, tham gia, rời phòng | Điều kiện, `RoomResultCode`, `ROOM_STATE` gửi cho thành viên phòng, cập nhật danh sách phòng ở sảnh. Quick Play ở Client: vào phòng đông nhất còn chỗ, thử lại một lần khi tranh chấp, nếu không thì tạo phòng. | `RoomMessageHandler`, `RoomManager`, `QuickPlay` |
| 4.5 Sẵn sàng, bắt đầu và chơi lại | READY; START_GAME: chỉ chủ phòng, ≥ 2 người, mọi người đã sẵn sàng (kể cả chủ phòng) → chuyển cho `GameSessionManager` (Chương 5). `LobbyStatus` ở Client hiển thị checklist khớp với luật Server. PLAY_AGAIN. Dọn phòng qua `handleSessionExit` khi mất kết nối (nối từ mục 3.6). | `RoomManager.startGame`, `LobbyStatus`, `RoomMessageHandler.handleSessionExit` |
| 4.6 Giao diện minh họa | Home, Room Browser, tạo phòng, Room Lobby (chưa sẵn sàng / đủ điều kiện), xem trước bản đồ. | `HomeScreen`, `RoomBrowserScreen`, `RoomLobbyScreen`, `MapPreview` |
| 4.7 Kết quả và xử lý lỗi | Phòng đầy, phòng đang chơi, phòng không tồn tại, không phải chủ phòng, chưa đủ người. | `RoomResultCode` |
| 4.8 Ánh xạ hình ↔ chức năng | | |

**Hình**

| Mã | Tên | Loại |
|---|---|---|
| Hình 4.1 | Sơ đồ trạng thái người chơi và phòng | Mermaid `stateDiagram` |
| Hình 4.2 | Trình tự tải và cập nhật sảnh | Sequence (nhiều Client nhận broadcast) |
| Hình 4.3 | Trình tự tạo phòng | Sequence |
| Hình 4.4 | Trình tự tham gia/rời phòng | Sequence |
| Hình 4.5 | Trình tự sẵn sàng và bắt đầu trận | Sequence (kết thúc ở `GameSessionManager`) |
| Hình 4.6 | Màn hình Home | Ảnh `c4-home.png` |
| Hình 4.7 | Danh sách phòng | Ảnh `c4-room-browser.png` |
| Hình 4.8 | Phòng chờ và trạng thái sẵn sàng | Ảnh `c4-room-lobby.png` (nên chụp 2 Client cạnh nhau để thấy đồng bộ) |

**Code trích dẫn:** `LobbyService.broadcastLobbyUpdates`; kiểm tra điều kiện trong `RoomManager.startGame`.

**Test dẫn chiếu:** `RoomManagerTest`, `LobbyStatusTest`, `QuickPlayTest`, `MapPreviewTest`.

---

### CHƯƠNG 5. GAME ENGINE VÀ ĐỒNG BỘ THỜI GIAN THỰC (Phần C)

| Mục | Nội dung chính | Căn cứ source |
|---|---|---|
| 5.1 Phạm vi chức năng | Bảng 5.1. | |
| 5.2 Thiết kế Game Engine | Server authoritative; `BombermanGame`, `GameMap` 13×11 với 4 vị trí xuất phát, `BomberPlayer` (1 bom, tầm nổ 2), `Bomb`, `Explosion`; mỗi phòng một `RoomGameLoop`; `GameSessionManager` gắn phòng ↔ vòng lặp. Bảng 5.2 thông số luật chơi. | các class trong `server/game` |
| 5.3 Nhận thao tác người chơi | `MOVE`/`PLACE_BOMB` → `GameMessageHandler` → tạo `GameCommand` → hàng đợi của `RoomGameLoop` (tối đa 512 lệnh, đầy → `QUEUE_FULL`) → xử lý ở tick kế tiếp. Không sửa trạng thái ngay trên luồng mạng. | `GameMessageHandler`, `GameSessionManager.enqueue`, `RoomGameLoop` |
| 5.4 Di chuyển, đặt bom, nổ bom | Kiểm tra ô đi được; đặt bom (giới hạn số bom); ngòi nổ 3 s; lửa tồn tại 500 ms; lan 4 hướng, dừng ở tường cứng, phá thùng; loại người chơi; nổ dây chuyền (**xác nhận trong source**). | `BombermanGame`, `MoveResult`, `PlaceBombResult` |
| 5.5 Đồng bộ trạng thái trận | 20 tick/giây (50 ms); mỗi tick gửi **toàn bộ** `GameStateDto` cho đúng các session trong phòng; lý do chọn full state thay vì delta; an toàn luồng (một luồng xử lý mỗi phòng + hàng đợi lệnh); nhiều phòng chạy độc lập. | `RoomGameLoop`, `GameStateMapper`, `GameSessionManager.broadcastGameState` |
| 5.6 Mất kết nối giữa trận và kết thúc trận | `DisconnectGameCommand` (nối từ mục 3.6/4.5); xác định thắng/hòa (`GameOutcome`); broadcast `GAME_OVER`; trả người chơi về IN_ROOM; gọi lưu trận (chi tiết ở Chương 6). | `GameSessionManager.handleGameOver` |
| 5.7 Thu input và hiển thị phía Client | `InputController`: ưu tiên hướng đang giữ, tối đa một MOVE mỗi frame; `GameRenderer` chỉ vẽ snapshot nhận từ Server; `PlayerVisual` nội suy vị trí hiển thị; `FlameClassifier` chọn sprite lửa; `BoardLayout`; HUD; menu ESC không tạm dừng Server. | các class `fx/game/*`, `GameScreen` |
| 5.8 Giao diện minh họa | | |
| 5.9 Kết quả và xử lý lỗi | Lệnh khi trận chưa chạy (`GAME_NOT_RUNNING`), hàng đợi đầy, di chuyển bị chặn, đặt bom vượt giới hạn. | `GameCommandResultCode` |
| 5.10 Ánh xạ hình ↔ chức năng | | |

**Hình**

| Mã | Tên | Loại |
|---|---|---|
| Hình 5.1 | Thành phần của Game Engine | Mermaid class/component |
| Hình 5.2 | Vòng lặp xử lý một tick | Activity: lấy lệnh → cập nhật → nổ bom → kiểm tra kết thúc → broadcast |
| Hình 5.3 | Trình tự xử lý MOVE | Sequence từ phím bấm tới khung hình mới |
| Hình 5.4 | Trình tự đặt bom và nổ bom | Sequence |
| Hình 5.5 | Thuật toán lan lửa | Activity (4 hướng, tường cứng/thùng/người chơi) |
| Hình 5.6 | Xác định kết quả trận | Flowchart |
| Hình 5.7 | Màn hình trận đấu và HUD | Ảnh `c5-game.png` |
| Hình 5.8 | Bom và vụ nổ | Ảnh `c5-explosion.png` |
| Hình 5.9 | Người chơi bị loại | Ảnh `c5-eliminated.png` |
| Hình 5.10 | Menu ESC trong trận | Ảnh `c5-menu.png` |

**Code trích dẫn:** vòng tick trong `RoomGameLoop`; hàm lan lửa trong `BombermanGame`; `broadcastGameState`.

**Test dẫn chiếu:** `BombermanGameplayTest`, `BombermanMovementTest`, `BombermanDisconnectTest`, `GameMapTest`, `GameStateBroadcastIntegrationTest`, `DisconnectDuringGameIntegrationTest`, `InputControllerTest`, `BoardLayoutTest`, `FlameClassifierTest`, `PlayerVisualTest`.

**[CẦN XÁC NHẬN]:** `PLAYER_DIED` có trong `MessageType` nhưng luồng hiện tại không gửi (Chương 2 đã ghi). Chương 5 giữ thống nhất với Chương 2.

---

### CHƯƠNG 6. KIẾN TRÚC DESKTOP CLIENT VÀ DỮ LIỆU TRẬN ĐẤU (Phần D)

| Mục | Nội dung chính | Căn cứ source |
|---|---|---|
| 6.1 Phạm vi chức năng | Bảng 6.1. | |
| 6.2 Kiến trúc Desktop Client | Các tầng: tầng vận chuyển (Chương 3) → `GameClientController` (facade gửi lệnh, trả `CompletableFuture`) / `ClientMessageDispatcher` → `ClientState` + `ClientStateListener` → màn hình. `BombermanApp` nối các thành phần; `AppShell` khung 1280×720 co giãn; `Navigator`/`ScreenNavigator`. | `BombermanApp`, `AppShell`, `ScreenNavigator` |
| 6.3 Xử lý message và mô hình luồng | Luồng mạng → `Platform::runLater` → JavaFX Application Thread; riêng `GAME_STATE` được gán thẳng từ luồng mạng (snapshot) để giảm độ trễ, các message khác chạy trên UI thread; bảng phân nhánh `MessageType` → hành động. | `ClientMessageDispatcher` (dòng ~63–98), `ClientState` |
| 6.4 Lưu trận và tính điểm (Server) | `GameSessionManager.handleGameOver` (Chương 5) → `MatchPersistenceService.recordCompletedMatch` (`@Transactional`): tạo `Match`, `MatchPlayer`, cập nhật bộ đếm trong `User`; `MatchScoring` (lưu theo đơn vị nửa điểm: thắng 2 = 1 điểm, hòa 1 = 0,5 điểm, thua 0); MySQL, `ddl-auto=update`, `docker-compose.yml`. Dẫn chiếu ERD Hình 2.5, không vẽ lại. | `MatchPersistenceService`, `MatchScoring`, `Match`, `MatchPlayer`, `User` |
| 6.5 Kết quả, lịch sử, bảng xếp hạng | `GAME_OVER` → `ResultScreen` (VICTORY/DEFEAT/DRAW, `MatchTracker` giữ thứ tự slot ổn định); `HISTORY_REQUEST/RESPONSE` → `HistoryScreen` (giờ địa phương); `RANKING_REQUEST/RESPONSE` → `LeaderboardScreen` (podium top 3 + bảng). Bảng 6.2 các message của phần. | `HistoryMessageHandler`, `MatchHistoryService`, `RankingService`, các màn hình |
| 6.6 Tài nguyên, cài đặt và đóng gói | Design system (`ui/theme`), SVG rasterize/recolor, `UserPreferences` (không lưu mật khẩu), `packageApp` ra bản Windows. Viết ngắn. | |
| 6.7 Giao diện minh họa | | |
| 6.8 Kết quả và xử lý lỗi | Lưu trận thất bại (Server ghi log, vẫn gửi GAME_OVER), timeout yêu cầu, lịch sử rỗng. | `GameSessionManager.handleGameOver` try/catch, `PendingRequests` |
| 6.9 Ánh xạ hình ↔ chức năng | | |

**Hình**

| Mã | Tên | Loại |
|---|---|---|
| Hình 6.1 | Kiến trúc phân tầng của Desktop Client | Mermaid flowchart |
| Hình 6.2 | Luồng phân phối message tới giao diện | Flowchart (có nhánh GAME_STATE) |
| Hình 6.3 | Sơ đồ điều hướng màn hình | Mermaid `stateDiagram` Login → Home → Browser → Lobby → Game → Result, History, Leaderboard |
| Hình 6.4 | Trình tự lưu kết quả trận | Sequence |
| Hình 6.5 | Trình tự tải lịch sử và bảng xếp hạng | Sequence |
| Hình 6.6 | Màn hình kết quả trận | Ảnh `c6-result-victory.png` (+ `c6-result-draw.png` nếu có) |
| Hình 6.7 | Lịch sử trận đấu | Ảnh `c6-history.png` |
| Hình 6.8 | Bảng xếp hạng | Ảnh `c6-leaderboard.png` |

**Code trích dẫn:** nhánh `switch` trong `ClientMessageDispatcher`; `recordCompletedMatch`.

**Test dẫn chiếu:** `MatchPersistenceIntegrationTest`, `ClientMessageDispatcherTest`, `MatchTrackerTest`, `GameFormatsTest`, `UserPreferencesTest`, `SvgRasterizerTest`, `SvgRecolorTest`.

---

### CHƯƠNG 7. KIỂM THỬ VÀ TỔNG HỢP KẾT QUẢ (chung)

| Mục | Nội dung |
|---|---|
| 7.1 Môi trường kiểm thử | JDK 21, MySQL (docker-compose), Windows; lệnh `.\gradlew.bat test`. |
| 7.2 Kiểm thử tự động | Bảng 7.1: Test class → Phần → Chức năng → Kết quả. **Chỉ điền PASS sau khi chạy thật**; ghi ngày chạy. |
| 7.3 Kiểm thử thủ công nhiều Client | Bảng 7.2 theo mục 10 của prompt (connect, login, full room, ready/start, move, bom, disconnect, history, ranking) với 2–4 Client thật. |
| 7.4 Bảng ánh xạ tổng hợp | Bảng 7.3 gộp toàn bộ bảng ánh xạ của Chương 3–6: `STT | Chương | Mã hình | Tên hình | Chức năng | Thành phần cài đặt`. Sinh tự động từ danh sách hình của 4 chương (mục 7). |
| 7.5 Kết chương | |

---

## 5. Sửa phần nhóm hiện có

| Vị trí | Sửa |
|---|---|
| MỞ ĐẦU, đoạn 3 | Thay "Phần báo cáo nhóm tập trung vào…" bằng đoạn mô tả bố cục: Chương 1–2 là phần chung; Chương 3–6 là nội dung từng module theo bảng phân công; Chương 7 là kiểm thử và tổng hợp. |
| Kết chương 2 | Thêm câu chuyển tiếp sang Chương 3. |
| KẾT LUẬN | Viết lại: kết quả đạt được theo 4 module, hạn chế (lấy từ mục 2.10 hiện có), hướng phát triển. Bỏ câu "cần được bổ sung phần cá nhân". |
| TÀI LIỆU THAM KHẢO | Thêm tài liệu Java Socket/virtual threads, JavaFX, Spring Data JPA (ghi đúng tên tài liệu thật đã tham khảo). |
| Metadata | `title` → "Báo cáo bài tập lớn - Bomberman Online Mini"; `subject` bỏ chữ "chung". |

---

## 6. Ảnh chụp giao diện

- Thư mục: `docs/bao-cao/screenshots/`, tên theo bảng hình ở mục 4 (`c3-*.png` … `c6-*.png`).
- Cách chụp: chạy MySQL + `.\gradlew.bat :server:bootRun`, mở 2–4 Client `.\gradlew.bat :client-fx:run`, cửa sổ 1280×720; ảnh đồng bộ nhiều Client thì ghép 2 cửa sổ cạnh nhau.
- Nếu ảnh chưa có, script chèn **khung giữ chỗ** có chữ `[CHÈN ẢNH: c4-room-lobby.png]` thay vì bỏ qua, để dễ thấy ảnh nào còn thiếu.

---

## 7. Thay đổi kỹ thuật trong script

```text
docs/bao-cao/
├── tools/
│   ├── build_group_report.py        # giữ helper + Chương 1–2; gọi các chương mới
│   ├── report_common.py             # (tách) add_heading/add_body/add_table/add_figure/add_code/add_screenshot
│   ├── chapter_3_connection.py      # write(doc)  – Phần A
│   ├── chapter_4_lobby_room.py      # write(doc)  – Phần B
│   ├── chapter_5_game_engine.py     # write(doc)  – Phần C
│   ├── chapter_6_client_data.py     # write(doc)  – Phần D
│   ├── chapter_7_testing.py         # write(doc, figure_registry)
│   └── render_diagrams.ps1          # chạy mermaid-cli cho mọi .mmd
├── diagrams/                        # c3-*.mmd … c6-*.mmd (mỗi hình một file)
├── generated-report-assets/         # PNG sinh ra (Chương 2 cũ + các .mmd mới)
└── screenshots/                     # ảnh chụp thật
```

Các bước sửa script:

1. Tách helper sang `report_common.py`; `build_group_report.py` import lại. Output docx của Chương 1–2 phải giữ nguyên.
2. Thêm `add_assignment_page(doc)` chạy sau khi xử lý bìa, trước MỤC LỤC (mục 2).
3. Thêm helper:
   - `add_code(doc, code, caption)`: Consolas 10pt, viền mảnh.
   - `add_screenshot(doc, name, caption, alt)`: chèn ảnh nếu có, nếu không thì chèn khung giữ chỗ.
   - `FigureRegistry`: mỗi lần `add_figure`/`add_screenshot` ghi lại (chương, mã hình, tên, chức năng, thành phần) để Chương 7 tự sinh Bảng 7.3.
4. Sau Chương 2, gọi lần lượt `chapter_3…chapter_7.write(doc)`, rồi mới đến KẾT LUẬN/TLTK.
5. `render_diagrams.ps1`: `npx -y @mermaid-js/mermaid-cli -i diagrams\X.mmd -o generated-report-assets\X.png -t neutral -b white -w 1600`.
6. Build: `python docs\bao-cao\tools\build_group_report.py`, sau đó `export_docx_pdf.ps1 -SaveUpdatedFields` để Word cập nhật mục lục và danh mục hình/bảng.

---

## 8. Cập nhật prompt cá nhân cho khớp

Sửa `docs/bao-cao/prompt_bao_cao_ca_nhan.md`:

- Mục 11: đổi "CHƯƠNG 4. NỘI DUNG CÁ NHÂN" (một chương, các mục 4.1–4.4) thành **Chương 3, 4, 5, 6** riêng, cộng Chương 7. Đổi mã hình từ `Hình 4.x` sang `Hình N.x` theo chương.
- Dòng 7 và 18 ("không ghi tên người phụ trách"): thêm "tên chỉ xuất hiện trong bảng phân công sau trang bìa".
- Mục 13: output ngoài file phân tích còn có `chapter_N_*.py` và `diagrams/*.mmd`.

---

## 9. Thứ tự thực hiện và phân việc

| Bước | Việc | Người | Điều kiện xong |
|---|---|---|---|
| 1 | Sửa prompt (mục 8), tách script, thêm trang phân công, helper, khung rỗng 5 chương | TV4 (hoặc trưởng nhóm) | Build ra docx có trang phân công và 5 heading chương rỗng; Chương 1–2 không đổi |
| 2 | Mỗi người viết file phân tích trong `plans/` theo prompt (source mapping, flow, [CẦN XÁC NHẬN]) | TV1–4 song song | Mọi thông số có nguồn |
| 3 | Vẽ `.mmd` và render PNG | TV1–4 | PNG trong `generated-report-assets` |
| 4 | Chạy app, chụp ảnh | Cả nhóm, một buổi, 4 máy/4 cửa sổ | Đủ ảnh mục 4 |
| 5 | Viết `chapter_N_*.py` | TV1–4 | Build không lỗi, không còn khung giữ chỗ |
| 6 | Chạy `gradlew test`, điền Chương 7 | Trưởng nhóm | Bảng 7.1 có kết quả thật và ngày chạy |
| 7 | Sửa MỞ ĐẦU/KẾT LUẬN/TLTK, export PDF, rà mục lục + danh mục hình/bảng | Trưởng nhóm | PDF đúng số chương 1–7, số hình liên tục |

Checklist rà soát mỗi chương:

- [ ] Có đủ khung mục 3.2.
- [ ] Mỗi hình có caption `Hình N.k.` và có dòng trong bảng ánh xạ.
- [ ] Không lặp nội dung Chương 2; có dẫn chiếu tới chương liền kề ở điểm giao.
- [ ] Không có tên thành viên trong thân chương.
- [ ] Không có ảnh mockup, không ghi PASS cho test chưa chạy.
- [ ] Các thông số (8081, 20 tick, 3 s, 500 ms, 13×11, 4 người, 1 MiB, 5 s timeout) khớp source.

---

## 10. Điểm cần xác nhận

1. Họ tên, mã SV và thứ tự thành viên (trưởng nhóm xếp số 1, như trang bìa template yêu cầu).
2. Bảng phân công có đánh số `Bảng 0.1` (sẽ xuất hiện trong danh mục bảng) hay để bảng không caption?
3. Có giữ Chương 7 riêng không, hay gộp bảng ánh xạ tổng hợp vào cuối Chương 6/KẾT LUẬN?
4. Client libGDX: chỉ nhắc một câu ở Chương 6 như client tương thích ngược, hay bỏ hẳn?
5. Giảng viên có quy định số trang tối đa không? (ảnh hưởng mục tiêu 8–12 trang/chương)
