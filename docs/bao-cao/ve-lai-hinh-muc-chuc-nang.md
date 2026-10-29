# Vẽ lại hình Chương 3–6 ở mức phân tích chức năng

Kết quả xử lý yêu cầu trong [update.md](update.md). Các hình mới do `tools/functional_diagrams.py` sinh ra (đen trắng, nền trắng, chữ khoảng 8 pt khi chèn khổ A4) và đã thay vào bản final (`tools/final/chuong_*.py`, dựng bằng `tools/build_final_report.py`).

Nguyên tắc áp dụng cho mọi hình:

- **Giữ:** tác nhân (Người dùng, Client, Server, CSDL), thành phần chức năng ở mức khái niệm, trạng thái chính, yêu cầu/phản hồi, nhánh điều kiện quan trọng.
- **Bỏ:** tên lớp, tên hàm, tên biến, luồng xử lý (thread), khóa, cấu trúc dữ liệu nội bộ, ngoại lệ, annotation, tên package.
- Mỗi hình 5–10 bước; tên hình và chú thích ngắn nằm ngay trong hình.
- Không thêm chức năng, không đổi logic nghiệp vụ: mọi bước đều đối chiếu với nội dung chương và mã nguồn hiện tại.

---

## Chương 3 – Kết nối và xác thực

### Hình 3.1 – Cấu trúc khung thông điệp và quá trình gửi, nhận
- **Hình cũ quá chi tiết:** ghi `MessageEncoder.encode()`, `MessageDecoder.decode()`, Jackson, `writeInt()`, `readFully()`, `ProtocolException`.
- **Giữ:** khung [Độ dài + Nội dung JSON]; bốn bước bên gửi; bốn bước bên nhận; kết nối TCP ở giữa.
- **Bỏ:** tên lớp mã hóa/giải mã, tên hàm đọc/ghi, thư viện JSON, ngoại lệ.
- **Hình mới:** [chuc-nang-3-1-khung-thong-diep.png](generated-report-assets/chuc-nang-3-1-khung-thong-diep.png)

### Hình 3.2 – Mô hình xử lý kết nối phía Server
- **Hình cũ quá chi tiết:** luồng acceptor `tcp-game-acceptor`, virtual thread, `ConnectionManager`/`ConcurrentHashMap`, `SessionWriter`/`ReentrantLock`, tên các Handler.
- **Giữ:** nhiều Client; tiếp nhận kết nối TCP; mỗi Client một phiên; xác định loại yêu cầu; 5 nhóm chức năng (xác thực, sảnh, phòng, trò chơi, lịch sử/xếp hạng); ghi chú các phiên chạy đồng thời.
- **Bỏ:** tên luồng, khóa, cấu trúc dữ liệu, tên lớp.
- **Hình mới:** [chuc-nang-3-2-xu-ly-ket-noi-server.png](generated-report-assets/chuc-nang-3-2-xu-ly-ket-noi-server.png)

### Hình 3.3 – Trình tự kết nối và nhận phản hồi
- **Hình cũ quá chi tiết:** 6 thành phần gồm `GameClientController`, `PendingRequests`, `GameNetworkClient`, `TcpGameServer`, `ClientSession`; nhắc `CompletableFuture`, `TCP_NODELAY`, luồng `client-network-writer`, `TimeoutException`.
- **Giữ:** Người dùng – Client – Server; mở kết nối lần đầu; yêu cầu kèm mã yêu cầu; phản hồi cùng mã; cập nhật giao diện; nhánh Server không phản hồi.
- **Bỏ:** future, thread, timeout exception, tên lớp.
- **Hình mới:** [chuc-nang-3-3-ket-noi-phan-hoi.png](generated-report-assets/chuc-nang-3-3-ket-noi-phan-hoi.png)

### Hình 3.4 – Trình tự đăng nhập
- **Hình cũ quá chi tiết:** `LoginScreen`, `AuthMessageHandler`, `AuthenticationService`, `OnlineUserRegistry`, `markOnline()`, `attachAuthenticatedUser()`, BCrypt.
- **Giữ:** Người dùng – Client – Server – CSDL; kiểm tra tài khoản; kiểm tra trạng thái trực tuyến; tạo phiên người dùng; trả kết quả; nhánh lỗi.
- **Bỏ:** tên lớp, tên hàm, thuật toán băm.
- **Hình mới:** [chuc-nang-3-4-dang-nhap.png](generated-report-assets/chuc-nang-3-4-dang-nhap.png)

### Hình 3.5 – Luồng xử lý mất kết nối
- **Hình cũ quá chi tiết:** EOF/`SocketException`/`ProtocolException`, `ClientSession.close()`, `MessageDispatcher.onDisconnect()`, `handleSessionExit()`, `markOffline()`, `ConnectionManager.remove()`.
- **Giữ:** mất kết nối → Server phát hiện → phiên đã đăng nhập? → xác định người dùng → xử lý phòng/trận → xóa trạng thái trực tuyến → cập nhật Client liên quan.
- **Bỏ:** ngoại lệ, tên hàm, tên lớp.
- **Hình mới:** [chuc-nang-3-5-mat-ket-noi.png](generated-report-assets/chuc-nang-3-5-mat-ket-noi.png)

## Chương 4 – Sảnh và phòng chờ

### Hình 4.1 – Trạng thái người chơi và trạng thái phòng
- **Hình cũ quá chi tiết:** tiêu đề nhánh ghi `OnlineUserRegistry`, `GameRoom / RoomStatus`, `RoomManager`; có thêm trạng thái ngoại tuyến ngoài phạm vi yêu cầu.
- **Giữ:** FREE → IN_ROOM → PLAYING → IN_ROOM; WAITING → PLAYING → FINISHED → WAITING; sự kiện vào phòng, bắt đầu trận, kết thúc trận, chơi lại, rời phòng.
- **Bỏ:** tên lớp; trạng thái ngoại tuyến.
- **Hình mới:** [chuc-nang-4-1-trang-thai.png](generated-report-assets/chuc-nang-4-1-trang-thai.png)

### Hình 4.2 – Đồng bộ sảnh
- **Hình cũ quá chi tiết:** `AuthMessageHandler`, `LobbyMessageHandler`, `LobbyService`, `broadcastLobbyUpdates()`, `snapshotRooms()`, `requestId = null`.
- **Giữ:** Client yêu cầu danh sách → Server lấy danh sách → trả Client; khi sảnh thay đổi → Server chủ động gửi → các Client ở sảnh cập nhật.
- **Bỏ:** tên lớp, tên hàm, chi tiết trường thông điệp.
- **Hình mới:** [chuc-nang-4-2-dong-bo-sanh.png](generated-report-assets/chuc-nang-4-2-dong-bo-sanh.png)

### Hình 4.3 – Tạo phòng và tham gia phòng
- **Hình cũ quá chi tiết:** `RoomMessageHandler`, `RoomManager`, `OnlineUserRegistry`, `LobbyService`, `createRoom()`, `joinRoom()`, `updateStatus()`.
- **Giữ:** Client 1 tạo phòng; Client 2 xin tham gia; Server kiểm tra phòng (tồn tại, đang chờ, chưa đủ 4 người); gửi trạng thái phòng mới cho cả hai.
- **Bỏ:** tên lớp, tên hàm, mã UUID.
- **Hình mới:** [chuc-nang-4-3-tao-tham-gia-phong.png](generated-report-assets/chuc-nang-4-3-tao-tham-gia-phong.png)

### Hình 4.4 – Luồng chơi nhanh
- **Hình cũ quá chi tiết:** ba nút quyết định, liệt kê mã lỗi `ROOM_FULL`, `ROOM_NOT_WAITING`, `ROOM_NOT_FOUND`, vòng lặp thử lại.
- **Giữ:** chọn PLAY → tìm phòng phù hợp → có: tham gia; không: tạo phòng → vào phòng chờ; nhánh Server từ chối (thử phòng khác tối đa 2 phòng rồi tạo phòng).
- **Bỏ:** mã lỗi cụ thể.
- **Hình mới:** [chuc-nang-4-4-choi-nhanh.png](generated-report-assets/chuc-nang-4-4-choi-nhanh.png)

### Hình 4.5 – Sẵn sàng và bắt đầu trận
- **Hình cũ quá chi tiết:** `RoomManager.setReady()`, `startGame()`, `GameSessionManager`, `LobbyStatus`, `beginGame()`.
- **Giữ:** READY → cập nhật trạng thái → chủ phòng START → kiểm tra điều kiện (chủ phòng, ≥ 2 người, tất cả sẵn sàng) → PLAYING, khởi tạo trận → các Client sang màn hình GAME.
- **Bỏ:** tên lớp, tên hàm.
- **Hình mới:** [chuc-nang-4-5-san-sang-bat-dau.png](generated-report-assets/chuc-nang-4-5-san-sang-bat-dau.png)

## Chương 5 – Game Engine và đồng bộ thời gian thực

### Hình 5.1 – Thành phần Game Engine
- **Hình cũ quá chi tiết:** `GameMessageHandler`, `GameSessionManager`, `RoomGameLoop`, `BombermanGame`, `stateLock`, `GameStateMapper`, hàng đợi 512 lệnh.
- **Giữ:** Client gửi thao tác → Server tiếp nhận → bộ xử lý trận của đúng phòng; trạng thái trận gồm bản đồ, người chơi, bom, vụ nổ; Server gửi trạng thái mới; hai phòng A, B để thể hiện mỗi phòng có trạng thái riêng.
- **Bỏ:** tên lớp, khóa, kích thước hàng đợi.
- **Hình mới:** [chuc-nang-5-1-thanh-phan-game-engine.png](generated-report-assets/chuc-nang-5-1-thanh-phan-game-engine.png)

### Hình 5.2 – Xử lý thao tác di chuyển
- **Hình cũ quá chi tiết:** `InputController`, `validateGameplaySession()`, `enqueueMove()`, `MoveGameCommand`, `movePlayer()`, `MoveResult`.
- **Giữ:** nhấn phím → gửi hướng → Server kiểm tra thao tác → kiểm tra ô đích → hợp lệ thì cập nhật vị trí → gửi trạng thái mới.
- **Bỏ:** hàng đợi lệnh, tên hàm, kiểu kết quả nội bộ.
- **Hình mới:** [chuc-nang-5-2-di-chuyen.png](generated-report-assets/chuc-nang-5-2-di-chuyen.png)

### Hình 5.3 – Chu kỳ xử lý trận
- **Hình cũ quá chi tiết (đã bị xóa khỏi bản final):** `runTickSafely()`, `drainCommands()`, `game.tick(now)`, `createSnapshot()`, "tick chẵn", "50 ms".
- **Giữ:** bắt đầu chu kỳ → tiếp nhận thao tác → xử lý di chuyển/đặt bom → cập nhật bom và vụ nổ → kiểm tra người bị loại → kiểm tra kết quả → gửi trạng thái mới → chu kỳ tiếp theo; nhánh có kết quả thì kết thúc trận.
- **Bỏ:** tên hàm, số mili giây, quy tắc gửi theo tick chẵn.
- **Hình mới:** [chuc-nang-5-3-chu-ky-xu-ly-tran.png](generated-report-assets/chuc-nang-5-3-chu-ky-xu-ly-tran.png) – đã đưa lại vào mục 5.4.

### Hình 5.4 – Đặt bom và phát nổ
- **Hình cũ quá chi tiết:** `PlaceBombGameCommand`, `placeBomb()`, `tick(now)`, `GameTickResult`, `remainingFuseMillis`.
- **Giữ:** yêu cầu đặt bom → kiểm tra → tạo bom → chờ thời gian nổ → phát nổ, tính vùng ảnh hưởng → cập nhật trạng thái → gửi tới Client.
- **Bỏ:** tên lớp lệnh, tên hàm, tên trường dữ liệu.
- **Hình mới:** [chuc-nang-5-4-dat-bom.png](generated-report-assets/chuc-nang-5-4-dat-bom.png)

### Hình 5.5 – Lan lửa và nổ dây chuyền
- **Hình cũ quá chi tiết:** thuật toán dạng vòng lặp với hàng đợi kích nổ, "trả lại lượt đặt bom cho chủ bom", ba nút quyết định lồng nhau.
- **Giữ:** bom nổ → lan bốn hướng → các nhánh: ô trống tiếp tục; tường cứng dừng; tường phá được phá và dừng; bom khác kích hoạt bom đó; người chơi bị loại.
- **Bỏ:** hàng đợi kích nổ, chi tiết đếm bom.
- **Hình mới:** [chuc-nang-5-5-lan-lua.png](generated-report-assets/chuc-nang-5-5-lan-lua.png)

### Hình 5.6 – Xác định kết quả trận
- **Hình cũ quá chi tiết:** `GameOutcome.win(userId)`, `GameOutcome.draw()`.
- **Giữ:** có người bị loại → kiểm tra số người còn sống → > 1 tiếp tục; = 1 người còn lại thắng; = 0 hòa → gửi kết quả → kết thúc trận.
- **Bỏ:** tên lớp, tên hàm.
- **Hình mới:** [chuc-nang-5-6-ket-qua-tran.png](generated-report-assets/chuc-nang-5-6-ket-qua-tran.png)

### Hình 5.7 – Xử lý hiển thị phía Client
- **Hình cũ quá chi tiết:** `AnimationTimer`, `MatchTracker`, `GameRenderer`, `PlayerVisual`, `InputController.update()`, "140 ms", "1,2 giây".
- **Giữ:** hai làn Client/Server: nhập bàn phím → gửi thao tác → Server xử lý → nhận trạng thái → cập nhật bản đồ, người chơi, bom, vụ nổ → hiển thị khung hình mới.
- **Bỏ:** tên lớp, thông số thời gian nội bộ.
- **Hình mới:** [chuc-nang-5-7-xu-ly-client.png](generated-report-assets/chuc-nang-5-7-xu-ly-client.png)

## Chương 6 – Desktop Client và dữ liệu trận đấu

### Hình 6.1 – Kiến trúc phân tầng Desktop Client
- **Hình cũ quá chi tiết:** `GameClientController`, `PendingRequests`, `ClientState` (AtomicReference), `ScreenNavigator`/`AppShell`, `ClientMessageDispatcher`, `GameNetworkClient`, tên luồng, các hàm `show()`, `complete()`.
- **Giữ:** bốn tầng Giao diện – Trạng thái Client – Xử lý ứng dụng – Kết nối mạng; chiều gửi Người dùng → Server; chiều nhận Server → Giao diện.
- **Bỏ:** tên lớp, tên luồng, cấu trúc dữ liệu.
- **Hình mới:** [chuc-nang-6-1-phan-tang-client.png](generated-report-assets/chuc-nang-6-1-phan-tang-client.png)

### Hình 6.2 – Phân phối thông điệp tới giao diện
- **Hình cũ quá chi tiết:** luồng `server-listener`, `AtomicReference`, `Platform.runLater()`, `switch` theo type, `PendingRequests.complete()`.
- **Giữ:** Server gửi → Client nhận → xác định loại dữ liệu → cập nhật trạng thái Client → cần chuyển màn hình? (điều hướng) → cập nhật giao diện.
- **Bỏ:** tên luồng, tên hàm, chi tiết đồng bộ luồng.
- **Hình mới:** [chuc-nang-6-2-phan-phoi-thong-diep.png](generated-report-assets/chuc-nang-6-2-phan-phoi-thong-diep.png)

### Hình 6.3 – Điều hướng màn hình
- **Hình cũ quá chi tiết:** nhãn là tên thông điệp (`LOGIN_RESPONSE`, `ROOM_STATE (member)`, `ROOM_STATE PLAYING`), tên màn hình theo hằng trong code (`ROOM_BROWSER`).
- **Giữ:** LOGIN → HOME → ROOM LIST / ROOM LOBBY → GAME → RESULT; HOME → HISTORY, LEADERBOARD; RESULT → HOME, PLAY AGAIN → ROOM LOBBY; mất kết nối → LOGIN.
- **Bỏ:** tên thông điệp; nhãn chuyển màn hình viết bằng sự kiện nghiệp vụ.
- **Hình mới:** [chuc-nang-6-3-dieu-huong-man-hinh.png](generated-report-assets/chuc-nang-6-3-dieu-huong-man-hinh.png)

### Hình 6.4 – Lưu kết quả trận đấu
- **Hình cũ quá chi tiết:** `RoomGameLoop`, `GameSessionManager`, `MatchPersistenceService`, `MatchScoring`, `recordCompletedMatch(...)`, `@Transactional`, `saveAndFlush()`, `user.recordMatch()`, `GAME_OVER {winnerUserId, matchResult, …}`.
- **Giữ:** Game Engine báo kết quả → tạo thông tin trận đấu → xác định kết quả cá nhân → lưu TRẬN ĐẤU → lưu CHI TIẾT TRẬN ĐẤU → cập nhật thống kê TÀI KHOẢN → gửi kết quả về Client; ghi chú các bước lưu cùng thành công hoặc cùng bị hủy.
- **Bỏ:** tên lớp, annotation, hàm lưu; không nhắc `winner_id` và `match_result` theo thiết kế CSDL hiện tại (kết quả WIN / LOSS / DRAW nằm ở kết quả cá nhân trong CHI TIẾT TRẬN ĐẤU).
- **Hình mới:** [chuc-nang-6-4-luu-ket-qua.png](generated-report-assets/chuc-nang-6-4-luu-ket-qua.png)

### Hình 6.5 – Tải lịch sử và bảng xếp hạng
- **Hình cũ quá chi tiết:** `RankingMessageHandler`, `RankingService.getRanking()`, `HistoryMessageHandler`, `MatchHistoryService.getHistory()`, `findMatchesByUserId()`; 6 thành phần trong một sơ đồ tuần tự 13 bước.
- **Giữ:** hai luồng song song: LỊCH SỬ (Client → Server xác định tài khoản → CSDL lấy các trận → Client hiển thị) và BẢNG XẾP HẠNG (Client → Server → CSDL thống kê tài khoản → sắp xếp → Client hiển thị).
- **Bỏ:** tên lớp, tên hàm truy vấn.
- **Hình mới:** [chuc-nang-6-5-lich-su-xep-hang.png](generated-report-assets/chuc-nang-6-5-lich-su-xep-hang.png)

---

## Thay đổi đi kèm trong bản final

- Tên hình (chú thích), văn bản thay thế của ảnh và cột "Hình vẽ / giao diện", "Chức năng được minh họa" trong bảng ánh xạ của Chương 3–6 được cập nhật theo tên hình mới.
- Hình 5.3 được đưa lại vào mục 5.4, kèm một câu dẫn "Hình 5.3 mô tả chu kỳ xử lý này."; số hình Chương 5 liên tục từ 5.1 đến 5.11.
- Nội dung chữ của các chương và các ảnh chụp giao diện không thay đổi.
