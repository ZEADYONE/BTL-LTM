# 08 · Kiểm thử và rủi ro

## 1. Unit test (JUnit 5, trong `client-fx/src/test`)

Chỉ test phần logic thuần, không test giao diện.

| Lớp được test | Trường hợp cần có |
|---|---|
| `InputController` | Nhấn phím → gửi `MOVE` ngay. Giữ 500 ms → gửi ở 0, 140, 280, 420 ms. Giữ 2 phím → phím nhấn sau thắng. Nhả phím đó → quay về phím còn giữ và gửi ngay. Space không lặp. Đã bị loại hoặc menu đang mở → không gửi gì |
| `PlayerVisual` | Tiến về đích đúng tốc độ. Cách hơn 1 ô → tăng tốc. Cách hơn 2.5 ô → nhảy thẳng. Hướng nhìn đổi đúng. Đứng yên giữ hướng cũ |
| Phân loại mảnh lửa | Sân trống tầm 2 → 1 center, 4 mid, 4 end. Đá sát bên → hướng đó không có mảnh. Thùng sát bên → 1 end. Góc xoay đúng cho 4 hướng |
| `MatchTracker` | Ghi đúng thứ tự bị loại. Bị loại cùng snapshot → cùng hạng. Trận hòa. Thiếu dữ liệu → xếp theo WIN/DRAW/LOSS rồi slot. Trận mới → xóa dữ liệu cũ. Phát hiện thùng vỡ, vụ nổ mới |
| `QuickPlay` | Chọn phòng WAITING đông nhất. Bỏ qua phòng đầy/đang chơi/đã xong. Bằng nhau → phòng đứng trước. Không có phòng → tạo `"<username>'s Room"`, cắt còn 60 ký tự |
| `SvgRecolor` | Thay đủ 3 màu khóa cho 4 đội. Không phân biệt hoa thường. Không đụng các màu khác |
| `MapPreview` | Map dựng ra khớp map của server (so với `GameMap.createDefault()` qua bảng ở [03](03-kien-truc-ky-thuat.md) mục 9) |
| Quy tắc chuyển màn | `ROOM_STATE` WAITING khi đang ở Result → ở lại, bật cờ rematch. `member = false` → Home. `GAME_STATE` khi đang ở Home → Game. Mất kết nối → Login |
| Kiểm tra dữ liệu nhập | Username 0 / 1 / 50 / 51 ký tự và toàn khoảng trắng. Tên phòng có khoảng trắng hai đầu, 60 / 61 ký tự. Port 0 / 1 / 65535 / 65536 |
| `PendingRequests` | Phản hồi đúng `requestId` → mở khóa. Hết 5 giây → báo hết giờ. Phản hồi đến muộn sau khi hết giờ → bỏ qua |
| `ClientNetworkConfig` | Test cũ lấy lại, thêm trường hợp đọc địa chỉ đã lưu |

Test của `server` và `common` không sửa và vẫn phải pass.

## 2. Kịch bản thử tay

Chuẩn bị: MySQL và server chạy như README gốc. Mở nhiều client trên một máy hoặc nhiều máy trong mạng LAN.

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| T01 | Đăng ký tài khoản mới với tên tiếng Việt có dấu, rồi đăng nhập | Thành công; tên hiển thị đúng dấu ở mọi màn |
| T02 | Đăng nhập sai mật khẩu; đăng nhập cùng tài khoản trên 2 client | Hiện đúng thông báo ở [02](02-man-hinh-va-dieu-huong.md) mục 6.2 |
| T03 | Tắt server rồi đăng nhập; đổi địa chỉ server qua nút bánh răng | Báo không kết nối được; đổi địa chỉ xong đăng nhập được, mở lại app vẫn nhớ địa chỉ |
| T04 | Ở Home, cho client khác đăng nhập/tạo phòng | Số online, số phòng tự cập nhật |
| T05 | Quick Play khi: không có phòng; có 1 phòng chờ; chỉ có phòng đầy | Lần lượt: tạo phòng mới; vào phòng đó; tạo phòng mới |
| T06 | Room Browser: tạo phòng, vào phòng; thử vào phòng đầy hoặc đang chơi | Nút JOIN bị khóa với phòng đầy hoặc đang chơi |
| T07 | Room Lobby với 4 client: READY, UNREADY, START; host rời phòng | Màu slot đúng thứ tự; START chỉ sáng khi đủ điều kiện; host mới có vương miện |
| T08 | Trận 2 người: giữ phím đi, đặt bom, nổ dây chuyền, phá thùng, bị loại | Đi liên tục khi giữ phím; hiệu ứng đúng [06](06-gameplay-va-hieu-ung.md); người thắng thấy VICTORY, người thua thấy DEFEAT |
| T09 | Hai người cùng bị loại trong một vụ nổ | Cả hai thấy DRAW, cùng hạng 1 |
| T10 | Giữa trận bấm ESC → LEAVE MATCH | Người rời về Home; người còn lại thắng |
| T11 | Hai người ở màn kết quả, A bấm PLAY AGAIN trước | B vẫn ở màn kết quả, thấy toast, nút đổi thành BACK TO ROOM |
| T12 | Bấm HOME ở màn kết quả | Rời phòng, về Home |
| T13 | Mở Leaderboard và History sau các trận trên | Điểm, số trận thắng/hòa/thua, thời lượng trận khớp dữ liệu |
| T14 | Kéo nhỏ/lớn cửa sổ, F11, đổi Windows scale 125% và 150% | Bố cục không vỡ, hình không mờ |
| T15 | Tắt server giữa trận | Popup CONNECTION LOST → Login |
| T16 | Đổi tên tạm một file SVG rồi chạy | App không lỗi, hiện ảnh thay thế, có log |
| T17 | Chạy file exe trên máy không cài Java, kết nối server qua LAN | Chạy và chơi được |
| T18 | Một client cũ (libGDX) và một client mới vào chung phòng rồi chơi | Chơi bình thường |
| T19 | Trận 4 người, bật F3 | FPS luôn từ 55 trở lên |

## 3. Rủi ro

| Mã | Rủi ro | Khả năng | Ảnh hưởng | Cách giảm |
|---|---|---|---|---|
| R1 | Chưa có bộ SVG, hoặc chất lượng không đều | Cao | Giao diện không đẹp như mẫu | Có SVG tạm từ giai đoạn 2; quy chuẩn rõ ở [05](05-tai-nguyen-svg-va-font.md); thay dần không cần sửa code |
| R2 | File SVG dùng tính năng JSVG không hỗ trợ | Trung bình | Hình vẽ sai | Quy chuẩn cấm filter, chữ, ảnh nhúng; có ảnh thay thế + log; kiểm tra bằng Gallery |
| R3 | Lilita One không có dấu tiếng Việt | Chắc chắn | Tên có dấu hiển thị lỗi | Chữ người dùng nhập luôn dùng Nunito |
| R4 | Hình bị mờ khi scale hoặc ở màn hình DPI cao | Trung bình | Kém sắc nét | Vẽ ảnh theo kích thước pixel thật; Canvas không bị phóng bitmap ([03](03-kien-truc-ky-thuat.md) mục 6) |
| R5 | Server đổi luật (tick, map, điểm) | Thấp | Client vẽ/hiển thị sai | Gom vào `ServerRules`, có bảng nguồn ở [03](03-kien-truc-ky-thuat.md) mục 9 |
| R6 | Server không giới hạn tốc độ `MOVE`: client bị sửa đổi có thể đi nhanh hơn | Trung bình | Mất công bằng | Ngoài phạm vi (cần sửa server); ghi nhận để làm sau |
| R7 | Độ trễ 50–150 ms do chỉ có 10 snapshot/giây | Trung bình | Phản hồi phím hơi chậm | Nội suy chuyển động; tùy chọn tăng lên 20 snapshot/giây (D10) |
| R8 | Màu slot đổi khi người vào trước rời phòng | Thấp | Hơi khó theo dõi | Chấp nhận, vì mọi client thấy giống nhau |
| R9 | File exe nặng 60–90 MB | Thấp | Gửi file lâu | Tùy chọn giới hạn module JDK sau khi bản đầy đủ đã ổn |
| R10 | Ẩn nhiều phần tử làm bố cục trống | Trung bình | Không giống mẫu | Đã thay bằng dữ liệu thật (ROOM STATUS, ONLINE ROOMS…); review ở cuối mỗi giai đoạn |
| R11 | Cảnh báo "Unsupported JavaFX configuration" khi chạy từ classpath | Chắc chắn | Chỉ là log | Vô hại; ghi chú trong README |
