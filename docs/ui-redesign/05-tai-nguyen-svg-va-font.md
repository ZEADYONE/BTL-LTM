# 05 · Tài nguyên: SVG và font

Tài liệu dành cho người chuẩn bị hình ảnh. Không cần biết code, chỉ cần làm đúng tên file, kích thước và quy chuẩn bên dưới.

## 1. Tóm tắt

| Nhóm | Số file | Ghi chú |
|---|---|---|
| Bắt buộc (A01–A22) | 22 SVG | Thiếu thì app vẫn chạy nhưng hiện hình thay thế |
| Nên có (B01–B12) | 12 SVG | Thiếu thì code tự thay bằng hình đơn giản hơn |
| Tùy chọn (C01–C06) | 6 mục | Làm đẹp thêm |
| Font (F01–F02) | 4 file TTF | Miễn phí, giấy phép OFL |

- Các mẫu trong `img/` là ảnh PNG do AI tạo và ghép sẵn, **không cắt ra dùng được**: nhân vật dính liền nền, độ phân giải cố định. Chỉ dùng chúng để **tham khảo phong cách**.
- Theo quyết định D6: Claude sẽ vẽ **bản tạm** cho toàn bộ file bắt buộc, đúng tên file và kích thước, để app chạy được trước. Nhóm thay dần bằng bản đẹp. Thay file là đổi hình, không phải sửa code.

## 2. Quy chuẩn chung cho mọi file SVG

| Mục | Yêu cầu |
|---|---|
| Nền | Trong suốt |
| `viewBox` | Đúng như bảng. Không đặt `width`/`height` khác với viewBox |
| Viền | Màu `#2A1F3D`, dày khoảng 5–6% cạnh ảnh: ≈3.5 đơn vị với ảnh 64, ≈7 với 128, ≈14 với 256, ≈28 với 512 |
| Tô màu | Màu phẳng, một lớp bóng tối, một vệt sáng. Được dùng `linearGradient` và `radialGradient` |
| **Cấm** | Filter (blur, drop-shadow…), ảnh nhúng (`<image>`), chữ (`<text>`; chữ phải chuyển thành path), mask phức tạp, CSS ngoài |
| Bóng đổ | Không vẽ. Code tự vẽ bóng dưới chân và dưới khối |
| Căn chỉnh | Nằm giữa khung, chừa lề khoảng 4%, trừ khi bảng ghi điểm neo riêng |
| Dung lượng | Mỗi file nhỏ hơn 100 KB. Giảm bớt điểm nút nếu lớn hơn |
| Tên file | Chữ thường, gạch dưới, đúng như bảng |

## 3. Màu khóa của nhân vật

Code đổi màu nhân vật cho 4 đội bằng cách thay 3 mã màu. Vì vậy **mỗi tư thế chỉ cần vẽ một file, màu đỏ**:

| Vai trò | Mã màu (đội Đỏ) | Tô cho |
|---|---|---|
| Màu chính | `#E53935` | Mũ bảo hiểm, bộ đồ |
| Màu tối | `#B71C1C` | Phần bóng của mũ và bộ đồ |
| Màu sáng | `#FF8A80` | Vệt sáng trên mũ và bộ đồ |

Quy tắc:
- Viết màu dưới dạng `fill="#E53935"` hoặc `stop-color="#E53935"`. Không dùng `rgb()` hay tên màu.
- Phần **không** thuộc màu đội (mặt, găng tay, giày, tai nghe vàng, bom đen…) **không được** dùng 3 mã này, kể cả mã gần giống.
- Sau khi vector hóa, kiểm tra lại: phần màu đội phải đúng 3 mã trên. Mã lệch như `#E53936` sẽ không được đổi màu.

Ba đội còn lại do code tự sinh: Xanh dương `#1E88E5 / #0D47A1 / #90CAF9`, Xanh lá `#43A047 / #1B5E20 / #A5D6A7`, Vàng `#FDD835 / #F9A825 / #FFF59D`.

## 4. Bắt buộc – 22 file

Cột **Tạm**: đã có bản tạm. Cột **Đẹp**: đã có bản chính thức. Đổi ☐ thành ☑ khi xong.

### 4.1 Nhân vật – `characters/`

| Mã | File | viewBox | Cần vẽ | Dùng ở | Tạm | Đẹp |
|---|---|---|---|---|---|---|
| A01 | `bomber_full.svg` | 0 0 512 512 | Toàn thân, đứng nghiêng 3/4 như mẫu 1: mũ bảo hiểm màu đội có tai nghe vàng, dây ngòi và tia lửa trên đỉnh; mặt nháy mắt cười; bộ đồ màu đội, thắt lưng đen khóa vàng; găng và giày trắng; tay cầm quả bom đen. Chân chạm y ≈ 480, căn giữa theo chiều ngang | Login, Home, slot phòng (cắt lấy nửa trên), kết quả hòa, bục Leaderboard | ☐ | ☐ |
| A02 | `bomber_head.svg` | 0 0 256 256 | Chỉ đầu và mũ, nhìn thẳng, cười. Chừa lề 8 | Mọi avatar: badge, HUD, thẻ phòng, bảng xếp hạng, lịch sử | ☐ | ☐ |
| A03 | `bomber_down.svg` | 0 0 128 128 | Nhân vật nhỏ trong sân: đầu to (khoảng 55% chiều cao), nhìn chính diện, dáng đang bước. Chân chạm y ≈ 120. Nét đơn giản để còn rõ khi thu nhỏ còn 48–64 px | Trong trận: đứng yên, đi xuống | ☐ | ☐ |
| A04 | `bomber_up.svg` | 0 0 128 128 | Như A03 nhưng nhìn từ sau lưng | Trong trận: đi lên | ☐ | ☐ |
| A05 | `bomber_side.svg` | 0 0 128 128 | Như A03 nhưng nhìn nghiêng, **quay sang phải**. Code lật ngang để có hướng trái | Trong trận: đi trái/phải | ☐ | ☐ |

### 4.2 Bản đồ – `tiles/`

Khối trên sân nhìn nghiêng 3/4 từ trên xuống: ảnh cao 80, rộng 64. **Đáy ảnh trùng đáy ô**; 16 đơn vị dư phía trên sẽ nhô lên ô phía trên, tạo cảm giác khối có chiều cao.

| Mã | File | viewBox | Cần vẽ | Dùng ở | Tạm | Đẹp |
|---|---|---|---|---|---|---|
| A06 | `block_stone.svg` | 0 0 64 80 | Khối đá xám: mặt trên sáng `#B8BCC8` chiếm y 0–60, mặt trước tối `#7C8091` chiếm y 60–80, vài vết nứt nhỏ | Ô `HARD_WALL` (cột bên trong và tường bao) | ☐ | ☐ |
| A07 | `crate.svg` | 0 0 64 80 | Thùng gỗ cam nâu (`#E08A3C` / `#B8621F`) có khung viền và tấm gỗ chéo như mẫu 5; cùng hình khối với A06 | Ô `BREAKABLE_WALL` | ☐ | ☐ |

### 4.3 Bom và vụ nổ – `fx/`

| Mã | File | viewBox | Cần vẽ | Dùng ở | Tạm | Đẹp |
|---|---|---|---|---|---|---|
| A08 | `bomb.svg` | 0 0 64 64 | Bom tròn đen ánh tím (`#3A3550`) có vệt sáng, nắp ngòi xám, dây ngòi cong màu be. **Không vẽ tia lửa.** Thân bom là hình tròn tâm (30, 38) bán kính 24. Đầu dây ngòi kết thúc ở (46, 8) để code gắn tia lửa vào | Bom trong trận, icon trên nút PLAY/START GAME, HUD | ☐ | ☐ |
| A09 | `spark.svg` | 0 0 32 32 | Tia lửa hình sao 6–8 cánh: lõi trắng, thân vàng `#FFD54A`, viền đỏ `#E53935`, tâm ở (16, 16) | Đầu ngòi bom (code làm nhấp nháy), trang trí | ☐ | ☐ |
| A10 | `flame_center.svg` | 0 0 64 64 | Tâm vụ nổ: khối lửa tròn nối ra cả 4 cạnh. Ở giữa mỗi cạnh, dải lửa chạm mép trong khoảng 13–51. Lõi trắng vàng `#FFF6C2`, giữa vàng `#FFC93C`, ngoài cam `#FF7A1A` | Ô tâm vụ nổ | ☐ | ☐ |
| A11 | `flame_mid.svg` | 0 0 64 64 | Đoạn lửa **nằm ngang**, chạm mép trái và mép phải trong khoảng y 13–51. Ghép nhiều ô liền nhau không thấy đường nối. Code xoay 90° cho chiều dọc | Thân tia lửa | ☐ | ☐ |
| A12 | `flame_end.svg` | 0 0 64 64 | Đầu mút: nối từ mép trái (y 13–51), thuôn tròn và kết thúc trước mép phải (x ≈ 56), **hướng sang phải**. Code xoay cho 3 hướng còn lại | Đầu tia lửa | ☐ | ☐ |

### 4.4 Icon minh họa nhiều màu – `icons/`

Icon đơn sắc (mũi tên, dấu ✔, ✖, home…) dùng font icon nên **không cần vẽ**. Chỉ vẽ các icon nhiều màu dưới đây.

| Mã | File | viewBox | Cần vẽ | Dùng ở | Tạm | Đẹp |
|---|---|---|---|---|---|---|
| A13 | `icon_trophy.svg` | 0 0 128 128 | Cúp vàng có ngôi sao | Nút LEADERBOARD, màn kết quả | ☐ | ☐ |
| A14 | `icon_history.svg` | 0 0 128 128 | Cuộn giấy kèm đồng hồ nhỏ. *Mẫu chưa có, cần thiết kế mới* | Nút HISTORY | ☐ | ☐ |
| A15 | `icon_gear.svg` | 0 0 128 128 | Bánh răng tím như mẫu 1 | Nút SETTINGS | ☐ | ☐ |
| A16 | `icon_help.svg` | 0 0 128 128 | Dấu hỏi vàng như mẫu 1 | Nút HELP | ☐ | ☐ |
| A17 | `icon_crown.svg` | 0 0 128 128 | Vương miện vàng | Badge HOST, hạng 1, chữ VICTORY | ☐ | ☐ |
| A18 | `icon_players.svg` | 0 0 128 128 | Nhóm 3 người như mẫu 4 | Tiêu đề ROOM LOBBY, ONLINE ROOMS | ☐ | ☐ |
| A19 | `icon_star.svg` | 0 0 64 64 | Ngôi sao vàng | Điểm số | ☐ | ☐ |
| A20 | `icon_fire.svg` | 0 0 64 64 | Ngọn lửa nhỏ màu cam | Tầm nổ trong HUD | ☐ | ☐ |
| A21 | `icon_timer.svg` | 0 0 64 64 | Đồng hồ bấm giờ | Đồng hồ HUD | ☐ | ☐ |
| A22 | `icon_skull.svg` | 0 0 64 64 | Đầu lâu trắng | Người chơi bị loại | ☐ | ☐ |

## 5. Nên có – 12 file

| Mã | File | viewBox | Cần vẽ | Nếu thiếu | Tạm | Đẹp |
|---|---|---|---|---|---|---|
| B01 | `characters/bomber_victory.svg` | 0 0 512 512 | Nhảy mừng, giơ cúp như mẫu 6 | Dùng A01 + A13 | ☐ | ☐ |
| B02 | `characters/bomber_defeat.svg` | 0 0 512 512 | Ngồi buồn hoặc choáng váng | A01 chuyển xám, nghiêng | ☐ | ☐ |
| B03 | `characters/bomber_dead.svg` | 0 0 128 128 | Hồn ma hoặc nằm bất tỉnh | A03 mờ xám + A22 | ☐ | ☐ |
| B04 | `tiles/block_border.svg` | 0 0 64 80 | Phiến đá viền quanh sân như mẫu 5, thấp và phẳng hơn A06 | Dùng A06 | ☐ | ☐ |
| B05 | `tiles/crate_debris.svg` | 0 0 32 32 | Một mảnh gỗ vỡ | Hình chữ nhật nâu | ☐ | ☐ |
| B06 | `icons/laurel.svg` | 0 0 128 256 | Nhành nguyệt quế **bên trái**; code lật cho bên phải | Bỏ | ☐ | ☐ |
| B07 | `bg/pattern_bomb.svg` | 0 0 160 160 | Họa tiết lặp: 3–4 quả bom và vài ngôi sao, nghiêng khác nhau, **chỉ một màu đen**, nền trong suốt. **Lặp liền mép**: hình nào cắt qua mép thì phần còn lại xuất hiện ở mép đối diện | Chỉ có gradient | ☐ | ☐ |
| B08 | `deco/tree.svg` | 0 0 128 128 | Cây tán tròn như mẫu 5 | Để trống | ☐ | ☐ |
| B09 | `deco/bush.svg` | 0 0 64 64 | Bụi cây | Để trống | ☐ | ☐ |
| B10 | `deco/flower.svg` | 0 0 32 32 | Hoa trắng nhụy vàng | Để trống | ☐ | ☐ |
| B11 | `deco/stump.svg` | 0 0 64 64 | Gốc cây | Để trống | ☐ | ☐ |
| B12 | `deco/rock.svg` | 0 0 64 64 | Tảng đá xám | Để trống | ☐ | ☐ |

B08–B12 dùng để trang trí hai bên sân. Sân thật chỉ có 13×11 ô, hẹp hơn mẫu, nên hai bên còn nhiều chỗ trống.

## 6. Tùy chọn

| Mã | File | Cần vẽ | Nếu thiếu |
|---|---|---|---|
| C01 | `characters/bomber_down_2.svg`, `bomber_up_2.svg`, `bomber_side_2.svg` (128) | Khung bước chân thứ hai cho mỗi hướng | Code làm nhún và co giãn |
| C02 | `bg/logo.svg` | Chữ BOMBERMAN ONLINE dạng hình | Dùng font Lilita One có viền |
| C03 | `icons/empty_rooms.svg` (256) | Minh họa khi chưa có phòng | Chỉ hiện chữ |
| C04 | `tiles/floor_grass.svg` (64) | Ô cỏ | Code vẽ caro 2 tông `#7CC04B` / `#72B545` |
| C05 | `icons/map_thumb.svg` (160×120) | Hình nhỏ trên thẻ ONLINE ROOMS | Code vẽ bản đồ thu nhỏ |
| C06 | `app_icon.svg` (256) | Icon cho file exe | Dùng A02 |

## 7. Font

| Mã | File | Nguồn | Dùng cho |
|---|---|---|---|
| F01 | `LilitaOne-Regular.ttf` | Google Fonts, "Lilita One" | Tiêu đề, nút, số |
| F02 | `Nunito-Black.ttf`, `Nunito-ExtraBold.ttf`, `Nunito-Bold.ttf` | Google Fonts, "Nunito" → thư mục `static/` trong gói tải về | Tên người chơi, tên phòng, nội dung |

- Cả hai font dùng giấy phép SIL Open Font License, được dùng miễn phí và đóng gói cùng app. Kèm file giấy phép: `fonts/OFL-LilitaOne.txt`, `fonts/OFL-Nunito.txt`.
- Nunito phải dùng **file tĩnh** (static), không dùng file variable (`Nunito[wght].ttf`), vì JavaFX không chọn được độ đậm trong font variable.
- **Lilita One không có dấu tiếng Việt**, nên không dùng cho chữ người dùng tự nhập.

## 8. Không cần file ảnh

**Làm bằng CSS hoặc code:** nút nổi, panel, khung, ô số liệu, chữ có viền, thanh READY, số slot, phím bấm trong Help, confetti, tia sáng xoay, bóng đổ, bụi dưới chân, hạt lửa, nền cỏ (mặc định), bục Leaderboard, badge HOST/WINNER/YOU.

**Dùng font icon (Ikonli):** mũi tên BACK, ✖, ➕, ‹ ›, home, replay (PLAY AGAIN), logout, dấu ✔, vòng tròn rỗng (NOT READY).

**Không cần vì tính năng đã ẩn** (xem [01](01-doi-chieu-mau-thiet-ke.md)): coin, gem, nút mua ➕, balo, 3 ảnh chế độ Solo/VS/Online, nhân vật hồng và đen, icon giày và thanh chỉ số, emoji và nút gửi của chat, icon mời bạn, copy mã phòng, bút chì, mũi tên đổi map, 4 vật phẩm (lửa, bom+, giày, đầu lâu), trái tim, badge MVP.

## 9. Cách nộp và kiểm tra

1. Đặt file vào `client-fx/src/main/resources/assets/svg/<nhóm>/` với đúng tên trong bảng. Trước khi module `client-fx` được tạo (giai đoạn 1), để tạm trong `img/assets/<nhóm>/`.
2. Chạy `.\gradlew :client-fx:run --args="--gallery"` để xem mọi asset ở kích thước thật:
   - Không bị thay bằng ô caro (nghĩa là file đọc được).
   - Điểm neo đúng: chân nhân vật chạm đáy ô, khối đá nhô lên ô trên đúng 16 đơn vị.
   - Nhân vật đổi đủ 4 màu, không sót mảng đỏ.
   - Tia lửa nối liền, không hở mép.
3. Đánh dấu ☑ ở cột **Đẹp**.

## 10. Tạo hình bằng AI rồi vector hóa

Các mẫu được tạo bằng AI, nên có thể tạo tiếp từng asset theo cùng phong cách:

1. **Tạo ảnh PNG** cho **từng asset riêng lẻ**, cỡ ít nhất 1024 px, nền trắng trơn. Đính kèm ảnh mẫu trong `img/` để AI bám phong cách. Mở đầu prompt bằng đoạn chung:
   > 2D game asset for a casual Bomberman game, cute chibi cartoon style matching the attached UI mockup. Thick dark navy outline (#2A1F3D), flat saturated colors with one darker shade and one soft highlight, no text, no drop shadow, no scenery, single object centered on a plain white background.

   Rồi thêm mô tả riêng:

   | Asset | Mô tả thêm |
   |---|---|
   | A01 | full-body bomber character in a 3/4 standing pose, red helmet with yellow headphones and a short fuse with a spark on top, winking and smiling, red suit, black belt with a gold buckle, white gloves, white-and-red shoes, holding a round black bomb |
   | A02 | only the head and helmet of the same character, facing forward, smiling |
   | A03 / A04 / A05 | small game sprite of the same character with a big head and short body, walking toward the camera / away from the camera / to the right, simple shapes readable at 48 pixels |
   | A06 | grey stone block for a top-down grid game, 3/4 view, lighter top face and darker front face, small cracks |
   | A07 | wooden crate with a diagonal plank, orange-brown, 3/4 view with a top face and a darker front face |
   | A08 | round black bomb with a highlight, grey cap and a curved beige fuse, no spark |
   | A09 | small star-shaped spark, yellow with a red outline and a white center |
   | A10 / A11 / A12 | cartoon fire for a grid explosion, white-yellow core, yellow middle, orange edge: a round burst reaching all four sides / a horizontal band touching the left and right edges / a band starting at the left edge and ending in a rounded tip |
   | A13–A22 | game UI icon: golden trophy with a star / paper scroll with a small clock / purple gear / yellow question mark / golden crown / group of three people / gold star / small orange flame / stopwatch / white skull |
   | B07 | seamless tile pattern of small bomb and star silhouettes, single black color |
   | B08–B12 | cartoon round tree / bush / white flower with yellow center / tree stump / grey rock |

2. **Vector hóa:** Inkscape (Path → Trace Bitmap, chế độ nhiều màu, 6–12 màu) hoặc công cụ vector hóa trực tuyến.
3. **Dọn file:** xóa nền trắng, gộp các màu gần giống, giảm điểm nút (Path → Simplify), đặt đúng `viewBox`, căn điểm neo, bỏ filter và chữ.
4. **Nhân vật:** tô lại phần màu đội bằng đúng 3 mã màu khóa (mục 3).
5. **Kiểm tra** bằng màn Gallery (mục 9).

**Nếu không vector hóa được:** app vẫn nhận PNG nền trong suốt cùng tên (ví dụ `icon_trophy.png`). PNG phải lớn ít nhất 4 lần viewBox với icon và tile (icon 128 → PNG 512 px), và 2 lần với ảnh 512. Nhân vật dạng PNG không đổi màu được, nên cần đủ 4 file cho mỗi tư thế: `bomber_full_red.png`, `bomber_full_blue.png`, `bomber_full_green.png`, `bomber_full_yellow.png`. Vẫn ưu tiên SVG vì sắc nét ở mọi kích thước.
