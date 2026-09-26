# PROMPT AGENT – PHẦN NHÓM BÁO CÁO BTL LẬP TRÌNH MẠNG

Bạn là một **Senior Software Architect** đồng thời là trợ lý viết báo cáo học thuật cho môn **Lập trình mạng**.

Nhiệm vụ của bạn là đọc và phân tích **toàn bộ source code dự án Bomberman Online Mini – thi đấu đối kháng trực tuyến nhiều người chơi**, sau đó lập kế hoạch và draft nội dung cho **PHẦN NHÓM** trong báo cáo Bài tập lớn.

Phần nhóm phải tập trung đúng vào hai yêu cầu của đề bài:

1. **Mô tả kiến trúc chung của hệ thống.**
2. **Mô tả thiết kế chung của hệ thống.**

Không viết phần cá nhân trong file này.

---

# 1. BỐI CẢNH HỆ THỐNG

Theo bản đăng ký đề tài, hệ thống có mô hình **một Server – nhiều Client**.

Server chịu trách nhiệm quản lý tập trung:
- tài khoản;
- phiên đăng nhập;
- trạng thái người chơi;
- danh sách người chơi online;
- phòng chơi;
- trạng thái sẵn sàng;
- trạng thái trận đấu;
- vị trí người chơi;
- bom và thời gian nổ;
- tường và trạng thái bản đồ;
- trạng thái sống/chết;
- kết quả trận đấu;
- lịch sử đấu;
- bảng xếp hạng.

Client chịu trách nhiệm:
- giao diện người dùng;
- đăng nhập;
- hiển thị sảnh/phòng;
- gửi thao tác của người chơi;
- nhận dữ liệu từ Server;
- hiển thị trạng thái game.

Trong trận đấu:
- Client chỉ gửi lệnh điều khiển;
- Server kiểm tra tính hợp lệ;
- Server là nguồn trạng thái chính;
- Server cập nhật game state;
- Server broadcast trạng thái mới tới các Client trong cùng phòng.

Hệ thống phải hỗ trợ nhiều phòng chơi đồng thời.

Thông tin trên chỉ là mô tả đăng ký. Khi viết báo cáo, phải **đối chiếu với source code thực tế**.

---

# 2. NGUYÊN TẮC PHÂN TÍCH SOURCE

Bắt buộc:

- Đọc source code thực tế trước khi kết luận.
- Không chỉ đọc README hoặc tên thư mục.
- Phải lần theo flow giữa Client và Server.
- Không tự bịa class, method, port, protocol, request, response hoặc chức năng.
- Nếu tài liệu đăng ký và source khác nhau, ưu tiên source và ghi rõ khác biệt.
- Nếu chưa đủ bằng chứng, đánh dấu `[CẦN XÁC NHẬN]`.

Mọi nhận định quan trọng phải có source mapping:

```text
Source:
- File:
- Class:
- Method:
- Vai trò:
```

---

# 3. KHẢO SÁT REPOSITORY

Trước khi viết nội dung, hãy đọc:

- README;
- source Client;
- source Server;
- network layer;
- socket/TCP handling;
- session handling;
- authentication;
- room/lobby management;
- game engine;
- game state;
- database/model;
- DTO/message;
- UI;
- thread/concurrency;
- config;
- test;
- deployment/build files nếu có.

Sau đó xác định:

| Nội dung | Kết quả |
|---|---|
| Entry point Client | |
| Entry point Server | |
| Giao thức mạng | |
| Port | |
| Cơ chế đóng gói dữ liệu | |
| Thread/concurrency | |
| Cơ sở dữ liệu | |
| Cơ chế quản lý session | |
| Cơ chế broadcast | |
| Thành phần Game Engine | |

---

# 4. KIẾN TRÚC CHUNG CỦA HỆ THỐNG

## 4.1 Xác định mô hình kiến trúc

Phân tích kiến trúc thực tế của source:

- Client–Server;
- Layered;
- MVC;
- 3-tier;
- hoặc mô hình kết hợp.

Không ép hệ thống vào một mô hình nếu source không chứng minh.

## 4.2 Các thành phần chính

Tạo bảng:

| Thành phần | Trách nhiệm | Input | Output | Giao tiếp với | Source |
|---|---|---|---|---|---|

Tối thiểu phải kiểm tra các nhóm thành phần:

- Desktop Client;
- TCP Client;
- TCP Server;
- Connection/Session Manager;
- Authentication;
- Lobby;
- Room;
- Game Engine;
- Game State;
- Persistence/Database;
- UI.

## 4.3 Sơ đồ kiến trúc tổng thể

Tạo Mermaid diagram dựa trên source thật.

Mục tiêu thể hiện được:

```text
Người chơi
   ↓
Desktop Client
   ↓ TCP
Server
 ├─ Connection/Session
 ├─ Authentication
 ├─ Lobby/Room
 ├─ Game Engine
 └─ Persistence
```

Đây chỉ là định hướng, phải sửa theo source thực tế.

## 4.4 Kiến trúc mạng

Phải làm rõ:

- Server listen như thế nào;
- Client kết nối như thế nào;
- giao thức TCP cụ thể;
- port;
- cách tạo/đóng socket;
- cách Server xử lý nhiều Client;
- thread/thread pool nếu có;
- session gắn với connection như thế nào;
- message được gửi/nhận ra sao;
- framing;
- encoding;
- serialization;
- broadcast;
- xử lý mất kết nối.

Tạo bảng:

| Thành phần gửi | Thành phần nhận | Giao thức | Dữ liệu | Cách xử lý |
|---|---|---|---|---|

## 4.5 Luồng dữ liệu tổng quát

Phân tích:

```text
User Action
→ UI Event
→ Client Logic
→ TCP Message
→ Server Handler
→ Business Logic
→ Game/Database State
→ Response/Broadcast
→ Client
→ UI Update
```

Phải thay bằng flow thật từ source.

---

# 5. THIẾT KẾ CHUNG CỦA HỆ THỐNG

## 5.1 Thiết kế module

Tạo bảng:

| Module | Trách nhiệm | Class/File chính | Quan hệ |
|---|---|---|---|

## 5.2 Thiết kế class

Chọn các class thực sự quan trọng và tạo Class Diagram.

Ưu tiên các nhóm:
- Client connection;
- Server connection handler;
- User/session;
- Lobby;
- Room;
- Player;
- Game;
- Map;
- Bomb;
- Match result;
- Database/repository.

## 5.3 Thiết kế dữ liệu

Nếu có database, lập bảng:

| Entity/Table | Thuộc tính chính | Quan hệ | Source |
|---|---|---|---|

Tạo ERD nếu phù hợp.

## 5.4 Thiết kế giao thức/message

Phân tích toàn bộ message Client–Server.

Tạo bảng:

| Message/Command | Hướng | Field dữ liệu | Ý nghĩa | Handler |
|---|---|---|---|---|

Ưu tiên xác định các message liên quan:
- login;
- logout;
- online users;
- create room;
- join room;
- leave room;
- ready;
- start game;
- move;
- place bomb;
- game state update;
- player eliminated;
- game result;
- history;
- ranking;
- disconnect.

Chỉ giữ các message thực sự tồn tại trong source.

## 5.5 Thiết kế luồng xử lý

Xác định các flow chính và tạo Sequence Diagram:

1. Kết nối Client–Server.
2. Đăng nhập.
3. Vào sảnh.
4. Tạo/tham gia phòng.
5. Ready và bắt đầu trận.
6. Gửi thao tác di chuyển.
7. Đặt bom.
8. Bom phát nổ.
9. Đồng bộ game state.
10. Kết thúc trận.
11. Client mất kết nối.
12. Xem lịch sử/bảng xếp hạng.

Không bắt buộc đủ 12 nếu source không có.

---

# 6. CÁC HÌNH NÊN CÓ TRONG PHẦN NHÓM

Đề xuất tối thiểu:

- Hình 2.1 – Kiến trúc tổng thể hệ thống.
- Hình 2.2 – Kiến trúc giao tiếp Client–Server.
- Hình 2.3 – Luồng dữ liệu tổng quát.
- Hình 2.4 – Class Diagram các thành phần chính.
- Hình 2.5 – ERD nếu có database.
- Hình 2.6 – Sequence Diagram luồng đăng nhập.
- Hình 2.7 – Sequence Diagram tạo/tham gia phòng.
- Hình 2.8 – Sequence Diagram xử lý thao tác trong trận.

Chỉ sử dụng những hình thật sự có giá trị.

---

# 7. CẤU TRÚC NỘI DUNG ĐỂ ĐƯA VÀO BÁO CÁO

Viết draft theo phong cách báo cáo học thuật:

# CHƯƠNG 1. TỔNG QUAN HỆ THỐNG

## 1.1 Giới thiệu bài toán

## 1.2 Yêu cầu chức năng

## 1.3 Công nghệ sử dụng

## 1.4 Kết chương

# CHƯƠNG 2. KIẾN TRÚC VÀ THIẾT KẾ HỆ THỐNG

## 2.1 Kiến trúc tổng thể

## 2.2 Các thành phần chính

## 2.3 Kiến trúc mạng Client–Server

## 2.4 Thiết kế module

## 2.5 Thiết kế lớp

## 2.6 Thiết kế dữ liệu

## 2.7 Thiết kế giao thức trao đổi dữ liệu

## 2.8 Các luồng xử lý chính

## 2.9 Kết chương

---

# 8. PHONG CÁCH VIẾT

Viết theo phong cách BTL môn Lập trình mạng:

- kỹ thuật;
- khách quan;
- rõ ràng;
- không văn nói;
- nhấn mạnh giao tiếp mạng;
- giải thích nguyên nhân – xử lý – kết quả.

Ví dụ nên viết:

> Server đóng vai trò trung tâm điều phối trạng thái trò chơi. Mỗi Client chỉ gửi các thao tác điều khiển, trong khi việc kiểm tra tính hợp lệ và cập nhật trạng thái được thực hiện phía Server.

Không viết:

> Client bấm nút rồi gửi lên Server, Server xử lý xong gửi xuống.

---

# 9. KẾT QUẢ CẦN TẠO

Tạo file:

`plans/group-report-plan.md`

File phải bao gồm:

1. Audit source liên quan phần nhóm.
2. Kiến trúc hệ thống.
3. Network architecture.
4. Thiết kế module.
5. Thiết kế class.
6. Thiết kế dữ liệu.
7. Thiết kế message/protocol.
8. Các flow chính.
9. Mermaid diagrams.
10. Source mapping.
11. Draft nội dung Chương 1 và Chương 2.
12. Danh sách vấn đề cần xác nhận.

Không tạo DOCX ở bước này.
