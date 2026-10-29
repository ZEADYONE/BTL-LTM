# 04 · Design system

Toàn bộ số liệu dưới đây được đo từ 6 mẫu rồi quy về khung logic **1280×720**. Khi code, định nghĩa chúng thành biến trong `css/game-theme.css` (looked-up color của JavaFX) và hằng số trong `ui/theme/`.

## 1. Đặc điểm phong cách (rút ra từ mẫu)

- Mọi thứ đều có **viền đậm màu tím than** `#2A1F3D`.
- Màu tươi, bão hòa. Mỗi khối gồm một màu chính, một màu tối hơn cho phần bóng, và một vệt sáng phía trên.
- Bo góc lớn. Nút và panel trông như khối nổi, có "cạnh đáy" tối màu bên dưới.
- Chữ trên nút và tiêu đề in hoa, rất đậm, màu trắng, có viền tối và bóng đổ cứng lệch xuống dưới (không nhòe).
- Nền cam có họa tiết bom mờ, đặt trong khung bo góc, ngoài cùng là nền tím tối.

## 2. Màu

### 2.1 Màu nền và bề mặt

| Token | Giá trị | Dùng cho |
|---|---|---|
| `outline` | `#2A1F3D` | Viền mọi thành phần, chữ tối |
| `bg-outer` | `#2B2140` | Nền tím ngoài khung; họa tiết trắng độ đục 6% |
| `frame-center` → `frame-edge` | `#FBB36B` → `#E8762C` (radial) | Khung cam chính; họa tiết đen độ đục 8% |
| `panel-purple` | `#6C45C9`, phần tiêu đề `#563299` | Tiêu đề, panel MAP, panel CHAT/STATUS |
| `panel-dark` | `#3A2D57` | Ô số liệu, nền ô trong panel tím |
| `panel-cream` | `#FFF1E6` | Thân popup, panel mô tả |
| `panel-brown` | `#B8581F`, ô bên trong `#9C4718` | Panel INFO ở Room Lobby |
| `text-light` | `#FFFFFF` | Chữ trên nền màu |
| `text-muted` | `#8B7FA8` | Chữ gợi ý, placeholder |
| `overlay` | `#1A1026`, độ đục 60% | Nền tối phía sau popup |

### 2.2 Màu nút

Mỗi màu gồm gradient mặt nút (trên → dưới) và màu cạnh đáy.

| Màu nút | Mặt nút | Cạnh đáy | Dùng cho |
|---|---|---|---|
| Vàng | `#FFE066` → `#FDB813` | `#C77F0A` | PLAY, START GAME, CONFIRM, PLAY AGAIN |
| Xanh dương | `#5AA9FF` → `#2F7BEA` | `#1F4FA8` | Nút menu, BACK, HOME, CANCEL |
| Xanh lá | `#6BD968` → `#38A83A` | `#1F6E24` | READY, JOIN, CREATE |
| Đỏ | `#FF6B6B` → `#E23C3C` | `#9E1F1F` | X, LEAVE, LOG OUT |
| Tím | `#8E63E8` → `#5F36B8` | `#3C1F80` | Thẻ ONLINE ROOMS, tab đang chọn |
| Xám | `#A7AABB` → `#8C8FA3` | `#5E6173` | Nút bị khóa, NOT READY |

### 2.3 Màu đội (nhân vật)

| Đội | Màu chính | Màu tối | Màu sáng |
|---|---|---|---|
| 1 · Đỏ | `#E53935` | `#B71C1C` | `#FF8A80` |
| 2 · Xanh dương | `#1E88E5` | `#0D47A1` | `#90CAF9` |
| 3 · Xanh lá | `#43A047` | `#1B5E20` | `#A5D6A7` |
| 4 · Vàng | `#FDD835` | `#F9A825` | `#FFF59D` |

Màu của đội Đỏ cũng là **3 màu khóa** trong file SVG nhân vật. Code thay 3 mã này để ra 3 đội còn lại (xem [05](05-tai-nguyen-svg-va-font.md) mục 3).

### 2.4 Màu trạng thái

| Ý nghĩa | Màu |
|---|---|
| Online: FREE / IN_ROOM / PLAYING | `#43C463` / `#F5B82E` / `#E84B4B` |
| Phòng: WAITING / PLAYING / FINISHED | `#43C463` / `#F28C28` / `#8C8FA3` |
| Kết quả: WIN | `#FFC93C` → `#F29F05` |
| Kết quả: LOSS | `#7A8BB8` → `#4E5D8C` |
| Kết quả: DRAW | `#D9DCE6` → `#A3A8BA` |
| Ô hạng 1 / 2 / 3 / còn lại | `#FFC93C` / `#C9CEDB` / `#E0894A` / `#A99BD6` |
| Toast info / success / error | `#6C45C9` / `#38A83A` / `#E23C3C` |

## 3. Chữ

| Kiểu | Font | Cỡ (px logic) | Dùng cho |
|---|---|---|---|
| `display` | Lilita One | 96 | VICTORY, DEFEAT, DRAW |
| `title` | Lilita One | 56 | Tiêu đề màn (ROOM LOBBY…) |
| `button-xl` | Lilita One | 60 | PLAY, START GAME |
| `button-l` | Lilita One | 28 | Nút menu, BACK, HOME, PLAY AGAIN |
| `button-m` | Lilita One | 22 | JOIN, CREATE, READY |
| `label` | Lilita One | 18 | Nhãn trong panel (GAME MODE…), nhãn P1 trong HUD |
| `number` | Lilita One | 22 | Số trong ô số liệu, HUD, điểm |
| `name` | Nunito Black | 22 | Username, tên phòng |
| `body` | Nunito ExtraBold | 17 | Nội dung Help, mô tả, danh sách |
| `caption` | Nunito Bold | 14 | Ghi chú nhỏ, thời gian |

- **Lilita One không có dấu tiếng Việt.** Mọi chữ do người dùng nhập (username, tên phòng) và mọi nội dung dài phải dùng **Nunito**.
- Chữ có viền dùng component `OutlinedText`: lớp chữ viền `#2A1F3D` dày 3–5 px phía sau, lớp chữ màu phía trước, cộng bóng cứng lệch xuống 4 px (độ đục 60%).

## 4. Kích thước và khoảng cách

### 4.1 Quy ước chung

| Mục | Giá trị |
|---|---|
| Lưới khoảng cách | 4 · 8 · 12 · 16 · 24 · 32 px |
| Viền | 3 px `#2A1F3D` (khung ngoài 4 px) |
| Bo góc | Khung 28 · Panel 20 · Thẻ 18 · Nút 16 · Ô nhập 14 · Pill 999 |
| Cạnh đáy nút | Cao 6 px (nút XL: 8 px) |
| Lề khung cam | Cách mép cửa sổ logic 64 px (trái/phải) và 36 px (trên/dưới) |

### 4.2 Kích thước tham chiếu (đo từ mẫu)

| Thành phần | Rộng × cao | Mẫu |
|---|---|---|
| Badge người chơi | 250 × 72 | 1 |
| Ô số liệu (pill) | 160 × 40 | 1 |
| Nút menu cột trái (`MenuTileButton`) | 250 × 72, cách nhau 26, chữ 24 px (220 px không đủ cho LEADERBOARD) | 1 |
| Vùng nhân vật lớn | 380 × 400 | 1 |
| Thẻ ONLINE ROOMS | 400 × 110 | 1 |
| Nút PLAY (`XL`) | 330 × 124 | 1 |
| Tiêu đề màn (`TitleBanner`) | cao 84, đệm ngang 32 | 3, 4 |
| Nút BACK (`L`) | 214 × 72 | 2, 3 |
| Slot người chơi (Room Lobby) | 287 × 187 | 4 |
| Thẻ người chơi HUD | 222 × 73 | 5 |
| Đồng hồ HUD | 206 × 73 | 5 |
| Dòng xếp hạng (Result) | 528 × 84 | 6 |
| Nút PLAY AGAIN / HOME | 330 × 88 | 6 |

### 4.3 Cỡ nút

| Cỡ | Cao | Font | Icon |
|---|---|---|---|
| `XL` | 120 | `button-xl` | 72 px, lấn ra khỏi mép trên như quả bom ở nút PLAY |
| `L` | 72 | `button-l` | 44 px |
| `M` | 56 | `button-m` | 32 px |
| `S` | 44 | `label` | 24 px |
| `ICON` | 56 × 56 | | 32 px (nút X, ⚙) |

## 5. Danh mục component

| Component | Biến thể / trạng thái | Cấu tạo | Dùng ở |
|---|---|---|---|
| `GameButton` | 6 màu × 5 cỡ. Trạng thái: thường, hover, nhấn, khóa, đang chờ | Lớp cạnh đáy, lớp mặt gradient, vệt sáng trắng 35% ở trên, viền 3 px, icon tùy chọn bên trái | Mọi màn |
| `MenuTileButton` | Xanh dương | Như `GameButton` cỡ L, icon minh họa lấn ra mép trái/trên | S2 cột trái |
| `TitleBanner` | `purple` (khối tím như ROOM LOBBY), `burst` (chỉ chữ, gạch vàng hai bên như SELECT MODE) | `OutlinedText` + icon | S3, S4, S7, S8 |
| `Panel` | `cream` (tiêu đề tối + thân kem), `purple`, `brown` | Tiêu đề + thân, viền 3 px | Popup, S4 |
| `Pill` | Có/không nút phụ (nút phụ đang ẩn) | Nền `panel-dark`, icon trái, số Lilita | S2 |
| `PlayerBadge` | | Ô avatar vuông bo góc nền màu đội + tên + dòng phụ | S2 |
| `PlayerHead` | 4 màu, xám (trống), mờ (bị loại) | `bomber_head.svg` đã đổi màu | Mọi nơi có avatar |
| `SlotCard` | Có người (READY / NOT READY), trống; thêm HOST, YOU | Số slot, nhân vật cắt nửa trên, thanh tên, thanh trạng thái | S4 |
| `RoomCard` | WAITING / PLAYING / FINISHED; JOIN bật/khóa | Tên, host, 4 đầu nhỏ, badge, nút | S3 |
| `Checklist` | Đạt ✔ (xanh) / chưa ✘ (xám) / chờ … | Vòng tròn icon + chữ | S4 ROOM STATUS |
| `HudPlayerCard` | Sống / bị loại | Avatar, tên, nhãn P, 💣 số, 🔥 số | S5 |
| `RankRow` | Hạng 1/2/3/khác, dòng của mình | Ô hạng, avatar, tên, ⭐ điểm, badge | S6, S7 |
| `TextField` | Thường, focus, lỗi | Nền kem, viền 3 px, bo 14; focus viền vàng; lỗi viền đỏ + chữ lỗi bên dưới | S1, S3, popup |
| `Tabs` | 2 tab | Nút liền nhau, tab chọn màu tím | S1 |
| `Toast` | info, success, error | Pill màu có icon | Mọi màn |
| `Modal` | | Lớp nền tối + `Panel` cream | Popup |
| `Confetti` | | Hình chữ nhật nhỏ nhiều màu, rơi có trọng lực | S6 WIN |
| `SunRays` | | Tia sáng xoay chậm phía sau nhân vật | S2, S6 |

## 6. Animation

| Hiệu ứng | Thời gian | Easing |
|---|---|---|
| Chuyển màn | 200 ms: mờ dần + trượt 16 px | ease-out |
| Hover nút | 120 ms, phóng to 1.03, sáng hơn 5% | ease-out |
| Nhấn nút | 60 ms lún xuống 4 px (cạnh đáy mỏng lại), 100 ms nảy về | linear / ease-out |
| Mở popup | 180 ms: phóng 0.9 → 1 + hiện dần | back-out |
| Toast | Hiện 150 ms, giữ 3 s (lỗi 4 s), ẩn 200 ms | ease-out |
| Nhân vật lớn nhún | Vòng lặp 1.6 s, lên xuống 6 px, co giãn 3% | sine |
| Tia sáng | Xoay 360° trong 20 s | linear |
| Tiêu đề kết quả | 450 ms: 0.3 → 1.1 → 1 | back-out |
| Dòng xếp hạng kết quả | Lần lượt cách nhau 80 ms, trượt từ phải vào 40 px | ease-out |
| Confetti | 80 mảnh trong 2.5 s | Trọng lực |

Hiệu ứng trong trận (bom, nổ, mảnh vỡ…) nằm ở [06](06-gameplay-va-hieu-ung.md).
