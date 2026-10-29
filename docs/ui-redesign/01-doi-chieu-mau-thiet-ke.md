# 01 · Đối chiếu mẫu thiết kế với chức năng hiện có

Mỗi phần tử trong 6 mẫu ở `img/` được xếp vào một trong ba loại:

| Ký hiệu | Ý nghĩa |
|---|---|
| ✅ Giữ | Server đã có dữ liệu hoặc chức năng, làm đúng như mẫu |
| 🔁 Thay | Chức năng trong mẫu chưa có, nhưng vị trí đó hiển thị dữ liệu hoặc chức năng **đang có** |
| 🙈 Ẩn | Chưa có chức năng nên không hiển thị. Ghi lại để làm sau |

Mã phần tử (`H-01`, `R-03`…) dùng để tham chiếu từ các tài liệu khác và khi review.

---

## Mẫu 1 · Home

<img src="../../img/ChatGPT%20Image%20Sep%2026,%202026,%2008_48_55%20PM-1.png" width="640" alt="Mẫu 1 - Home">

| Mã | Phần tử trong mẫu | Xử lý | Hiển thị thực tế | Nguồn dữ liệu / lý do |
|---|---|---|---|---|
| H-01 | Badge PLAYER: avatar + tên | ✅ | Đầu nhân vật + username | `LoginResponse.username` |
| H-02 | "Lv. 12" + thanh XP | 🔁 | "RANK #3 · 12.5 PTS", ẩn thanh XP. Chưa có trận nào thì "UNRANKED" | `RANKING_RESPONSE`, server trả về toàn bộ người chơi nên luôn tìm được hạng của mình |
| H-03 | Ô Coin 1,250 + nút ➕ | 🔁 | Ô ⭐ tổng điểm, ẩn nút ➕ | `RankingEntryDto.totalScoreUnits ÷ 2` |
| H-04 | Ô Gem 120 + nút ➕ | 🔁 | Ô "● 5 ONLINE", ẩn nút ➕. Bấm vào mở Room Browser | `ONLINE_USERS_UPDATE` |
| H-05 | Nút CHARACTERS | 🔁 | Nút **LEADERBOARD** (icon cúp) | Chưa có chọn nhân vật. Leaderboard đã có nhưng mẫu chưa có lối vào |
| H-06 | Nút INVENTORY | 🔁 | Nút **HISTORY** (icon cuộn giấy) | Chưa có túi đồ. History đã có nhưng mẫu chưa có lối vào |
| H-07 | Nút SETTINGS | ✅ | Popup Settings | Client |
| H-08 | Nút HELP | ✅ | Popup Help | Client |
| H-09 | Nhân vật lớn giữa màn | ✅ | `bomber_full` theo màu đại diện, nhún nhẹ | Asset |
| H-10 | Thẻ GAME MODE "Classic · Battle · Custom" | 🔁 | Thẻ **ONLINE ROOMS** "3 OPEN · 5 ONLINE", bấm vào mở Room Browser | `ROOM_LIST_UPDATE`, `ONLINE_USERS_UPDATE` |
| H-11 | Nút PLAY | 🔁 | **Quick Play**, thuật toán ở [02](02-man-hinh-va-dieu-huong.md) mục 3.2 | `JOIN_ROOM` / `CREATE_ROOM` |
| H-12 | Nền cam có họa tiết bom, khung tím bên ngoài | ✅ | Như mẫu | CSS + `pattern_bomb.svg` |

---

## Mẫu 2 · Select Mode — 🙈 ẩn cả màn

<img src="../../img/ChatGPT%20Image%20Sep%2026,%202026,%2008_48_58%20PM-2.png" width="640" alt="Mẫu 2 - Select Mode">

| Mã | Phần tử trong mẫu | Xử lý | Lý do |
|---|---|---|---|
| M-01 | Thẻ SOLO (đấu với AI) | 🙈 | Server không có bot |
| M-02 | Thẻ VS BATTLE (chơi chung một máy) | 🙈 | Chưa có chế độ nhiều người trên một máy |
| M-03 | Thẻ ONLINE ROOM | 🔁 | Là chế độ duy nhất, nên nút PLAY đi thẳng vào phòng mà không cần chọn |
| M-04 | Nút BACK / NEXT | 🙈 | Không có màn này |

**Dùng lại từ mẫu này:**
- Kiểu thẻ lớn (hình ở trên, dải màu tiêu đề ở dưới) → thẻ phòng trong Room Browser.
- Tiêu đề chữ lớn có gạch vàng hai bên → `TitleBanner` biến thể "burst".

**Khi có thêm chế độ chơi:** chèn màn này vào giữa nút PLAY và màn phòng.

---

## Mẫu 3 · Character Select — 🙈 ẩn cả màn

<img src="../../img/ChatGPT%20Image%20Sep%2026,%202026,%2008_48_59%20PM-3.png" width="640" alt="Mẫu 3 - Character Select">

| Mã | Phần tử trong mẫu | Xử lý | Lý do |
|---|---|---|---|
| C-01 | Dải chọn 6 nhân vật | 🙈 | Server không lưu nhân vật/skin. Mỗi phòng tối đa 4 người |
| C-02 | Panel tên + mô tả nhân vật | 🙈 | |
| C-03 | Chỉ số SPEED / BOMBS / POWER | 🙈 | Mọi người chơi có chỉ số giống nhau: 1 bom, tầm nổ 2 ô |
| C-04 | Nút CONFIRM | 🙈 | |

**Thay thế:** màu nhân vật được gán tự động theo vị trí trong phòng (xem [03](03-kien-truc-ky-thuat.md) mục 8). Chỉ cần 4 màu, không cần nhân vật hồng và đen như trong mẫu.

**Dùng lại từ mẫu này:**
- Panel tiêu đề tối + thân màu kem → khuôn cho mọi popup.
- Khung chọn màu vàng ở 4 góc → hiệu ứng hover/focus của thẻ phòng.

---

## Mẫu 4 · Room Lobby

<img src="../../img/ChatGPT%20Image%20Sep%2026,%202026,%2008_49_01%20PM-4.png" width="640" alt="Mẫu 4 - Room Lobby">

| Mã | Phần tử trong mẫu | Xử lý | Hiển thị thực tế | Nguồn dữ liệu / lý do |
|---|---|---|---|---|
| R-01 | Tiêu đề ROOM LOBBY + icon nhóm người | ✅ | | |
| R-02 | Ô ROOM NAME | ✅ | Tên phòng, chỉ đọc | `RoomStateDto.roomName` |
| R-03 | Nút bút chì sửa tên | 🙈 | | Server không có chức năng đổi tên phòng |
| R-04 | ROOM CODE + nút copy | 🙈 | | `roomId` là UUID dài 36 ký tự, không có chức năng vào phòng bằng mã |
| R-05 | 4 slot: số thứ tự, nhân vật, tên | ✅ | | `RoomStateDto.players` |
| R-06 | Thanh READY / NOT READY | ✅ | | `RoomPlayerDto.ready` |
| R-07 | Vương miện HOST | ✅ | | `RoomStateDto.hostUserId` |
| R-08 | *(mẫu không có)* Slot trống | ✅ thêm | Viền nét đứt, bóng mờ, chữ "WAITING…" | |
| R-09 | *(mẫu không có)* Đánh dấu slot của mình | ✅ thêm | Nhãn nhỏ "YOU" | |
| R-10 | Panel MAP với ảnh map "GRASSLANDS" | ✅ | "CLASSIC ARENA" + ảnh xem trước vẽ bằng tile thật, đánh dấu điểm xuất phát của từng slot | Map của server cố định, tính trước được nên ảnh khớp 100% với map thật |
| R-11 | Mũi tên đổi map | 🙈 | | Chỉ có 1 map |
| R-12 | GAME MODE: CLASSIC | ✅ | Chữ tĩnh | Chỉ có 1 chế độ |
| R-13 | WIN CONDITION: 3 WINS | 🔁 | "LAST ONE STANDING" | Luật thật: một ván, người sống sót cuối cùng thắng |
| R-14 | MAX PLAYERS: 4 | ✅ | | `RoomStateDto.maxPlayers` |
| R-15 | ITEMS: ON | 🙈 | | Chưa có vật phẩm |
| R-16 | Khung CHAT, ô nhập, emoji, nút gửi | 🔁 | Panel **ROOM STATUS**: danh sách điều kiện để bắt đầu | Chưa có chat. Danh sách suy ra từ `ROOM_STATE` |
| R-17 | Nút INVITE | 🔁 | Nút **READY / UNREADY** | Chưa có mời bạn. READY bắt buộc với mọi người, kể cả host |
| R-18 | Nút START GAME | ✅ | Chỉ host bấm được, điều kiện ở [02](02-man-hinh-va-dieu-huong.md) mục 3.4 | `START_GAME` |
| R-19 | Nút X | ✅ | Rời phòng, có popup xác nhận | `LEAVE_ROOM` |
| R-20 | Ô Coin/Gem | 🙈 | | |

---

## Mẫu 5 · Trong trận

<img src="../../img/ChatGPT%20Image%20Sep%2026,%202026,%2008_49_03%20PM-5.png" width="640" alt="Mẫu 5 - Trong trận">

| Mã | Phần tử trong mẫu | Xử lý | Hiển thị thực tế | Nguồn dữ liệu / lý do |
|---|---|---|---|---|
| G-01 | Thẻ P1–P4: avatar | ✅ | Thẻ của slot không có người thì ẩn | Vị trí trong danh sách người chơi |
| G-02 | Tên "P1" | 🔁 | Username (cắt bớt nếu dài) + nhãn nhỏ P1–P4 | `GamePlayerStateDto.username` |
| G-03 | Tim ❤❤❤ | 🔁 | Trạng thái sống, hoặc "OUT" (thẻ xám + đầu lâu) | Không có mạng: trúng vụ nổ là bị loại |
| G-04 | 💣 số bom | ✅ | Số bom còn đặt được | `bombCapacity − activeBombs` |
| G-05 | 🔥 tầm nổ | ✅ | | `bombRange` |
| G-06 | Đồng hồ đếm ngược 02:15 | 🔁 | Đồng hồ **đếm lên** thời gian đã chơi | `tick ÷ 20`. Server không giới hạn thời gian trận |
| G-07 | Nền cỏ caro | ✅ | | Ô `EMPTY` |
| G-08 | Khối đá | ✅ | | Ô `HARD_WALL` (tường bao + cột bên trong) |
| G-09 | Thùng gỗ | ✅ | | Ô `BREAKABLE_WALL` |
| G-10 | Bụi cây dạng khối trong lưới | 🙈 | | Server chỉ có 3 loại ô. Thêm hình khác dễ khiến người chơi tưởng đó là loại ô khác |
| G-11 | Vật phẩm: lửa, bom+, giày, đầu lâu | 🙈 | | Chưa có power-up |
| G-12 | Bom có tia lửa ở ngòi | ✅ | | `bombs[].remainingFuseMillis` |
| G-13 | Vụ nổ hình chữ thập | ✅ | | `explosions[].origin`, `affectedPositions`, `remainingMillis` |
| G-14 | Mảnh gỗ văng | ✅ | | So sánh map giữa 2 snapshot liên tiếp |
| G-15 | Bụi dưới chân khi chạy | ✅ | | Hiệu ứng phía client |
| G-16 | Cây, hoa, gốc cây ngoài sân | ✅ | Trang trí | Sân thật 13×11 ô, hẹp hơn mẫu (khoảng 17×11) nên hai bên cần lấp |
| G-17 | *(mẫu không có)* Thoát trận | ✅ thêm | Phím ESC mở menu nhỏ: RESUME / HELP / LEAVE MATCH | `LEAVE_ROOM`. Server xử lý như mất kết nối: nhân vật bị loại |

---

## Mẫu 6 · Victory

<img src="../../img/ChatGPT%20Image%20Sep%2026,%202026,%2008_49_06%20PM-6.png" width="640" alt="Mẫu 6 - Victory">

| Mã | Phần tử trong mẫu | Xử lý | Hiển thị thực tế | Nguồn dữ liệu / lý do |
|---|---|---|---|---|
| V-01 | Chữ VICTORY + vương miện + nhành nguyệt quế | ✅ | Thêm 2 biến thể **DEFEAT** và **DRAW** | `GameOverPlayerDto.result` của mình |
| V-02 | Nhân vật cầm cúp | ✅ | Thắng: cầm cúp. Thua: dáng buồn. Hòa: dáng thường | Asset |
| V-03 | Confetti, tia sáng phía sau | ✅ | Confetti chỉ hiện khi thắng | |
| V-04 | Danh sách hạng 1–4: số hạng, avatar, tên | ✅ | Người thắng đứng đầu, sau đó ai bị loại muộn hơn xếp trên | Client tự ghi thứ tự bị loại từ các snapshot |
| V-05 | ⭐ 24 | 🔁 | Điểm nhận được ở trận này: +1 / +0.5 / +0 | `scoreEarnedUnits ÷ 2` |
| V-06 | Badge MVP | 🔁 | Badge WINNER, hoặc DRAW khi hòa | Không có thống kê MVP |
| V-07 | Nút PLAY AGAIN | ✅ | | `PLAY_AGAIN` |
| V-08 | Nút HOME | ✅ | Rời phòng rồi về Home | `LEAVE_ROOM` |
| V-09 | *(mẫu không có)* Tranh chấp Play Again | ✅ thêm | Xem [02](02-man-hinh-va-dieu-huong.md) mục 3.6 | Một người bấm PLAY AGAIN thì server đưa cả phòng về trạng thái chờ |

---

## Tổng hợp phần bị ẩn (để làm sau)

| Chức năng | Phần tử đang bị ẩn | Server cần bổ sung |
|---|---|---|
| Tiền tệ, level | H-02 (thanh XP), nút ➕ ở H-03/H-04, R-20 | Lưu dữ liệu tiền/kinh nghiệm, thưởng sau trận |
| Chọn nhân vật | Mẫu 3, nút CHARACTERS (H-05) | Lưu skin theo user, gửi kèm trong DTO phòng và trận |
| Túi đồ | Nút INVENTORY (H-06) | Hệ thống vật phẩm sở hữu |
| Chế độ chơi khác | Mẫu 2, thẻ GAME MODE (H-10) | Bot AI, chế độ chơi chung máy |
| Đổi tên phòng, mã phòng | R-03, R-04 | Thông điệp đổi tên; mã phòng ngắn và vào phòng bằng mã |
| Nhiều map | R-11 | Nhiều layout map, chọn map trong phòng |
| Chat, mời bạn | R-16, R-17 | Thông điệp CHAT/INVITE và broadcast |
| Vật phẩm, mạng | G-11, R-15, G-03 | Power-up, số mạng |
| MVP | V-06 | Thống kê trong trận (số người bị hạ…) |
