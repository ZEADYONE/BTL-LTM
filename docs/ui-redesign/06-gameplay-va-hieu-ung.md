# 06 · Gameplay và hiệu ứng

Đặc tả màn chơi S5: luật server mà client phải tôn trọng, điều khiển, cách làm chuyển động mượt, thứ tự vẽ và hiệu ứng. Mọi thay đổi chỉ nằm ở client.

## 1. Luật server liên quan

| Luật | Chi tiết | Ảnh hưởng tới client |
|---|---|---|
| Nhịp server | 20 tick/giây. Snapshot gửi mỗi 2 tick (10 lần/giây); snapshot có kết quả trận luôn được gửi | Cần nội suy để chuyển động mượt |
| Di chuyển ([movePlayer](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L93)) | Mỗi `MOVE` đi đúng 1 ô. Server **không giới hạn** tốc độ gửi `MOVE` | Tốc độ đi do nhịp lặp phím của client quyết định |
| Va chạm | Không đi vào: ngoài map, ô đá, ô thùng, ô có bom, ô có người còn sống. Đang đứng trên bom vừa đặt thì vẫn bước ra được | Server từ chối thì nhân vật đứng yên, client không cần tự kiểm tra |
| Đặt bom | Tại ô đang đứng, tối đa `bombCapacity` quả cùng lúc (mặc định 1), không đặt chồng | |
| Ngòi bom | 3000 ms | Client tự đếm ngược giữa các snapshot |
| Vụ nổ ([createExplosion](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L326)) | Lan tối đa `bombRange` ô (mặc định 2) theo mỗi hướng. Dừng **trước** ô đá. Gặp thùng: phá thùng, tính cả ô đó rồi dừng. Gặp bom khác: kích nổ bom đó, tính cả ô đó rồi dừng | Hình chữ thập không đều, cần phân loại mảnh lửa (mục 7) |
| Hạ gục ([killPlayersIn](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L377)) | Chỉ người đứng trong vùng nổ **ngay lúc nổ** bị loại. Lửa còn hiện 500 ms nhưng **không gây hại thêm** | Hiệu ứng phải làm rõ khoảnh khắc nổ (chớp sáng lúc đầu), sau đó chỉ mờ dần |
| Kết thúc ([determineOutcome](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L386)) | Sau khi có người bị loại: còn đúng 1 người sống thì người đó thắng; không còn ai thì hòa | |
| Rời trận ([disconnectPlayer](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L170)) | `LEAVE_ROOM` hoặc mất kết nối giữa trận thì bị loại ngay | Menu ESC phải cảnh báo |
| Map mặc định | Cố định, xem [03](03-kien-truc-ky-thuat.md) mục 9 | Dựng được ảnh xem trước ở Room Lobby |

## 2. Điều khiển

| Phím | Hành động |
|---|---|
| W / ↑ | Đi lên |
| S / ↓ | Đi xuống |
| A / ← | Đi trái |
| D / → | Đi phải |
| Space | Đặt bom |
| Esc | Mở menu trận |
| F11 | Bật/tắt toàn màn hình |
| F3 | *(Chế độ dev)* Hiện FPS |

**Quy tắc**
- Giữ một danh sách hướng đang được nhấn. Nhấn phím thì đưa hướng đó lên đầu danh sách (nếu đã có thì chuyển lên đầu); nhả phím thì bỏ khỏi danh sách. Hướng đang có hiệu lực là hướng ở đầu danh sách.
- Khi hướng hiệu lực thay đổi (nhấn phím mới, hoặc nhả phím đầu mà vẫn còn giữ phím khác): gửi `MOVE` **ngay**, sau đó lặp lại mỗi `MOVE_REPEAT_MS = 140` ms khi còn giữ.
- Nhịp lặp được tính trong `AnimationTimer` theo `System.nanoTime()`, không dùng `Timeline`, để không bị lệch.
- Space chỉ đặt một quả bom cho mỗi lần nhấn. Bỏ qua sự kiện lặp phím do hệ điều hành tự sinh (dựa vào tập phím đang giữ).
- Không gửi gì khi: trận chưa ở trạng thái RUNNING, mình đã bị loại, menu ESC hoặc popup đang mở.
- Cửa sổ mất focus thì xóa danh sách phím đang giữ.
- 140 ms tương đương khoảng 7 ô/giây. Đây là hằng số, sẽ tinh chỉnh khi chơi thử ở giai đoạn 5.

## 3. Chuyển động mượt

- Mỗi người chơi có **vị trí hiển thị** (số thực, đơn vị ô) và **vị trí đích** (ô trong snapshot mới nhất).
- Mỗi frame, vị trí hiển thị tiến về đích với tốc độ không đổi `VISUAL_SPEED = 1 ô / 130 ms`, nhanh hơn nhịp lặp phím một chút để không bị tụt lại phía sau.
- Cách đích hơn 1 ô thì tăng tốc gấp đôi để đuổi kịp. Cách hơn 2.5 ô (do mất snapshot) thì nhảy thẳng tới đích.
- **Hướng nhìn** theo lần đổi ô gần nhất. Đứng yên thì giữ hướng cũ. Mặc định nhìn xuống.
- **Dáng bước:** khi đang di chuyển, sprite nhún 2 px theo chu kỳ 260 ms và nghiêng ±3°. Nếu có khung bước thứ hai (C01) thì đổi khung thay vì nhún.
- Người chơi khác dùng cùng cách tính.
- Không làm dự đoán phía client, lý do ở mục 10.

## 4. Bố cục sân

- Sân 13 × 11 ô. Vùng dành cho sân = cửa sổ trừ HUD (cao 96 px logic, quy ra pixel thật) và lề 16 px.
- Cỡ ô `T = floor(min(rộng / 13, cao / 11.25))`. Phần 0.25 ô dư dành cho khối ở hàng trên cùng nhô lên.
- Sân căn giữa. Hai bên đặt trang trí (B08–B12) theo vị trí giả ngẫu nhiên với seed cố định, để trang trí không nhảy lung tung khi đổi kích thước cửa sổ.
- Nền cam, họa tiết, cỏ và trang trí được vẽ một lần thành ảnh nền, chỉ vẽ lại khi đổi kích thước.

## 5. Thứ tự vẽ mỗi frame

1. Ảnh nền đã cache (nền cam, trang trí, cỏ).
2. Bóng: hình ellipse mờ dưới bom và dưới chân người chơi.
3. Lần lượt từng hàng, từ hàng 0 đến hàng 10:
   1. Khối đá và thùng ở hàng đó.
   2. Bom ở hàng đó.
   3. Người chơi có vị trí hiển thị thuộc hàng đó, sắp theo tọa độ y.

   Nhờ vậy người ở hàng dưới che khối ở hàng trên, còn khối ở hàng dưới che chân người ở hàng trên.
4. Lửa của các vụ nổ.
5. Hạt: mảnh gỗ, bụi, tia lửa.
6. Tên người chơi trên đầu (Nunito, có viền). Nếu ô quá nhỏ (T < 40 px) thì chỉ hiện nhãn P1–P4.
7. Chữ lớn giữa màn (GO!…).

HUD và popup nằm trong cây giao diện JavaFX phía trên Canvas, không vẽ lên Canvas.

## 6. Hiệu ứng

| Hiệu ứng | Kích hoạt | Mô tả | Thời gian |
|---|---|---|---|
| Bom nhấp nhô | Có bom trên sân | Phóng to/thu nhỏ ±6%. Tần số tăng từ 2 Hz lên 8 Hz khi ngòi ngắn dần. Giây cuối phủ đỏ tăng dần (độ đục 0 → 45%) | Theo ngòi |
| Tia lửa ở ngòi | Có bom trên sân | `spark.svg` ở đầu ngòi, xoay một vòng mỗi 0.6 s, phóng 0.8–1.2 | Lặp |
| Đếm ngòi | Mỗi snapshot | Còn lại = `remainingFuseMillis` − (bây giờ − lúc nhận snapshot). Đồng bộ lại ở mỗi snapshot | |
| Vụ nổ | Có vụ nổ mới | Ghép mảnh lửa (mục 7). 0–80 ms phóng 0.6 → 1.1; 80–150 ms về 1.0; sau đó mờ dần theo `remainingMillis`. Lõi trắng chớp sáng trong 80 ms đầu | 500 ms |
| Rung màn hình | Có vụ nổ mới | Lệch ngẫu nhiên tối đa 3 px logic, giảm dần. Tắt được | 150 ms |
| Thùng vỡ | Ô đổi từ `BREAKABLE_WALL` sang `EMPTY` | 5 mảnh `crate_debris` văng tỏa, xoay, rơi theo trọng lực, mờ dần | 400 ms |
| Bụi chân | Người chơi bắt đầu sang ô mới | 2–3 vòng tròn trắng phía sau chân, độ đục 50% → 0 | 250 ms |
| Bị loại | `alive` đổi từ true sang false | Nháy trắng 2 lần (mỗi lần 100 ms), đổi sang `bomber_dead` (hoặc sprite xám), bay lên 12 px, mờ còn 35% | 600 ms |
| GO! | Snapshot RUNNING đầu tiên | Chữ "GO!" phóng to rồi mờ. Chỉ để trang trí, không chặn phím | 900 ms |
| Người thắng | Nhận `GAME_OVER` | Nhân vật thắng nhảy 2 lần trước khi sang màn kết quả | 1200 ms |

## 7. Phân loại mảnh lửa

Đầu vào: tâm `O` (`origin`) và tập ô `A` (`affectedPositions`) của một vụ nổ.

- Ô `O` dùng `flame_center`.
- Mỗi ô `p ≠ O` luôn cùng hàng hoặc cùng cột với `O`. Gọi `d` là hướng đơn vị từ `O` tới `p`:
  - `p + d` không thuộc `A` → `flame_end`;
  - ngược lại → `flame_mid`.
- Góc xoay theo `d`: phải 0°, xuống 90°, trái 180°, lên 270°.
- Nhiều vụ nổ chồng lên nhau thì vẽ tất cả, không cần gộp.

Ví dụ (tầm nổ 2):
- Giữa sân trống: 1 `center`, 4 `mid`, 4 `end`.
- Ô đá nằm ngay bên phải tâm: hướng phải không có mảnh nào.
- Thùng nằm ngay bên trái tâm: hướng trái chỉ có 1 mảnh `end`, đúng tại ô thùng.

## 8. Bộ theo dõi trận (`MatchTracker`)

| Việc | Cách làm |
|---|---|
| Nhận biết trận mới | Nhận `ROOM_STATE` PLAYING, hoặc `tick` của snapshot mới nhỏ hơn snapshot trước → xóa dữ liệu cũ |
| Chốt màu | Từ snapshot đầu tiên: `userId → slot` theo thứ tự `players` |
| Thứ tự bị loại | So sánh `alive` giữa 2 snapshot liên tiếp, ghi lại `(userId, tick)`. Bị loại trong cùng snapshot thì cùng hạng |
| Thùng vỡ | So sánh map giữa 2 snapshot, lấy danh sách ô vừa vỡ |
| Vụ nổ mới | Vụ nổ có `origin` chưa xuất hiện ở snapshot trước |
| Bom mới | `bombId` chưa từng thấy |
| Dữ liệu cho màn kết quả | Tên, slot, thứ tự bị loại → xếp hạng theo [02](02-man-hinh-va-dieu-huong.md) mục 3.6 |

## 9. Hiệu năng

- Mục tiêu 60 FPS, thời gian vẽ mỗi frame dưới 4 ms trên GPU tích hợp.
- Không tạo ảnh mới trong vòng lặp vẽ; mọi sprite lấy từ cache.
- Hạt dùng pool, tối đa 200 hạt cùng lúc.
- Nạp trước sprite khi vào Room Lobby ([03](03-kien-truc-ky-thuat.md) mục 7).
- F3 (chế độ dev) hiện FPS và thời gian vẽ để đo.

## 10. Không làm và lý do

| Việc | Lý do |
|---|---|
| Dự đoán phía client (client-side prediction) | Giao thức không có số thứ tự input nên không đối chiếu được với server. Tự đoán sẽ khiến nhân vật bị kéo giật lùi |
| Bù độ trễ | Cần sửa server |
| Giới hạn tốc độ di chuyển | Cần sửa server. Hiện một client bị sửa đổi có thể đi nhanh hơn (rủi ro R6 trong [08](08-kiem-thu-va-rui-ro.md)) |

**Nếu được phép sửa server (quyết định D10):** gửi snapshot ở mọi tick thay vì cách 2 tick, chỉ cần sửa một dòng ở [RoomGameLoop.java:90](../../server/src/main/java/com/bomberman/server/game/RoomGameLoop.java#L90). Phản hồi nhanh hơn khoảng 50 ms, băng thông gấp đôi nhưng vẫn nhỏ với 4 người chơi.
