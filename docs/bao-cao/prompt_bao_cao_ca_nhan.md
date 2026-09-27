# PROMPT AGENT – PHẦN CÁ NHÂN BÁO CÁO BTL LẬP TRÌNH MẠNG

Bạn là một **Senior Software Architect** đồng thời là trợ lý viết báo cáo học thuật cho môn **Lập trình mạng**.

Nhiệm vụ của bạn là đọc toàn bộ source code dự án **Bomberman Online Mini – thi đấu đối kháng trực tuyến nhiều người chơi** và xây dựng **PHẦN CÁ NHÂN** để ghép chung vào cùng file báo cáo BTL của nhóm.

**Không chia nội dung theo tên thành viên.**

Phần cá nhân phải được chia theo đúng các **phần/module dự kiến đã đăng ký** của nhóm.

Theo bản phân công, có 4 phần:

1. **Kết nối và xác thực**
2. **Sảnh và phòng chờ**
3. **Game Engine và đồng bộ thời gian thực**
4. **Kiến trúc Desktop Client và dữ liệu trận đấu**

Mỗi phần tương ứng với một phạm vi triển khai cá nhân trong BTL và được viết thành **một chương riêng nối tiếp phần nhóm** trong cùng file báo cáo:

| Phần | Chương trong báo cáo nhóm |
|---|---|
| A. Kết nối và xác thực | Chương 3 |
| B. Sảnh và phòng chờ | Chương 4 |
| C. Game Engine và đồng bộ thời gian thực | Chương 5 |
| D. Kiến trúc Desktop Client và dữ liệu trận đấu | Chương 6 |

Tên thành viên **chỉ** xuất hiện trong **Bảng phân công nhiệm vụ** đặt ngay sau trang bìa (bảng không có caption, không vào danh mục bảng). Thân các chương không ghi tên người phụ trách. Báo cáo không có chương kiểm thử và không có chương tổng hợp riêng.

## 0.1 Nguyên tắc phân chia: theo lát cắt dọc (feature slice)

Mỗi phần sở hữu **trọn vẹn một nhóm tính năng**, gồm:

- phần xử lý phía Server;
- các DTO/message trong module `common` của tính năng đó;
- màn hình/popup và logic hiển thị phía Client JavaFX (`client-fx`) của tính năng đó;
- test tương ứng.

Nhờ vậy mỗi phần đều có đủ: giao tiếp mạng hai chiều, biểu đồ sequence Client–Server và giao diện minh họa, đồng thời khối lượng giữa các phần cân bằng hơn.

**Client chính là `client-fx` (JavaFX).** Module `client` (libGDX) là bản cũ giữ để tương thích, **không đưa vào phạm vi phần cá nhân**; nếu nhắc đến thì chỉ ghi là client tương thích ngược.

## 0.2 Bảng phân công chi tiết theo source

Đường dẫn rút gọn: `server/…` = `server/src/main/java/com/bomberman/server/`, `fx/…` = `client-fx/src/main/java/com/bomberman/clientfx/`, `common/…` = `common/src/main/java/com/bomberman/common/`.

| Phần | Server | Common (giao thức) | Client JavaFX | Message sở hữu |
|---|---|---|---|---|
| **A. Kết nối và xác thực** | `server/network/*` (`TcpGameServer`, `ClientSession`, `ConnectionManager`, `SessionWriter`, `MessageDispatcher`), `server/auth/*`, `server/user/*` (`OnlineUserRegistry`, `User`), `server/config/*`, `repository/UserRepository` | `common/message/*` (`NetworkMessage`, `MessageEncoder`, `MessageDecoder`, `ProtocolException`), `enums/MessageType`, `enums/AuthResultCode`, DTO `LoginRequest/Response`, `RegisterRequest/Response`, `ErrorResponse` | `fx/network/GameNetworkClient`, `ServerListener`, `PendingRequests`, `ClientNetworkConfig`, `fx/ui/screen/LoginScreen`, `fx/ui/popup/ServerAddressPopup`, `fx/ui/InputValidation`, `fx/ui/component/PasswordInput` | `REGISTER_*`, `LOGIN_*`, `LOGOUT`, `PING`/`PONG`, `ERROR` |
| **B. Sảnh và phòng chờ** | `server/lobby/*` (`LobbyService`, `LobbyMessageHandler`), `server/room/*` (`RoomManager`, `GameRoom`, `RoomMessageHandler`, `RoomDtoMapper`…) | `enums/RoomStatus`, `RoomResultCode`, `PlayerStatus`, DTO `OnlineUser*`, `RoomListUpdate`, `RoomSummaryDto`, `RoomStateDto/Update`, `RoomPlayerDto`, `CreateRoomRequest`, `JoinRoomRequest`, `ReadyRequest` | `fx/ui/screen/HomeScreen`, `RoomBrowserScreen`, `RoomLobbyScreen`, `fx/game/QuickPlay`, `LobbyStatus`, `MapPreview`, component `RoomCard`, `SlotCard`, `ArenaPreview`, `PlayerBadge`, `StatusDot`, `ChecklistItem` | `ONLINE_USERS_*`, `ROOM_LIST_*`, `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `ROOM_STATE`, `READY`, `START_GAME`, `PLAY_AGAIN` |
| **C. Game Engine và đồng bộ thời gian thực** | `server/game/*` (`BombermanGame`, `GameMap`, `Bomb`, `Explosion`, `BomberPlayer`, `RoomGameLoop`, `GameSessionManager`, `GameMessageHandler`, `GameStateMapper`, các `*GameCommand`) | `enums/Direction`, `TileType`, `GameStatus`, `GameCommandResultCode`, DTO `MoveRequest`, `GameStateDto`, `GamePlayerStateDto`, `BombStateDto`, `ExplosionStateDto`, `PositionDto` | `fx/ui/screen/GameScreen`, `fx/game/GameRenderer`, `InputController`, `BoardLayout`, `FlameClassifier`, `PlayerVisual`, `fx/game/fx/ParticlePool`, `fx/ui/popup/MatchMenuPopup`, component `HudPlayerCard` | `MOVE`, `PLACE_BOMB`, `GAME_STATE`, `GAME_OVER` (phía phát) |
| **D. Kiến trúc Desktop Client và dữ liệu trận đấu** | `server/match/*` (`Match`, `MatchPlayer`, `MatchScoring`, `MatchPersistenceService`, `MatchHistoryService`, `HistoryMessageHandler`), `server/ranking/*`, `repository/MatchRepository`, `MatchPlayerRepository`; CSDL (`docker-compose.yml`, `application.properties`) | `enums/GameResult`, DTO `GameOverDto`, `GameOverPlayerDto`, `HistoryResponse`, `MatchHistory*Dto`, `RankingResponse`, `RankingEntryDto` | Khung ứng dụng: `fx/BombermanApp`, `Launcher`, `fx/network/ClientMessageDispatcher`, `GameClientController`, `fx/state/*`, `fx/ui/AppShell`, `Navigator`, `ScreenNavigator`, `fx/ui/theme/*`, `fx/asset/*`, `fx/ui/popup/Popups`, `SettingsPopup`, `HelpPopup`; dữ liệu trận: `ResultScreen`, `HistoryScreen`, `LeaderboardScreen`, `fx/game/MatchTracker`, `GameFormats`, component `RankRow`; đóng gói (`packaging/`, task `packageApp`) | `GAME_OVER` (phía lưu/hiển thị), `HISTORY_*`, `RANKING_*` |

## 0.3 Luồng liên phần (ghi rõ điểm giao, không tính trùng)

- **Mất kết nối:** A phát hiện (read loop kết thúc) và gọi `AuthMessageHandler.cleanupSession` → B dọn phòng qua `RoomMessageHandler.handleSessionExit` → C xử lý người chơi rời trận (`DisconnectGameCommand`). Mỗi phần chỉ mô tả đoạn của mình và dẫn chiếu sang phần kia.
- **Bắt đầu trận:** B kiểm tra điều kiện `START_GAME` trong `RoomManager` → C tạo `RoomGameLoop` trong `GameSessionManager`.
- **Kết thúc trận:** C xác định kết quả và broadcast `GAME_OVER` → D lưu trận qua `MatchPersistenceService.recordCompletedMatch` và hiển thị `ResultScreen`.
- **Trạng thái online:** A sở hữu `OnlineUserRegistry` (online/offline); B và C chỉ gọi `updateStatus` để đổi `FREE`/`IN_ROOM`/`PLAYING`.
- **Client:** A sở hữu tầng vận chuyển (socket, read loop, ghép request–response); D sở hữu tầng ứng dụng (`ClientMessageDispatcher` áp message vào `ClientState`, `GameClientController` là facade gửi lệnh, điều hướng màn hình). Màn hình của từng tính năng thuộc phần sở hữu tính năng đó.

---

# 1. YÊU CẦU CỦA ĐỀ BÀI

Phần cá nhân phải đáp ứng:

- Mô tả bằng **biểu đồ hoặc giao diện** nội dung cá nhân thực hiện.
- Ánh xạ **hình vẽ / giao diện / biểu đồ** với **chức năng cá nhân thực hiện** dưới dạng bảng.

Do đó, với mỗi module cá nhân phải thể hiện được:

1. Phạm vi chức năng.
2. Các class/file/method liên quan.
3. Luồng xử lý.
4. Giao tiếp mạng.
5. Request/response/message.
6. Biểu đồ phù hợp.
7. Giao diện phù hợp nếu có.
8. Bảng ánh xạ hình ↔ chức năng.
9. Bằng chứng từ source code.

---

# 2. NGUYÊN TẮC CHUNG

- Đọc source trước khi viết.
- Không dựa riêng vào bản đăng ký.
- Bản đăng ký chỉ dùng để xác định phạm vi dự kiến.
- Nếu source thực tế khác bản phân công, ghi rõ.
- Không tự bịa chức năng.
- Không tự bịa message/API/port/class/method.
- Không tự tạo screenshot giả.
- Không tự ghi test PASS nếu chưa chạy.
- Nếu thiếu bằng chứng: `[CẦN XÁC NHẬN]`.

Mỗi nội dung quan trọng phải có:

```text
Source:
- File:
- Class:
- Method:
- Vai trò:
```

---

# 3. PHẦN A – KẾT NỐI VÀ XÁC THỰC

Phạm vi dự kiến:

- quản lý kết nối TCP;
- phiên làm việc;
- trao đổi dữ liệu xác thực Client–Server;
- nền tảng kết nối nhiều Client với Server;
- quản lý phiên người dùng;
- chức năng xác thực cơ bản;
- **giao thức truyền tin trong `common`**: cấu trúc `NetworkMessage`, đóng khung (framing) và mã hóa/giải mã qua `MessageEncoder`/`MessageDecoder`, `requestId`;
- **heartbeat** `PING`/`PONG` và thông điệp lỗi `ERROR`;
- **registry trạng thái online** (`OnlineUserRegistry`);
- **tầng vận chuyển phía Client**: `GameNetworkClient` (socket + read loop trên virtual thread), `PendingRequests` (ghép phản hồi theo `requestId`, timeout), `ClientNetworkConfig`;
- **giao diện**: màn hình đăng nhập/đăng ký (`LoginScreen`), chọn địa chỉ Server (`ServerAddressPopup`), kiểm tra input phía Client (`InputValidation`).

## 3.1 Xác định source

Tìm và lập bảng:

| Chức năng | File | Class | Method | Network liên quan |
|---|---|---|---|---|

Kiểm tra tối thiểu:

- Server socket;
- bind/listen;
- accept;
- Client socket;
- connect;
- connection handler;
- session;
- login;
- logout;
- authentication;
- disconnect;
- mapping connection ↔ user;
- framing/encode/decode message;
- ghép request–response bằng `requestId`;
- ghi đồng thời an toàn (`SessionWriter`).

## 3.2 Phân tích luồng kết nối

Mô tả:

```text
Client start
→ tạo socket
→ connect Server
→ Server accept
→ tạo handler/session
→ sẵn sàng trao đổi dữ liệu
```

Phải sửa theo source thật.

## 3.3 Phân tích luồng đăng nhập

Xác định:

- dữ liệu login;
- message;
- cách gửi;
- server handler;
- validate;
- database;
- response;
- session;
- trạng thái online.

## 3.4 Xử lý mất kết nối

Phân tích:

- Server phát hiện disconnect bằng cách nào;
- cleanup session;
- cập nhật online state;
- nếu đang ở room/game thì xử lý thế nào;
- thông báo tới Client khác.

## 3.5 Biểu đồ bắt buộc đề xuất

- Sequence Diagram kết nối TCP.
- Sơ đồ cấu trúc một frame message (framing) và luồng encode/decode.
- Sequence Diagram đăng ký/đăng nhập.
- Activity/Flow Diagram xử lý disconnect (dừng ở điểm gọi sang phần B/C).

## 3.6 Giao diện

Login UI thuộc module này:
- screenshot màn hình đăng nhập/đăng ký;
- popup chọn địa chỉ Server;
- trạng thái login thành công;
- trạng thái login lỗi (sai mật khẩu, tài khoản đang online, không kết nối được Server).

---

# 4. PHẦN B – SẢNH VÀ PHÒNG CHỜ

Phạm vi dự kiến:

- đồng bộ trạng thái người chơi;
- quản lý danh sách người chơi online;
- danh sách phòng;
- tạo phòng;
- tham gia phòng;
- rời phòng;
- ready;
- start game (kiểm tra điều kiện, chuyển giao cho phần C);
- play again;
- dọn phòng khi người chơi rời/mất kết nối;
- đồng bộ thay đổi của phòng tới các Client liên quan;
- **giao diện**: `HomeScreen` (Quick Play), `RoomBrowserScreen`, `RoomLobbyScreen`, xem trước bản đồ (`MapPreview`), checklist điều kiện bắt đầu (`LobbyStatus`).

## 4.1 Xác định source

| Chức năng | File | Class | Method | Message |
|---|---|---|---|---|

Kiểm tra:

- lobby;
- online users;
- room list;
- create room;
- join room;
- leave room;
- room owner;
- ready;
- start condition;
- broadcast room state.

## 4.2 Phân tích sảnh

Mô tả:

```text
Client login thành công
→ yêu cầu dữ liệu sảnh
→ Server lấy trạng thái
→ Server gửi danh sách
→ Client render
```

## 4.3 Phân tích tạo/tham gia phòng

Làm rõ:

- điều kiện tạo;
- room ID/name;
- giới hạn 4 người;
- trạng thái room;
- cập nhật danh sách phòng;
- broadcast tới Client khác.

## 4.4 Phân tích ready/start

Làm rõ:

- thay đổi ready;
- điều kiện 2–4 người;
- kiểm tra tất cả ready;
- quyền chủ phòng;
- chuyển trạng thái sang game.

## 4.5 Biểu đồ

- Sequence Diagram tải sảnh.
- Sequence Diagram tạo phòng.
- Sequence Diagram tham gia/rời phòng.
- Sequence Diagram ready/start.

## 4.6 Giao diện

Đề xuất screenshot thật:

- màn hình Home (nút Quick Play);
- sảnh;
- danh sách online;
- danh sách phòng;
- tạo phòng;
- phòng chờ;
- trạng thái ready.

---

# 5. PHẦN C – GAME ENGINE VÀ ĐỒNG BỘ THỜI GIAN THỰC

Phạm vi dự kiến:

- nhận dữ liệu điều khiển từ Client;
- xử lý toàn bộ logic Bomberman phía Server;
- Server authoritative;
- xử lý di chuyển;
- đặt bom;
- nổ bom;
- va chạm;
- phá tường;
- loại người chơi;
- xác định thắng/hòa;
- broadcast game state;
- quản lý nhiều trận/phòng đồng thời;
- xử lý người chơi mất kết nối giữa trận;
- gọi lưu kết quả khi hết trận (phần lưu thuộc phần D);
- **phía Client**: thu input bàn phím (`InputController`), gửi `MOVE`/`PLACE_BOMB`, render snapshot nhận được (`GameRenderer`, `BoardLayout`, `FlameClassifier`), nội suy vị trí hiển thị (`PlayerVisual`), HUD và menu ESC trong `GameScreen`.

## 5.1 Xác định source Game Engine

| Thành phần | File | Class | Method | Vai trò |
|---|---|---|---|---|

Tìm:

- Game;
- GameEngine;
- GameLoop;
- PlayerState;
- Map;
- Bomb;
- Explosion;
- Collision;
- Match;
- Room/Game binding;
- scheduler/timer/thread;
- broadcast state.

## 5.2 Di chuyển

Phân tích:

```text
Key input
→ Client gửi MOVE
→ Server nhận
→ validate vị trí
→ update PlayerState
→ broadcast state
```

Phải theo source thật.

## 5.3 Đặt bom

Phân tích:

- điều kiện đặt bom;
- số bom giới hạn;
- tọa độ bom;
- timer;
- owner;
- lưu bomb state;
- broadcast.

## 5.4 Bom phát nổ

Làm rõ:

- thời gian dự kiến;
- scheduler/timer thực tế;
- phạm vi 4 hướng;
- tường cứng;
- tường phá được;
- player hit;
- chain explosion nếu source có.

## 5.5 Kết thúc trận

Làm rõ:

- alive players;
- thắng;
- hòa;
- match result;
- broadcast;
- persistence;
- chuyển về trạng thái sau trận.

## 5.6 Đồng bộ thời gian thực

Phân tích:

- tần suất update;
- event-based hay tick-based;
- message state;
- full state hay delta;
- broadcast theo room;
- thread safety;
- concurrent rooms.

## 5.7 Biểu đồ

Ưu tiên:

- Sequence Diagram MOVE.
- Sequence Diagram PLACE_BOMB.
- Activity Diagram xử lý bomb explosion.
- Sequence Diagram broadcast game state.
- Sequence Diagram phía Client: phím → `InputController` → `MOVE` → nhận `GAME_STATE` → `GameRenderer` vẽ khung hình.
- Flowchart xác định kết quả.
- Component Diagram Game Engine nếu hữu ích.

## 5.8 Giao diện

Ảnh cần chụp:

- màn hình trận đấu (HUD người chơi);
- người chơi di chuyển;
- bom đã đặt;
- vụ nổ;
- người chơi bị loại;
- menu ESC trong trận.

Màn hình kết quả thuộc phần D.

---

# 6. PHẦN D – KIẾN TRÚC DESKTOP CLIENT VÀ DỮ LIỆU TRẬN ĐẤU

Phạm vi dự kiến:

- khung ứng dụng JavaFX: khởi tạo (`BombermanApp`, `Launcher`), khung cửa sổ (`AppShell`), điều hướng màn hình (`Navigator`, `ScreenNavigator`);
- tầng ứng dụng của Client: `GameClientController` (facade tạo và gửi mọi lệnh), `ClientMessageDispatcher` (áp message nhận được vào `ClientState`, chuyển màn hình), `ClientState`/`ClientStateListener`;
- mô hình luồng Client: network thread → JavaFX Application Thread;
- design system và tài nguyên (`ui/theme/*`, `asset/*`), popup dùng chung, cài đặt người dùng (`UserPreferences`);
- **phía Server**: lưu trận đấu và cập nhật điểm (`MatchPersistenceService`, `MatchScoring`), truy vấn lịch sử (`MatchHistoryService`, `HistoryMessageHandler`), bảng xếp hạng (`RankingService`, `RankingMessageHandler`), repository JPA và CSDL;
- **giao diện dữ liệu trận**: `ResultScreen`, `HistoryScreen`, `LeaderboardScreen`, `MatchTracker` (dữ liệu cho bảng kết quả);
- đóng gói ứng dụng desktop (`packageApp`).

## 6.1 Xác định cấu trúc Client

| Thành phần | File | Class | Method | Vai trò |
|---|---|---|---|---|

Tìm:

- Client entry point;
- facade gửi lệnh;
- message dispatcher;
- state model và listener;
- chuyển sang UI thread;
- navigation;
- result/history/ranking screen.

Socket, read loop và `PendingRequests` thuộc phần A; chỉ dẫn chiếu, không phân tích lại.

## 6.2 Gửi dữ liệu

Phân tích:

- UI event → method của `GameClientController`;
- tạo `NetworkMessage` và `requestId`;
- `CompletableFuture` nhận phản hồi;
- chuyển xuống tầng vận chuyển (phần A).

## 6.3 Nhận dữ liệu

Phân tích:

- message type → nhánh xử lý trong `ClientMessageDispatcher`;
- riêng `GAME_STATE` được xử lý khác các message còn lại (xác nhận trong source);
- update `ClientState`;
- `ClientStateListener` cập nhật UI;
- UI thread (`Platform.runLater` nếu có — xác nhận trong source).

## 6.4 Lưu trận đấu và tính điểm (Server)

Phân tích:

- điểm gọi từ `GameSessionManager` (phần C) khi hết trận;
- `@Transactional recordCompletedMatch`;
- quy tắc điểm trong `MatchScoring`;
- entity `Match`, `MatchPlayer`, bộ đếm trong `User`;
- CSDL và cấu hình.

## 6.5 Kết quả, lịch sử, bảng xếp hạng

Phân tích:

- `GAME_OVER` → `ResultScreen`;
- `HISTORY_REQUEST` → `HISTORY_RESPONSE`;
- `RANKING_REQUEST` → `RANKING_RESPONSE`;
- data model (DTO);
- UI;
- refresh.

## 6.6 Biểu đồ

- Component Diagram kiến trúc Client (tầng vận chuyển – tầng ứng dụng – state – UI).
- Flow Diagram message dispatch.
- UI navigation diagram.
- Sequence Diagram lưu trận khi `GAME_OVER`.
- Sequence Diagram tải lịch sử/bảng xếp hạng.
- ERD bảng `users`, `matches`, `match_players` (theo entity thật).

## 6.7 Giao diện

Ảnh cần chụp:

- result (VICTORY/DEFEAT/DRAW nếu có);
- history;
- ranking/leaderboard;
- popup cài đặt, trợ giúp.

---

# 7. CẤU TRÚC CHI TIẾT CHO MỖI PHẦN CÁ NHÂN

Mỗi phần A/B/C/D phải theo cùng format:

## X.1 Phạm vi chức năng

Viết đoạn văn mô tả phạm vi.

## X.2 Các chức năng đã triển khai

| ID | Chức năng | Source | Trạng thái xác minh |
|---|---|---|---|

## X.3 Thiết kế chức năng

Mô tả:
- mục đích;
- input;
- output;
- thành phần;
- network interaction.

## X.4 Luồng xử lý

Mô tả từng bước.

## X.5 Source Mapping

| Bước | File | Class | Method | Nội dung |
|---|---|---|---|---|

## X.6 Biểu đồ

Tạo Mermaid phù hợp.

## X.7 Giao diện minh họa

Nếu có UI:
- mô tả;
- source;
- screenshot cần chụp.

## X.8 Kết quả và xử lý lỗi

## X.9 Bảng ánh xạ hình ↔ chức năng

| STT | Mã hình | Hình/Giao diện/Biểu đồ | Chức năng tương ứng | Source |
|---|---|---|---|---|

---

# 8. BẢNG ÁNH XẠ HÌNH ↔ CHỨC NĂNG

Mỗi chương cá nhân có **một** bảng ánh xạ ở mục ngay trước Kết chương:

| Mã hình | Hình vẽ / giao diện | Chức năng được minh họa | Thành phần cài đặt |
|---|---|---|---|

Bảng được sinh tự động từ các hình đã chèn trong chương (`FigureLog` trong `report_common.py`). Không tạo bảng ánh xạ tổng hợp và không tạo chương tổng hợp riêng.

---

# 9. PHẦN CODE QUAN TRỌNG

Không copy toàn bộ source.

Lập bảng:

| Code-ID | Phần | File | Method | Ý nghĩa | Có nên chèn báo cáo |
|---|---|---|---|---|---|

Ưu tiên:
- (A) socket connect/listen, accept, session, encode/decode frame, login handler, Client read loop;
- (B) room broadcast, điều kiện start game;
- (C) move handler, game loop tick, explosion logic, game state broadcast, Client input/render;
- (D) Client message dispatch → UI thread, lưu trận `@Transactional`, ranking query.

---

# 10. KIỂM THỬ

Theo quyết định của nhóm, báo cáo **không** có phần kiểm thử: không viết mục, bảng hay chương kiểm thử, không liệt kê test class.

---

# 11. CẤU TRÚC CÁC CHƯƠNG CÁ NHÂN TRONG BÁO CÁO NHÓM

Các chương cá nhân nối tiếp Chương 2 của báo cáo nhóm, trước KẾT LUẬN. Số chương do style `CTDT-H1` tự đánh; số mục do `CTDT-H2/H3` tự đánh; số hình, số bảng ghi tay theo chương (`Hình 3.1.`, `Bảng 3.1.`).

```text
[Trang bìa]
BẢNG PHÂN CÔNG NHIỆM VỤ        (không caption)
MỤC LỤC / DANH MỤC HÌNH / DANH MỤC BẢNG / MỞ ĐẦU
CHƯƠNG 1, CHƯƠNG 2             (phần nhóm)
CHƯƠNG 3. KẾT NỐI VÀ XÁC THỰC
CHƯƠNG 4. SẢNH VÀ PHÒNG CHỜ
CHƯƠNG 5. GAME ENGINE VÀ ĐỒNG BỘ THỜI GIAN THỰC
CHƯƠNG 6. KIẾN TRÚC DESKTOP CLIENT VÀ DỮ LIỆU TRẬN ĐẤU
KẾT LUẬN / TÀI LIỆU THAM KHẢO
```

Khung chung của mỗi chương cá nhân (N = 3..6):

```text
N.1  Phạm vi chức năng               - đoạn văn + Bảng N.1 các chức năng đã triển khai
N.2 … N.k  Các mục kỹ thuật          - thiết kế, luồng xử lý, thông điệp, biểu đồ, đoạn mã ngắn
N.(k+1)  Giao diện minh họa          - ảnh chụp thật từ ứng dụng
N.(k+2)  Ánh xạ hình vẽ với chức năng
N.(k+3)  Kết chương
```

Các mục kỹ thuật đã dùng:

- **Chương 3:** Giao thức truyền tin (cấu trúc khung, mã hóa/giải mã, thông điệp) · Tổ chức kết nối phía Server · Tổ chức kết nối phía Client · Đăng ký và đăng nhập · Kiểm tra kết nối, đăng xuất và mất kết nối.
- **Chương 4:** Mô hình dữ liệu và trạng thái · Đồng bộ sảnh · Tạo, tham gia và rời phòng (chơi nhanh, dọn phòng khi mất kết nối) · Sẵn sàng, bắt đầu trận và chơi lại.
- **Chương 5:** Thiết kế Game Engine · Tiếp nhận thao tác người chơi · Vòng lặp xử lý và đồng bộ trạng thái · Di chuyển, đặt bom và nổ bom · Kết thúc trận và mất kết nối giữa trận · Thu nhận thao tác và hiển thị phía Client.
- **Chương 6:** Kiến trúc Desktop Client · Xử lý thông điệp và cập nhật giao diện · Điều hướng màn hình · Lưu kết quả trận đấu và tính điểm · Kết quả, lịch sử và bảng xếp hạng.

Điểm giao giữa các chương (mục 0.3) được viết một lần ở chương sở hữu và dẫn chiếu bằng số mục, ví dụ "mục 4.4", "mục 5.6".

---

# 12. PHONG CÁCH VIẾT

Phải viết theo phong cách báo cáo học thuật môn Lập trình mạng.

Không viết:

> Người dùng bấm nút rồi Client gửi lên.

Nên viết:

> Khi người dùng thực hiện thao tác trên giao diện, Client tạo thông điệp tương ứng và truyền tới Server qua kết nối TCP đang được duy trì. Server tiếp nhận thông điệp, kiểm tra tính hợp lệ, cập nhật trạng thái hệ thống và gửi dữ liệu phản hồi tới các Client liên quan.

Không dùng quá nhiều văn nói hoặc mô tả UI đơn thuần.

Phải luôn làm rõ:
- thành phần nào gửi;
- thành phần nào nhận;
- dữ liệu gì;
- xử lý ở đâu;
- trạng thái thay đổi như thế nào.

---

# 13. FILE KẾT QUẢ

Báo cáo được sinh bằng script, không sửa tay file Word:

| File | Nội dung |
|---|---|
| `docs/bao-cao/tools/report_common.py` | Style, hàm chèn đoạn văn, bảng, hình, đoạn mã, ảnh giao diện, `FigureLog` |
| `docs/bao-cao/tools/report_diagrams.py` | Vẽ sơ đồ tuần tự và sơ đồ luồng trắng đen cùng phong cách Chương 2 |
| `docs/bao-cao/tools/build_group_report.py` | Trang bìa, bảng phân công, Chương 1–2, gọi Chương 3–6, Kết luận |
| `docs/bao-cao/tools/chapter_3_ket_noi_xac_thuc.py` | Chương 3 (Phần A) |
| `docs/bao-cao/tools/chapter_4_sanh_phong_cho.py` | Chương 4 (Phần B) |
| `docs/bao-cao/tools/chapter_5_game_engine.py` | Chương 5 (Phần C) |
| `docs/bao-cao/tools/chapter_6_client_du_lieu_tran.py` | Chương 6 (Phần D) |
| `docs/bao-cao/screenshots/*.png` | Ảnh chụp giao diện thật; thiếu ảnh thì script chèn khung giữ chỗ và in danh sách |

Lệnh dựng: `python docs\bao-cao\tools\build_group_report.py` (đóng file Word trước, hoặc dùng `--output <đường dẫn khác>`). Sau khi dựng, mở bằng Word và cập nhật trường (F9) hoặc chạy `export_docx_pdf.ps1 -SaveUpdatedFields` để làm mới mục lục, danh mục hình và bảng.

## 13.1 Bản final (đã chỉnh tay trong Word)

Nhóm đã sửa trực tiếp bản Word thành `bao_cao_nhom_bomberman-final.docx` (văn phong mô tả chức năng, bỏ tên lớp và đoạn mã, rút gọn bảng). Bản này có bộ script riêng; bộ script chi tiết ở trên được giữ nguyên.

| File | Nội dung |
|---|---|
| `docs/bao-cao/tools/docx_to_python.py` | Đọc file Word đã chỉnh tay và sinh lại các module trong `tools/final/`; tách ảnh chèn tay ra `docs/bao-cao/images/` |
| `docs/bao-cao/tools/final/*.py` | Nội dung bản final: `phan_cong`, `mo_dau_chuong_1_2`, `chuong_3_…` đến `chuong_6_…`, `ket_luan` |
| `docs/bao-cao/tools/functional_diagrams.py` | Hình Chương 3–6 ở mức phân tích chức năng theo `update.md` (phân tích từng hình: `ve-lai-hinh-muc-chuc-nang.md`) |
| `docs/bao-cao/tools/build_final_report.py` | Vẽ lại sơ đồ (Chương 2 và các hình chức năng), dựng bản final ra `bao_cao_nhom_bomberman-final-build.docx` |

Quy trình:

1. Sửa nội dung trong `tools/final/*.py` rồi chạy `python docs\bao-cao\tools\build_final_report.py`.
2. Nếu lại sửa trong Word, chạy `python docs\bao-cao\tools\docx_to_python.py --input <file.docx>` để đồng bộ về Python. Lệnh này ghi đè `tools/final/*.py`, kể cả các chỉnh sửa tay ghi ở đầu mỗi file.

