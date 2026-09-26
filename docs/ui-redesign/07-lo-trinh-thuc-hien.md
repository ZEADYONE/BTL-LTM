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
- [x] Tạo module `client-fx`, thêm vào `settings.gradle`.
- [x] `build.gradle`: JavaFX 21.0.12 (classifier theo hệ điều hành), JSVG 2.2.0, Ikonli 12.4.0 + gói Material Design 2, `project(':common')`, JUnit 5.
- [x] `Launcher`, `BombermanApp`, `AppShell` (khung 1280×720 có scale, lớp toast, chuyển màn). Lớp popup làm ở giai đoạn 3 cùng các popup.
- [x] Lấy lại tầng network và state từ client cũ ([03](03-kien-truc-ky-thuat.md) mục 5), đổi sang `Platform.runLater`.
- [x] Lấy lại `ClientNetworkConfigTest`, thêm test cho thứ tự ưu tiên địa chỉ đã lưu.
- [x] Màn Login tạm và Home tạm (chưa làm đẹp) để thử kết nối.

**Nghiệm thu**
- [x] `.\gradlew :client-fx:run` mở được cửa sổ, đăng nhập được vào server thật (tài khoản LAZY, xác nhận trong log server).
- [x] Tắt server → quay về Login với thông báo lỗi (kiểm bằng `ClientNetworkIntegrationTest` với server TCP giả; nên thử lại tay với server thật).
- [x] `.\gradlew test` pass cho mọi module: server 60, common 9, client 1, client-fx 22.

## Giai đoạn 2 · Design system và tài nguyên (L)

**Đầu việc**
- [x] `css/game-theme.css`: biến màu và class cho component ([04](04-design-system.md)).
- [x] Các component ở [04](04-design-system.md) mục 5, thêm `Tag`, `GameFields`, `Motion.BACK_OUT`.
- [x] Nạp font F01, F02 (bản tĩnh từ Google Fonts; Nunito đủ 62/62 chữ tiếng Việt, Lilita One chỉ 6/62).
- [x] `SvgAssets` + `SvgRasterizer`: đọc SVG, đổi màu đội, cache, dự phòng PNG, ảnh thay thế khi thiếu file, nạp trước.
- [x] Vẽ **SVG tạm** cho A01–A22 và B07 (quyết định D6).
- [x] Màn Gallery (`--args="--gallery"`): mọi component ở mọi trạng thái, 4 màu nhân vật, tile, hiệu ứng lửa. Phím PageUp/PageDown/Home/End để cuộn.
- [x] Khung cam trong nền tối (lề 64/36), họa tiết bom, toast theo loại, lớp popup (chuyển từ giai đoạn 3 lên).

**Nghiệm thu**
- [x] Gallery hiển thị đủ, đổi màu 4 đội không sót mảng đỏ (có test tự động `SvgRasterizerTest`).
- [x] Sắc nét khi co giãn cửa sổ: ảnh vẽ lại theo tỷ lệ thật × DPI. Máy hiện tại chỉ thử được ở mức scale đang dùng; 125/150% cần thử thêm trên máy khác (kịch bản T14).
- [x] Thiếu file SVG → app không lỗi, hiện ảnh thay thế, có đúng một dòng log (Gallery có sẵn mục thử `icons/does_not_exist`).
- [x] Hiệu năng: luồng vẽ 0% khi đứng yên (xem mục "Bài học" bên dưới).

**Bài học từ giai đoạn 2** (đã ghi vào [03](03-kien-truc-ky-thuat.md) mục 13): bản đầu làm luồng vẽ bận ~100% và thao tác trễ ~2 giây, do viền chữ kiểu `outside`, hiệu ứng đặt trên node cha và animation chạy trong ScrollPane. Sau khi sửa: 0% khi đứng yên, 24% ở trường hợp nặng nhất có chủ đích (tia sáng xoay trong ScrollPane của Gallery).

## Giai đoạn 3 · Login, Home, popup (M)

**Đầu việc**
- [x] S1 Login/Register + popup Server Address ([02](02-man-hinh-va-dieu-huong.md) mục 3.1, 4.2). Thêm theo yêu cầu: ô CONFIRM PASSWORD và nút con mắt hiện/ẩn mật khẩu (`PasswordInput`).
- [x] S2 Home theo mẫu 1, các phần tử H-01…H-12 (`HeroCharacter` dùng chung với Login).
- [x] Quick Play (`game/QuickPlay` + `HomeScreen.tryJoin`).
- [x] Popup Settings, Help, Confirm, Connection Lost (`ui/popup`).
- [x] Theo dõi yêu cầu đang chờ, hết giờ 5 giây (`PendingRequests`); nút hiện vòng xoay khi chờ.
- [x] Màn chờ cho các màn chưa làm có nút thoát (BACK hoặc LEAVE ROOM).

**Nghiệm thu** (chạy thật với server + MySQL cục bộ, 2 client)
- [x] Đăng ký: xác nhận sai → viền đỏ + "Passwords do not match."; nút con mắt hiện đúng mật khẩu; đăng ký xong → toast xanh, về tab LOGIN, xóa mật khẩu.
- [x] Đăng nhập → Home đúng bố cục mẫu 1; tên, UNRANKED, số online, số phòng mở tự cập nhật khi client thứ hai đăng nhập.
- [x] Quick Play: không có phòng → tạo "fxtest3's Room"; client thứ hai bấm PLAY → vào đúng phòng đó (log server `Room joined` cùng roomId).
- [x] HELP, SETTINGS hiển thị đủ; Server Address báo lỗi cổng 99999.
- [x] Tắt server → popup CONNECTION LOST, về Login, chấm trạng thái chuyển xám.
- [x] `.\gradlew test` pass: client-fx 46, server 60, common 9, client 1.

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
