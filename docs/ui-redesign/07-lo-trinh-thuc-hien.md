# 07 · Lộ trình thực hiện

Chia thành 7 giai đoạn. Hết mỗi giai đoạn, app đều chạy được để cả nhóm xem và góp ý trước khi làm tiếp. Xong giai đoạn nào thì đánh dấu trong [README](README.md) mục Tiến độ.

Khối lượng: **S** ≈ nửa ngày · **M** ≈ 1 ngày · **L** ≈ 2 ngày làm việc.

```
GĐ1 ──► GĐ2 ──┬──► GĐ3 ──► GĐ4 ──► GĐ5 ──► GĐ6 ──► GĐ7
              └──► (thay SVG đẹp bất cứ lúc nào sau GĐ2)
```

---

## Giai đoạn 1 · Nền móng (M)

**Đầu việc**
- [ ] Tạo module `client-fx`, thêm vào `settings.gradle`.
- [ ] `build.gradle`: JavaFX 21 (classifier theo hệ điều hành), JSVG 2.2.0, Ikonli 12.4.0 + gói Material Design 2, `project(':common')`, JUnit 5.
- [ ] `Launcher`, `BombermanApp`, `AppShell` (khung 1280×720 có scale, lớp toast/popup, chuyển màn).
- [ ] Lấy lại tầng network và state từ client cũ ([03](03-kien-truc-ky-thuat.md) mục 5), đổi sang `Platform.runLater`.
- [ ] Lấy lại `ClientNetworkConfigTest`.
- [ ] Màn Login tạm (chưa làm đẹp) để thử kết nối.

**Nghiệm thu**
- [ ] `.\gradlew :client-fx:run` mở được cửa sổ, đăng nhập được vào server thật.
- [ ] Tắt server → quay về Login với thông báo lỗi.
- [ ] `.\gradlew test` pass cho mọi module.

## Giai đoạn 2 · Design system và tài nguyên (L)

**Đầu việc**
- [ ] `css/game-theme.css`: biến màu và class cho component ([04](04-design-system.md)).
- [ ] Các component ở [04](04-design-system.md) mục 5.
- [ ] Nạp font F01, F02.
- [ ] `SvgAssets`: đọc SVG, đổi màu đội, cache, dự phòng PNG, ảnh thay thế khi thiếu file, nạp trước.
- [ ] Vẽ **SVG tạm** cho A01–A22 và B07 (quyết định D6).
- [ ] Màn Gallery (`--args="--gallery"`): mọi component ở mọi trạng thái, 4 màu nhân vật, tile, hiệu ứng lửa.

**Nghiệm thu**
- [ ] Gallery hiển thị đủ, đổi màu 4 đội không sót mảng đỏ.
- [ ] Sắc nét ở Windows scale 100/125/150%.
- [ ] Đổi tên tạm một file SVG → app không lỗi, hiện ảnh thay thế, có log.

## Giai đoạn 3 · Login, Home, popup (M)

**Đầu việc**
- [ ] S1 Login/Register + popup Server Address ([02](02-man-hinh-va-dieu-huong.md) mục 3.1, 4.2).
- [ ] S2 Home theo mẫu 1, các phần tử H-01…H-12.
- [ ] Quick Play.
- [ ] Popup Settings, Help, Confirm, Connection Lost; Toast.
- [ ] Theo dõi yêu cầu đang chờ (hết giờ 5 giây).

**Nghiệm thu**
- [ ] Tiêu chí ở [02](02-man-hinh-va-dieu-huong.md) mục 3.1 và 3.2.
- [ ] Đặt cạnh mẫu 1 để so sánh: bố cục, màu, cỡ chữ gần như trùng.

## Giai đoạn 4 · Room Browser và Room Lobby (M)

**Đầu việc**
- [ ] S3 Room Browser: lưới phòng, tạo phòng, danh sách online.
- [ ] S4 Room Lobby theo mẫu 4 (R-01…R-20): slot, map xem trước có điểm xuất phát, panel INFO và ROOM STATUS.
- [ ] Quy tắc chuyển màn theo `ROOM_STATE` ([02](02-man-hinh-va-dieu-huong.md) mục 5).
- [ ] Nạp trước sprite trận khi vào S4.

**Nghiệm thu**
- [ ] Tiêu chí ở [02](02-man-hinh-va-dieu-huong.md) mục 3.3 và 3.4.
- [ ] Hai client: tạo phòng, vào phòng, READY, START → cả hai vào trận.

## Giai đoạn 5 · Màn chơi (L)

**Đầu việc**
- [ ] Bố cục S5: HUD (G-01…G-06), Canvas sân, trang trí hai bên.
- [ ] `GameRenderer` theo thứ tự vẽ ở [06](06-gameplay-va-hieu-ung.md) mục 5; `BoardLayout`.
- [ ] `InputController` (giữ phím, lặp `MOVE`).
- [ ] `PlayerVisual` (nội suy, hướng nhìn, dáng bước).
- [ ] Hiệu ứng ở [06](06-gameplay-va-hieu-ung.md) mục 6.
- [ ] `MatchTracker`.
- [ ] Menu ESC, dải thông báo khi mình bị loại.

**Nghiệm thu**
- [ ] 2–4 client chơi hết trận, giữ phím là đi liên tục, không giật.
- [ ] 60 FPS suốt trận (xem bằng F3).
- [ ] Hình dạng vụ nổ đúng khi gặp đá, thùng và bom khác.
- [ ] ESC → LEAVE MATCH hoạt động và người còn lại thắng.

## Giai đoạn 6 · Result, Leaderboard, History (M)

**Đầu việc**
- [ ] S6 Result: 3 biến thể, bảng xếp hạng theo thứ tự bị loại, tranh chấp Play Again.
- [ ] S7 Leaderboard: bục top 3 và bảng.
- [ ] S8 History.

**Nghiệm thu**
- [ ] Tiêu chí ở [02](02-man-hinh-va-dieu-huong.md) mục 3.6–3.8.
- [ ] Điểm hiển thị khớp với dữ liệu server sau vài trận.

## Giai đoạn 7 · Đóng gói và hoàn thiện (S)

**Đầu việc**
- [ ] Tạo `packaging/app_icon.ico` (16–256 px) từ C06 hoặc A02.
- [ ] Task `packageApp`; tùy chọn `packageInstaller`.
- [ ] Cập nhật README gốc: cách chạy và đóng gói `client-fx`.
- [ ] Chạy toàn bộ kịch bản thử tay ở [08](08-kiem-thu-va-rui-ro.md).

**Nghiệm thu**
- [ ] `BombermanOnline.exe` chạy trên máy không cài Java.
- [ ] Kết nối được server trong mạng LAN bằng địa chỉ nhập ở màn Login.
- [ ] Client cũ và client mới chơi chung một phòng được.

---

## Sau khi hoàn thành

- Quyết định có xóa client libGDX cũ hay không (D9).
- Thay dần SVG tạm bằng bản đẹp theo bảng ở [05](05-tai-nguyen-svg-va-font.md).
- Các chức năng đang ẩn ([01](01-doi-chieu-mau-thiet-ke.md), "Tổng hợp phần bị ẩn") cần làm ở server trước, rồi mới bật lại trên giao diện.
