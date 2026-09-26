# Nâng cấp giao diện client – Bomberman Online Mini

Bộ tài liệu gồm kế hoạch và yêu cầu cho việc làm lại giao diện client desktop, theo 6 mẫu thiết kế trong thư mục [`img/`](../../img/).

> **Trạng thái:** đang lập kế hoạch, chưa viết code.
> **Cập nhật lần cuối:** 2026-09-26

## Mục tiêu

- Giao diện theo phong cách game casual 2D của các mẫu: nút nổi, viền đậm, nền cam, nhân vật chibi.
- Chơi mượt hơn client libGDX hiện tại: giữ phím là đi liên tục, nhân vật trượt mượt giữa các ô, có hiệu ứng bom và vụ nổ.
- Đóng gói thành file `.exe`, chạy được trên máy không cài Java.

## Phạm vi

| Có làm | Không làm |
|---|---|
| Module mới `client-fx` viết bằng JavaFX | Sửa `server` và `common` |
| Giao diện cho mọi chức năng **server đã có** | Các chức năng server chưa có (xem [01](01-doi-chieu-mau-thiet-ke.md), mục "Tổng hợp phần bị ẩn") |
| Đóng gói exe bằng `jpackage` | Bản web, âm thanh |
| Giữ client libGDX cũ cho tới khi client mới xong | Đổi giao thức mạng |

## Nguyên tắc

1. Server vẫn là nơi quyết định trạng thái game. Client chỉ gửi input và vẽ lại snapshot, giống hiện tại.
2. Không hiển thị thứ không có dữ liệu thật. Phần tử nào trong mẫu mà server chưa hỗ trợ thì **ẩn**, hoặc **thay bằng dữ liệu đang có**.
3. Tầng mạng giữ nguyên TCP socket và codec length-prefix hiện có, vì đây là trọng tâm của môn Lập trình mạng.
4. Mỗi hình ảnh là một file SVG riêng. Muốn đổi hình thì thay file, không phải sửa code.

## Danh sách tài liệu

| File | Nội dung | Người đọc chính |
|---|---|---|
| [01-doi-chieu-mau-thiet-ke.md](01-doi-chieu-mau-thiet-ke.md) | Từng phần tử trong 6 mẫu được giữ, thay hay ẩn | Cả nhóm |
| [02-man-hinh-va-dieu-huong.md](02-man-hinh-va-dieu-huong.md) | Đặc tả từng màn hình, luồng chuyển màn, xử lý thông điệp từ server, thông báo lỗi | Người code giao diện |
| [03-kien-truc-ky-thuat.md](03-kien-truc-ky-thuat.md) | Thư viện, cấu trúc module, quyết định kỹ thuật, đóng gói | Người code |
| [04-design-system.md](04-design-system.md) | Màu, font, kích thước, component, animation | Người code giao diện, người vẽ |
| [05-tai-nguyen-svg-va-font.md](05-tai-nguyen-svg-va-font.md) | Danh sách SVG và font cần có, quy chuẩn vẽ, gợi ý prompt AI | Người chuẩn bị hình ảnh |
| [06-gameplay-va-hieu-ung.md](06-gameplay-va-hieu-ung.md) | Luật server liên quan, điều khiển, nội suy chuyển động, thứ tự vẽ, hiệu ứng | Người code màn chơi |
| [07-lo-trinh-thuc-hien.md](07-lo-trinh-thuc-hien.md) | 7 giai đoạn, đầu việc, tiêu chí nghiệm thu | Cả nhóm |
| [08-kiem-thu-va-rui-ro.md](08-kiem-thu-va-rui-ro.md) | Unit test, kịch bản thử tay, rủi ro | Cả nhóm |

## Mẫu thiết kế

| # | Màn | File mẫu | Cách xử lý |
|---|---|---|---|
| 1 | Home | [PM-1](<../../img/ChatGPT Image Sep 26, 2026, 08_48_55 PM-1.png>) | Làm, có điều chỉnh |
| 2 | Select Mode | [PM-2](<../../img/ChatGPT Image Sep 26, 2026, 08_48_58 PM-2.png>) | Ẩn cả màn |
| 3 | Character Select | [PM-3](<../../img/ChatGPT Image Sep 26, 2026, 08_48_59 PM-3.png>) | Ẩn cả màn |
| 4 | Room Lobby | [PM-4](<../../img/ChatGPT Image Sep 26, 2026, 08_49_01 PM-4.png>) | Làm, có điều chỉnh |
| 5 | Trong trận | [PM-5](<../../img/ChatGPT Image Sep 26, 2026, 08_49_03 PM-5.png>) | Làm, có điều chỉnh |
| 6 | Victory | [PM-6](<../../img/ChatGPT Image Sep 26, 2026, 08_49_06 PM-6.png>) | Làm, thêm biến thể Defeat và Draw |

Có 4 màn bắt buộc nhưng chưa có mẫu: **Login, Room Browser, Leaderboard, History**. Các màn này được thiết kế cùng phong cách, xem [02](02-man-hinh-va-dieu-huong.md).

## Quyết định

| Mã | Nội dung | Trạng thái |
|---|---|---|
| D1 | Chỉ làm desktop, không làm web | ✅ Đã chốt |
| D2 | Client viết bằng Java (JavaFX), không dùng Electron hay Tauri | ✅ Đã chốt |
| D3 | Làm theo mẫu trong `img/`; chức năng chưa có thì ẩn hoặc chưa làm | ✅ Đã chốt |
| D4 | Build ra file `.exe` | ✅ Đã chốt |
| D5 | Coin, Gem, Level được thay bằng tổng điểm, số người online và hạng | ✅ Theo đề xuất (chốt khi bắt đầu GĐ1) |
| D6 | Claude vẽ SVG tạm đúng tên file và viewBox; nhóm thay dần bằng bản đẹp | ✅ Theo đề xuất (chốt khi bắt đầu GĐ1) |
| D7 | Màu nhân vật ngoài trận tính theo `userId % 4` | ✅ Theo đề xuất (chốt khi bắt đầu GĐ1) |
| D8 | Chữ trên UI bằng tiếng Anh như mẫu; tên người chơi và tên phòng dùng font có dấu tiếng Việt | ✅ Theo đề xuất (chốt khi bắt đầu GĐ1) |
| D9 | Giữ client libGDX cũ tới khi client mới xong | ✅ Theo đề xuất (chốt khi bắt đầu GĐ1) |
| D10 | Không sửa server, giữ 10 snapshot/giây | ✅ Theo đề xuất (chốt khi bắt đầu GĐ1) |

Khi một quyết định thay đổi, cập nhật bảng này và các file liên quan.

## Tiến độ

- [x] Giai đoạn 1: Nền móng (2026-09-26) — chạy bằng `.\gradlew :client-fx:run`
- [x] Giai đoạn 2: Design system và tài nguyên (2026-09-26) — xem bằng `.\gradlew :client-fx:run --args="--gallery"`
- [x] Giai đoạn 3: Login, Home, popup (2026-09-26)
- [ ] Giai đoạn 4: Room Browser, Room Lobby
- [ ] Giai đoạn 5: Màn chơi
- [ ] Giai đoạn 6: Result, Leaderboard, History
- [ ] Giai đoạn 7: Đóng gói exe

Tài nguyên: 22/22 SVG bắt buộc đã có **bản tạm** (0/22 bản đẹp), B07 họa tiết nền có bản tạm, 2/2 bộ font (xem [05](05-tai-nguyen-svg-va-font.md)).

## Yêu cầu tổng quát

### Yêu cầu chức năng

| Mã | Yêu cầu |
|---|---|
| FR-01 | Đăng ký, đăng nhập, đăng xuất. Nhập được địa chỉ server (host:port) và nhớ giá trị đã dùng lần trước |
| FR-02 | Home hiển thị tên, hạng, tổng điểm, số người online và số phòng đang mở |
| FR-03 | Nút Quick Play đưa người chơi vào phòng phù hợp, hoặc tạo phòng mới nếu không có |
| FR-04 | Xem danh sách phòng, tạo phòng, vào phòng; xem ai đang online và trạng thái của họ |
| FR-05 | Trong phòng: xem 4 slot, biết ai là host, ai đã sẵn sàng; bật/tắt READY; host bấm START; rời phòng |
| FR-06 | Trong trận: di chuyển, đặt bom; HUD hiện người chơi, số bom, tầm nổ, thời gian; rời trận bằng ESC |
| FR-07 | Màn kết quả có 3 biến thể thắng/thua/hòa, kèm bảng xếp hạng của trận; có PLAY AGAIN và HOME |
| FR-08 | Xem bảng xếp hạng toàn server và lịch sử trận của mình |
| FR-09 | Popup hướng dẫn chơi; popup cài đặt (toàn màn hình, xem server, đăng xuất) |
| FR-10 | Kết quả và lỗi được báo bằng toast; mất kết nối thì báo lỗi và quay về màn đăng nhập |

### Yêu cầu phi chức năng

| Mã | Yêu cầu |
|---|---|
| NFR-01 | Màn chơi đạt 60 FPS trên máy phổ thông, không giật khi nhận snapshot |
| NFR-02 | Hình sắc nét ở Windows scale 100/125/150% và khi đổi kích thước cửa sổ; cửa sổ nhỏ nhất 960×540 |
| NFR-03 | Bố cục bám sát mẫu ở tỷ lệ 16:9 |
| NFR-04 | Hiển thị đúng tiếng Việt có dấu trong tên người chơi và tên phòng |
| NFR-05 | Thiếu hoặc hỏng file hình thì app vẫn chạy: hiện hình thay thế và ghi log |
| NFR-06 | Chạy từ file exe mà không cần cài Java |
| NFR-07 | Không đổi giao thức: client cũ và client mới chơi chung được trên cùng một server |
