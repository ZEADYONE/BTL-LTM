
Hãy phân tích nội dung tài liệu và VẼ LẠI các biểu đồ/sơ đồ theo MỨC PHÂN TÍCH VÀ THIẾT KẾ CHỨC NĂNG, không đi sâu vào mức cài đặt source code.

Mục tiêu của các hình là phục vụ yêu cầu:

- Cá nhân mô tả nội dung mình thực hiện bằng biểu đồ hoặc giao diện.
- Ánh xạ hình vẽ với chức năng cá nhân thực hiện.
- Người đọc nhìn hình phải hiểu hệ thống hoặc chức năng hoạt động như thế nào mà không cần biết source code.

========================

1. NGUYÊN TẮC CHUNG
   ========================

Các sơ đồ chỉ được thể hiện:

- Actor hoặc người dùng.
- Client.
- Server.
- Cơ sở dữ liệu nếu có liên quan.
- Các thành phần chức năng ở mức khái niệm.
- Trạng thái chính.
- Luồng dữ liệu.
- Yêu cầu và phản hồi.
- Các bước xử lý nghiệp vụ chính.
- Quan hệ giữa các thành phần.
- Điều kiện hoặc nhánh xử lý quan trọng.

KHÔNG đưa vào sơ đồ:

- Tên class cụ thể.
- Tên method/hàm.
- Tên biến.
- Đoạn code.
- Exception.
- Annotation.
- Thread cụ thể.
- Virtual thread.
- Lock, mutex, AtomicReference.
- ConcurrentHashMap, LinkedHashMap, ArrayBlockingQueue.
- ScheduledExecutor.
- CompletableFuture.
- Platform.runLater().
- saveAndFlush().
- @Transactional.
- Tên package.
- Các cấu trúc dữ liệu nội bộ.
- Các chi tiết tối ưu implementation.

Ví dụ KHÔNG viết:
GameSessionManager.startGame()
RoomGameLoop.runTickSafely()
ClientMessageDispatcher.onMessage()
AuthenticationService.login()
OnlineUserRegistry.markOnline()
publishRoomState()
PendingRequests.complete()

Thay vào đó hãy viết ở mức chức năng như:
"Khởi tạo trận đấu"
"Xử lý trận đấu"
"Nhận thao tác"
"Kiểm tra yêu cầu"
"Cập nhật trạng thái"
"Đồng bộ trạng thái phòng"
"Xác thực người dùng"
"Cập nhật giao diện"
"Lưu kết quả trận đấu"

========================
2. MỨC ĐỘ CHI TIẾT
======================

Mỗi hình chỉ nên có khoảng 5–10 bước hoặc thành phần chính.

Nếu một sơ đồ hiện tại có quá nhiều bước kỹ thuật:
→ hãy gom chúng thành một bước chức năng lớn hơn.

Ví dụ:

Thay vì:

NetworkMessage
→ MessageEncoder
→ Jackson
→ byte[]
→ writeInt()
→ write()
→ socket

hãy biểu diễn:

Thông điệp
→ Đóng gói dữ liệu
→ Gửi qua TCP
→ Server nhận và giải mã

Thay vì:

server-listener
→ ClientMessageDispatcher
→ Platform.runLater()
→ ClientState
→ Listener
→ Screen

hãy biểu diễn:

Server gửi dữ liệu
→ Client nhận thông điệp
→ Cập nhật trạng thái Client
→ Cập nhật giao diện

Thay vì:

MoveGameCommand
→ ArrayBlockingQueue
→ RoomGameLoop
→ BombermanGame.movePlayer()

hãy biểu diễn:

Client gửi hướng di chuyển
→ Server kiểm tra
→ Game Engine xử lý
→ Cập nhật vị trí
→ Đồng bộ trạng thái tới các Client

========================
3. QUY ƯỚC VẼ
================

Phong cách:

- Đen trắng.
- Nền trắng.
- Đường nét rõ ràng.
- Phong cách sơ đồ kỹ thuật / UML tối giản.
- Không dùng hiệu ứng 3D.
- Không dùng icon trang trí không cần thiết.
- Không dùng màu nếu không thật sự cần.
- Font dễ đọc.
- Toàn bộ nội dung bằng tiếng Việt.
- Bố cục phù hợp để chèn vào báo cáo Word khổ A4.
- Ưu tiên chiều ngang nếu sơ đồ có nhiều thành phần.
- Không để chữ quá nhỏ.
- Không để đường nối chồng chéo.

Mỗi hình phải có:

- Tên hình.
- Các thành phần chính.
- Mũi tên rõ chiều xử lý.
- Chú thích ngắn gọn.
- Không có thông tin source code.

========================
4. VẼ LẠI CÁC HÌNH CHƯƠNG 3
=================================

CHƯƠNG 3 – KẾT NỐI VÀ XÁC THỰC

Hình 3.1 – Cấu trúc khung thông điệp và quá trình gửi/nhận

Chỉ cần thể hiện:

BÊN GỬI:
Thông điệp
→ Chuyển thành dữ liệu JSON
→ Đóng khung [Độ dài + Nội dung]
→ Gửi qua TCP

BÊN NHẬN:
Nhận dữ liệu TCP
→ Xác định độ dài
→ Đọc nội dung
→ Giải mã thông điệp

Không hiển thị tên encoder/decoder, Jackson, writeInt(), readFully(), ProtocolException.

Hình 3.2 – Mô hình xử lý kết nối phía Server

Thể hiện:

Nhiều Client
→ Server TCP
→ Quản lý phiên kết nối
→ Xác định loại yêu cầu
→ Chuyển tới chức năng phù hợp

Các chức năng:

- Xác thực
- Sảnh
- Phòng
- Trò chơi
- Lịch sử / xếp hạng

Phải thể hiện một Server có thể phục vụ nhiều Client đồng thời.

Hình 3.3 – Trình tự kết nối và nhận phản hồi

Actor:
Người dùng
Client
Server

Luồng:

Người dùng thực hiện thao tác
→ Client gửi yêu cầu
→ Server nhận và xử lý
→ Server gửi phản hồi
→ Client xác định phản hồi tương ứng
→ Cập nhật giao diện

Không mô tả Future, thread, timeout exception hoặc class.

Hình 3.4 – Trình tự đăng nhập

Người dùng
→ Client nhập tài khoản
→ Server nhận yêu cầu đăng nhập
→ Kiểm tra tài khoản
→ Kiểm tra trạng thái trực tuyến
→ Tạo phiên người dùng
→ Trả kết quả
→ Client chuyển sang màn hình chính

Hình 3.5 – Luồng xử lý mất kết nối

Mất kết nối
→ Server phát hiện
→ Xác định người dùng
→ Xử lý trạng thái phòng/trận
→ Xóa trạng thái trực tuyến
→ Cập nhật các Client liên quan

========================
5. VẼ LẠI CÁC HÌNH CHƯƠNG 4
=================================

CHƯƠNG 4 – SẢNH VÀ PHÒNG CHỜ

Hình 4.1 – Trạng thái người chơi và phòng

Người chơi:
FREE
→ IN_ROOM
→ PLAYING
→ IN_ROOM

Phòng:
WAITING
→ PLAYING
→ FINISHED
→ WAITING

Thể hiện các sự kiện:

- Vào phòng
- Bắt đầu trận
- Kết thúc trận
- Chơi lại
- Rời phòng

Hình 4.2 – Đồng bộ sảnh

Client
→ yêu cầu danh sách
→ Server
→ lấy danh sách người online + phòng
→ trả Client

Đồng thời thể hiện:
Khi sảnh thay đổi
→ Server chủ động gửi trạng thái mới
→ các Client đang ở sảnh cập nhật.

Hình 4.3 – Tạo và tham gia phòng

Client 1 tạo phòng
→ Server tạo phòng
→ Client 1 vào phòng

Client 2 yêu cầu tham gia
→ Server kiểm tra phòng
→ thêm Client 2
→ Server gửi trạng thái phòng mới cho cả hai Client.

Hình 4.4 – Chơi nhanh

Người chơi chọn PLAY
→ tìm phòng phù hợp
→ nếu có: tham gia phòng
→ nếu không có: tạo phòng mới
→ chuyển vào phòng chờ.

Hình 4.5 – Sẵn sàng và bắt đầu trận

Người chơi READY
→ Server cập nhật trạng thái
→ kiểm tra tất cả đã sẵn sàng
→ chủ phòng START
→ Server kiểm tra điều kiện
→ chuyển phòng sang PLAYING
→ khởi tạo trận
→ các Client chuyển sang màn hình GAME.

========================
6. VẼ LẠI CÁC HÌNH CHƯƠNG 5
=================================

CHƯƠNG 5 – GAME ENGINE VÀ ĐỒNG BỘ THỜI GIAN THỰC

Hình 5.1 – Thành phần Game Engine

Client
→ Gửi thao tác
→ Server tiếp nhận
→ Bộ xử lý trận của phòng
→ Trạng thái trận

Trạng thái trận gồm:

- Bản đồ
- Người chơi
- Bom
- Vụ nổ

Sau xử lý:
→ Server gửi trạng thái mới
→ các Client trong phòng.

Phải thể hiện mỗi phòng có trạng thái trận riêng.

Hình 5.2 – Xử lý thao tác di chuyển

Người chơi nhấn phím
→ Client gửi hướng
→ Server nhận
→ kiểm tra thao tác
→ kiểm tra ô đích
→ nếu hợp lệ cập nhật vị trí
→ gửi trạng thái mới cho Client.

Hình 5.3 – Chu kỳ xử lý trận

Bắt đầu chu kỳ
→ tiếp nhận thao tác
→ xử lý di chuyển / đặt bom
→ cập nhật bom và vụ nổ
→ kiểm tra người chơi bị loại
→ kiểm tra kết quả
→ gửi trạng thái mới
→ chu kỳ tiếp theo.

Hình 5.4 – Đặt bom và phát nổ

Người chơi yêu cầu đặt bom
→ Server kiểm tra
→ tạo bom
→ chờ thời gian nổ
→ bom phát nổ
→ tính vùng ảnh hưởng
→ cập nhật trạng thái
→ gửi tới Client.

Hình 5.5 – Lan lửa và nổ dây chuyền

Bom nổ
→ lan theo bốn hướng

Các nhánh:

- Gặp tường cứng → dừng.
- Gặp tường phá được → phá và dừng.
- Gặp bom khác → kích hoạt bom đó.
- Gặp người chơi → người chơi bị loại.

Hình 5.6 – Xác định kết quả trận

Có người bị loại
→ kiểm tra số người còn sống

Nếu > 1:
→ tiếp tục trận

Nếu = 1:
→ người còn lại thắng

Nếu = 0:
→ hòa

Sau đó:
→ gửi kết quả
→ kết thúc trận.

Hình 5.7 – Xử lý phía Client

Người chơi nhập bàn phím
→ Client gửi thao tác
→ Server xử lý
→ Client nhận trạng thái
→ cập nhật bản đồ / người chơi / bom / vụ nổ
→ hiển thị khung hình mới.

========================
7. VẼ LẠI CÁC HÌNH CHƯƠNG 6
=================================

CHƯƠNG 6 – DESKTOP CLIENT VÀ DỮ LIỆU TRẬN ĐẤU

Hình 6.1 – Kiến trúc phân tầng Desktop Client

Chỉ thể hiện 4 tầng:

Giao diện
↓
Trạng thái Client
↓
Xử lý ứng dụng
↓
Kết nối mạng

Thể hiện hai chiều:
Người dùng → Server
và
Server → giao diện.

Không đưa tên class vào.

Hình 6.2 – Phân phối thông điệp tới giao diện

Server
→ Client nhận thông điệp
→ xác định loại dữ liệu
→ cập nhật trạng thái Client
→ điều hướng nếu cần
→ cập nhật giao diện.

Hình 6.3 – Điều hướng màn hình

LOGIN
→ HOME
→ ROOM LIST / ROOM LOBBY
→ GAME
→ RESULT

Từ HOME có thể tới:

- HISTORY
- LEADERBOARD

RESULT:

- HOME
- PLAY AGAIN → ROOM LOBBY

Mất kết nối:
→ LOGIN.

Hình 6.4 – Lưu kết quả trận đấu

Game Engine xác định kết quả
→ tạo thông tin trận đấu
→ xác định kết quả cá nhân từng người
→ lưu TRẬN ĐẤU
→ lưu CHI TIẾT TRẬN ĐẤU
→ cập nhật thống kê TÀI KHOẢN
→ gửi kết quả về Client.

LƯU Ý:
Thiết kế CSDL hiện tại KHÔNG sử dụng winner_id và match_result.
Kết quả WIN / LOSS / DRAW nằm ở personal_result của từng người chơi.

Hình 6.5 – Tải lịch sử và bảng xếp hạng

Có thể thể hiện hai luồng song song:

LỊCH SỬ:
Client
→ yêu cầu lịch sử
→ Server
→ CSDL
→ dữ liệu các trận của người dùng
→ Client hiển thị

BẢNG XẾP HẠNG:
Client
→ yêu cầu xếp hạng
→ Server
→ CSDL
→ thống kê tài khoản
→ sắp xếp thứ hạng
→ Client hiển thị.

========================
8. KẾT QUẢ ĐẦU RA
=====================

Hãy xử lý lần lượt từng hình.

Với mỗi hình:

1. Nêu ngắn gọn hình hiện tại đang quá chi tiết ở đâu.
2. Đề xuất những thành phần nên giữ.
3. Đề xuất những thành phần cần loại bỏ.
4. Vẽ lại sơ đồ ở mức phân tích.
5. Đặt tên hình bằng tiếng Việt.
6. Không thêm chức năng mà tài liệu không có.
7. Không thay đổi logic nghiệp vụ hiện tại của hệ thống.

Nếu xuất bằng Mermaid:

- Dùng flowchart hoặc sequenceDiagram phù hợp.
- Code Mermaid phải chạy được.
- Không dùng tên class hoặc method.
- Nhãn các node bằng tiếng Việt.
- Không để node quá dài.
- Ưu tiên sơ đồ dễ đọc khi chèn vào Word.

Nếu tạo thành ảnh:

- ảnh đen trắng;
- nền trắng;
- chữ đen;
- khung đơn giản;
- chất lượng cao;
- bố cục học thuật;
- tỉ lệ phù hợp tài liệu Word;
- không dùng hình minh họa trang trí.

Quan trọng nhất:
Đây là báo cáo PHÂN TÍCH VÀ THIẾT KẾ ở mức chức năng.
Không biến sơ đồ thành tài liệu giải thích source code.
Người đọc cần hiểu "hệ thống làm gì và các thành phần tương tác như thế nào", không cần biết "hàm nào hoặc class nào thực hiện"
