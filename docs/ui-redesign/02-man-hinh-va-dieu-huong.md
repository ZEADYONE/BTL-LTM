# 02 · Màn hình và điều hướng

Tài liệu này đặc tả từng màn hình (kể cả các màn không có mẫu), luồng chuyển màn, cách client phản ứng với thông điệp từ server, và các thông báo lỗi.

Quy ước:
- Chữ hiển thị trên UI viết bằng tiếng Anh, in hoa như mẫu (quyết định D8).
- Wireframe vẽ trên khung logic 1280×720 (xem [03](03-kien-truc-ky-thuat.md) mục 6). Wireframe chỉ thể hiện bố cục; màu sắc và kích thước lấy theo mẫu và [04](04-design-system.md).
- Mã phần tử `H-xx`, `R-xx`… tham chiếu tới [01](01-doi-chieu-mau-thiet-ke.md).

---

## 1. Danh sách màn hình

| Mã | Màn | Mẫu | Mở khi |
|---|---|---|---|
| S1 | Login / Register | Không có | Mở app, đăng xuất, mất kết nối |
| S2 | Home | Mẫu 1 | Đăng nhập thành công, rời phòng, BACK từ màn con |
| S3 | Room Browser | Không có | Thẻ ONLINE ROOMS hoặc ô ONLINE ở Home |
| S4 | Room Lobby | Mẫu 4 | Tạo/vào phòng thành công, PLAY AGAIN |
| S5 | Game | Mẫu 5 | Phòng chuyển sang PLAYING |
| S6 | Result | Mẫu 6 | 1.2 giây sau khi nhận `GAME_OVER` |
| S7 | Leaderboard | Không có | Nút LEADERBOARD ở Home |
| S8 | History | Không có | Nút HISTORY ở Home |

## 2. Luồng điều hướng

```
S1 Login        ──đăng nhập────────────────► S2 Home
S2 Home         ──PLAY (Quick Play)────────► S4 Room Lobby
S2 Home         ──ONLINE ROOMS / ô ONLINE──► S3 Room Browser ──JOIN / CREATE──► S4 Room Lobby
S2 Home         ──LEADERBOARD──────────────► S7 Leaderboard  ──BACK──► S2 Home
S2 Home         ──HISTORY──────────────────► S8 History      ──BACK──► S2 Home
S2 Home         ──SETTINGS › LOG OUT───────► S1 Login
S4 Room Lobby   ──host bấm START───────────► S5 Game (cả phòng)
S4 Room Lobby   ──X (rời phòng)────────────► S2 Home
S5 Game         ──GAME_OVER + 1.2 giây─────► S6 Result
S5 Game         ──ESC › LEAVE MATCH────────► S2 Home
S6 Result       ──PLAY AGAIN / BACK TO ROOM► S4 Room Lobby
S6 Result       ──HOME (rời phòng)─────────► S2 Home
Mọi màn         ──mất kết nối──────────────► S1 Login + popup "CONNECTION LOST"
```

---

## 3. Đặc tả từng màn

### 3.1 S1 · Login / Register (không có mẫu)

```
+----------------------------------------------------------------+
|                                                                |
|  BOMBERMAN ONLINE               +----------------------------+ |
|  (logo chữ có viền)             |  [ LOGIN ]  [ REGISTER ]   | |
|                                 +----------------------------+ |
|        +-------------+          |  USERNAME                  | |
|        |  nhân vật   |          |  [______________________]  | |
|        | bomber_full |          |  PASSWORD                  | |
|        | (nhún nhẹ)  |          |  [______________________]  | |
|        +-------------+          |                            | |
|                                 |  [        LOGIN         ]  | |
|                                 +----------------------------+ |
|  (o) 127.0.0.1:8081  [gear]                                    |
+----------------------------------------------------------------+
```

**Hành vi**
- Hai tab LOGIN và REGISTER. Tab REGISTER có thêm ô CONFIRM PASSWORD, nút lớn đổi thành REGISTER.
- Nhấn Enter trong ô nhập tương đương bấm nút lớn.
- Kiểm tra dữ liệu trước khi gửi theo mục 6.1. Sai thì báo ngay dưới ô nhập, không gửi lên server.
- Bấm gửi thì nút chuyển sang trạng thái chờ (có vòng xoay), khóa cho tới khi có phản hồi hoặc hết 5 giây (mục 5.2).
- Góc dưới trái hiện địa chỉ server. Chấm tròn màu xám nghĩa là chưa kết nối, màu xanh là đã kết nối. Nút bánh răng mở popup **Server Address** (mục 4.2).
- Đăng ký thành công: toast "Account created. You can log in now.", chuyển sang tab LOGIN, giữ nguyên username, xóa mật khẩu.
- Username đăng nhập thành công lần trước được điền sẵn. Không lưu mật khẩu.
- Kết nối chỉ mở khi gửi yêu cầu đầu tiên, giống client hiện tại.

**Nghiệm thu**
- [ ] Đăng nhập đúng → sang Home.
- [ ] Sai mật khẩu, tài khoản đang online nơi khác, username trống → hiện đúng thông báo ở mục 6.
- [ ] Server tắt → toast "Cannot connect to <host>:<port>." và nút mở khóa lại.
- [ ] Username tiếng Việt có dấu hiển thị đúng.

### 3.2 S2 · Home (mẫu 1)

```
+----------------------------------------------------------------+
| [av] PLAYER_NAME                         [* 12.5] [o 5 ONLINE] |
|      RANK #3 · 12.5 PTS                                        |
|                                                                |
| [icon LEADERBOARD]                                             |
| [icon HISTORY    ]            (nhân vật lớn)                   |
| [icon SETTINGS   ]            (bomber_full)                    |
| [icon HELP       ]                                             |
|                                                                |
|                         [map ONLINE ROOMS 3 OPEN >] [  PLAY  ] |
+----------------------------------------------------------------+
```

**Dữ liệu khi mở màn:** gửi `ONLINE_USERS_REQUEST`, `ROOM_LIST_REQUEST`, `RANKING_REQUEST`. Sau đó server tự đẩy cập nhật danh sách online và danh sách phòng.

**Hành vi**
- Badge (H-01, H-02): đầu nhân vật theo màu đại diện, username, dòng "RANK #n · x PTS" (hoặc "UNRANKED").
- Ô ⭐ (H-03): tổng điểm. Ô ● (H-04): số người online, bấm vào mở S3.
- Cột trái: LEADERBOARD → S7, HISTORY → S8, SETTINGS → popup Settings, HELP → popup Help.
- Thẻ ONLINE ROOMS (H-10): "n OPEN · m ONLINE", với n là số phòng đang `WAITING` và còn chỗ. Bấm vào mở S3.
- Nút PLAY (H-11) chạy **Quick Play**:
  1. Lọc các phòng có `status = WAITING` và `playerCount < maxPlayers`.
  2. Chọn phòng đông người nhất. Nếu bằng nhau thì lấy phòng đứng trước trong danh sách (server đã sắp theo tên).
  3. Có phòng thì gửi `JOIN_ROOM`. Nếu bị lỗi `ROOM_FULL`, `ROOM_NOT_WAITING` hoặc `ROOM_NOT_FOUND` thì thử lại **một lần** với phòng kế tiếp, rồi mới báo lỗi.
  4. Không có phòng nào thì gửi `CREATE_ROOM` với tên `"<username>'s Room"`, cắt còn tối đa 60 ký tự.
  5. Nút PLAY ở trạng thái chờ cho tới khi nhận `ROOM_STATE` hoặc lỗi.

**Nghiệm thu**
- [ ] Bố cục khớp mẫu 1, các phần tử bị ẩn không để lại khoảng trống lạ.
- [ ] Số online, số phòng, hạng tự cập nhật khi client khác vào/ra.
- [ ] Quick Play: không có phòng → tạo; có phòng → vào phòng đông nhất; bỏ qua phòng đầy/đang chơi.

### 3.3 S3 · Room Browser (không có mẫu)

```
+----------------------------------------------------------------+
| [< BACK]           [ ONLINE ROOMS ]                            |
|                                                                |
| +-----------------+ +-----------------+ +--------------------+ |
| | Bomber Party!   | | Pro Room        | | CREATE ROOM        | |
| | host: alex      | | host: momo      | | [________________] | |
| | (o)(o)( )( ) 2/4| | (o)(o)(o)(o) 4/4| | [     CREATE     ] | |
| | WAITING  [JOIN] | | PLAYING  [----] | |--------------------| |
| +-----------------+ +-----------------+ | ONLINE (5)         | |
| +-----------------+                     | o alex     IN LOBBY| |
| | Chill           |                     | o momo      IN ROOM| |
| | host: tako      |                     | o tako      PLAYING| |
| | (o)( )( )( ) 1/4|                     |                    | |
| | WAITING  [JOIN] |                     |                    | |
| +-----------------+                     +--------------------+ |
|                                                                |
+----------------------------------------------------------------+
```

**Dữ liệu khi mở màn:** gửi `ROOM_LIST_REQUEST`, `ONLINE_USERS_REQUEST`.

**Hành vi**
- Lưới thẻ phòng 2 cột, cuộn dọc khi nhiều phòng. Mỗi thẻ gồm:
  - Tên phòng (font Nunito vì có thể có dấu tiếng Việt).
  - Tên host: `RoomSummaryDto` chỉ có `hostUserId`, nên tra tên trong danh sách online. Không tìm thấy thì ẩn dòng này.
  - 4 đầu nhân vật nhỏ: `playerCount` đầu đầu tiên tô theo màu slot 1..n, số còn lại màu xám.
  - Badge trạng thái: WAITING (xanh lá), PLAYING (cam), FINISHED (xám).
  - Nút JOIN chỉ bật khi phòng `WAITING` và còn chỗ.
- Panel CREATE ROOM: ô nhập tên (giới hạn 60 ký tự, tự trim), nút CREATE khóa khi ô trống. Nhấn Enter để tạo.
- Panel ONLINE (n): mỗi người một dòng với chấm trạng thái: `FREE` → "IN LOBBY" (xanh lá), `IN_ROOM` → "IN ROOM" (vàng), `PLAYING` → "PLAYING" (đỏ). Dòng của mình có nhãn "(you)".
- Không có phòng nào: hiện minh họa + "No rooms yet. Create one!".
- BACK → S2.

**Nghiệm thu**
- [ ] Danh sách tự cập nhật khi client khác tạo phòng/vào phòng/bắt đầu chơi.
- [ ] Phòng đầy hoặc đang chơi thì nút JOIN bị khóa.
- [ ] Tạo phòng với tên rỗng hoặc dài hơn 60 ký tự bị chặn ở client.

### 3.4 S4 · Room Lobby (mẫu 4)

```
+----------------------------------------------------------------+
| [ROOM LOBBY]                                             [ X ] |
| [ROOM NAME: Bomber Party!]                                     |
|                                                                |
| +-1-- HOST --+ +-2----------+ +-MAP: CLASSIC---+ +-INFO-------+|
| | nhân vật   | | nhân vật   | | xem trước map  | | CLASSIC    ||
| | PLAYER     | | Alex       | | [1]      [2]   | | LAST ALIVE ||
| |[  READY   ]| |[NOT READY ]| | [3]      [4]   | | 2/4 PLAYERS||
| +------------+ +------------+ +----------------+ +------------+|
| +-3----------+ +-4----------+ +-ROOM STATUS-------------------+|
| | WAITING... | | WAITING... | | [v] Players 2/4 (min 2)       ||
| |            | |            | | [x] All players ready (1/2)   ||
| +------------+ +------------+ | ... Waiting for players       ||
| [  READY / UNREADY  ]         +-------------------------------+|
|                                [         START GAME          ] |
+----------------------------------------------------------------+
```

**Hành vi**
- **Slot:** slot thứ i ứng với người thứ i trong `RoomStateDto.players` (thứ tự vào phòng). Màu slot i là màu đội thứ i (đỏ, xanh dương, xanh lá, vàng). Mọi client đều thấy giống nhau vì cùng dựa trên thứ tự của server.
- Slot có người: số thứ tự, nhân vật (`bomber_full` cắt lấy nửa trên), tên, thanh READY (xanh lá, dấu ✔) hoặc NOT READY (xám, vòng tròn rỗng). Host có vương miện "HOST". Slot của mình có nhãn "YOU".
- Slot trống (R-08): viền nét đứt, bóng nhân vật mờ, "WAITING…".
- **MAP (R-10):** vẽ map mặc định bằng chính các tile trong trận. Map này cố định (công thức ở [06](06-gameplay-va-hieu-ung.md) mục 1). Đánh dấu điểm xuất phát bằng đầu nhân vật màu slot: slot 1 góc trên trái, 2 trên phải, 3 dưới trái, 4 dưới phải.
- **INFO:** MODE "CLASSIC", WIN "LAST ONE STANDING", PLAYERS "n/4".
- **ROOM STATUS (R-16):**
  - `[v]`/`[x]` Players n/4 (min 2): đạt khi n ≥ 2.
  - `[v]`/`[x]` All players ready (k/n): đạt khi mọi người đều READY, kể cả host.
  - Dòng cuối: đủ điều kiện thì host thấy "Ready to start!", người khác thấy "Waiting for host to start…". Chưa đủ thì "Waiting for players…".
- **READY/UNREADY (R-17):** gửi `READY` với giá trị ngược lại trạng thái hiện tại.
- **START GAME (R-18):** chỉ host thấy nút vàng, sáng khi n ≥ 2 và tất cả READY. Người khác thấy nút xám "WAITING FOR HOST".
- **X (R-19):** popup xác nhận "Leave this room?" rồi gửi `LEAVE_ROOM`.
- Host rời phòng: server chuyển host cho người vào sớm nhất còn lại. UI chỉ cần đọc lại `hostUserId`. Người đứng sau người vừa rời sẽ lùi slot và đổi màu theo; đây là hành vi chấp nhận được vì mọi client thấy giống nhau.

**Nghiệm thu**
- [ ] Hai client: tạo phòng, vào phòng, READY, host START → cả hai sang S5.
- [ ] START bị khóa khi có 1 người hoặc còn người chưa READY.
- [ ] Host rời phòng → người còn lại thành host, có vương miện.
- [ ] Điểm xuất phát trên map preview trùng với vị trí thật khi vào trận.

### 3.5 S5 · Game (mẫu 5)

```
+----------------------------------------------------------------+
| [P1 b1 f2] [P2 b1 f2]     [ 01:23 ]      [P3 b1 f2] [P4 b1 f2] |
|                                                                |
|  cây    +--------------------------------------------+   cây   |
|  hoa    |            sân 13x11 ô (Canvas)            |   đá    |
|  gốc    |  viền + cột = HARD_WALL, thùng, bom, lửa   |   bụi   |
|  bụi    +--------------------------------------------+   hoa   |
+----------------------------------------------------------------+
```
`P1 b1 f2` là thẻ người chơi: avatar, tên, nhãn P1, số bom còn đặt được (b), tầm nổ (f).

**Hành vi**
- **HUD:** P1, P2 bên trái, đồng hồ ở giữa, P3, P4 bên phải; slot không có người thì ẩn thẻ. Người bị loại: thẻ chuyển xám, avatar có đầu lâu, nhãn "OUT".
- **Đồng hồ:** thời gian đã chơi `mm:ss` tính từ `tick ÷ 20`.
- **Sân chơi, điều khiển, hiệu ứng:** theo [06](06-gameplay-va-hieu-ung.md).
- Đầu trận: toast gợi ý "WASD / Arrows: move · SPACE: bomb · ESC: menu" trong 3 giây.
- Mình bị loại: dải thông báo "YOU'RE OUT – watching the match" ở cạnh dưới. Vẫn xem tiếp được hoặc bấm ESC để rời.
- **ESC:** mở menu MATCH MENU gồm RESUME, HELP, LEAVE MATCH. Menu ghi rõ "The match keeps running", vì server không tạm dừng trận. Khi menu mở, input di chuyển bị chặn.
- **LEAVE MATCH:** xác nhận "Leave the match? You will be knocked out and lose." rồi gửi `LEAVE_ROOM` → S2.
- Nhận `GAME_OVER`: chờ 1.2 giây cho người chơi thấy vụ nổ cuối (người thắng nhảy mừng), sau đó chuyển S6.

**Nghiệm thu:** xem [06](06-gameplay-va-hieu-ung.md) và kịch bản T08–T10 trong [08](08-kiem-thu-va-rui-ro.md).

### 3.6 S6 · Result (mẫu 6)

**Biến thể** theo kết quả của mình (`GameOverPlayerDto.result`):

| Kết quả | Tiêu đề | Màu tiêu đề | Nhân vật | Hiệu ứng |
|---|---|---|---|---|
| WIN | VICTORY + vương miện + nguyệt quế | Vàng | `bomber_victory` (cầm cúp) | Confetti, tia sáng xoay |
| LOSS | DEFEAT | Xanh xám | `bomber_defeat` (buồn) | Không confetti |
| DRAW | DRAW + nguyệt quế | Bạc | `bomber_full` | Tia sáng nhẹ |

**Bảng xếp hạng trận:**
1. Người có `result = WIN` đứng hạng 1.
2. Những người còn lại xếp theo thời điểm bị loại, ai bị loại muộn hơn xếp trên. Người bị loại trong cùng một snapshot có cùng hạng.
3. Trận hòa: mọi người có `result = DRAW` cùng hạng 1.
4. Thiếu dữ liệu thứ tự bị loại (ví dụ client mất vài snapshot): xếp theo WIN → DRAW → LOSS, rồi theo số slot.

Mỗi dòng: ô số hạng (1 vàng, 2 bạc, 3 đồng, còn lại tím nhạt), avatar màu slot, username, ⭐ điểm nhận được (+1 / +0.5 / +0), badge WINNER hoặc DRAW. Dòng của mình có viền sáng và nhãn "YOU".

**Nút**
- PLAY AGAIN → gửi `PLAY_AGAIN`. Nhận `ROOM_STATE` (WAITING) → S4.
- HOME → gửi `LEAVE_ROOM` → S2.

**Tranh chấp Play Again.** Khi **một** người bấm PLAY AGAIN, server đưa cả phòng về `WAITING`, đặt lại READY và gửi `ROOM_STATE` cho **mọi** thành viên.
- Nhận `ROOM_STATE` (WAITING) khi đang ở S6 mà mình chưa bấm PLAY AGAIN: **ở lại** S6, toast "Room is ready for a rematch", nút PLAY AGAIN đổi thành **BACK TO ROOM**. Bấm nút này chỉ chuyển màn sang S4, không gửi gì.
- Bấm PLAY AGAIN mà nhận lỗi `ROOM_NOT_FINISHED` (người khác đã bấm trước): coi như thành công, chuyển S4.

**Nghiệm thu**
- [ ] Đúng biến thể WIN, LOSS, DRAW.
- [ ] Thứ tự hạng đúng với thứ tự bị loại thực tế.
- [ ] Điểm nhận được khớp với Leaderboard sau trận.
- [ ] Hai người cùng xem kết quả, một người bấm PLAY AGAIN: người kia không bị chuyển màn đột ngột.

### 3.7 S7 · Leaderboard (không có mẫu)

```
+----------------------------------------------------------------+
| [< BACK]            [ LEADERBOARD ]                            |
|                                                                |
|                          (nhân vật)                            |
|        (nhân vật)        +--------+        (nhân vật)          |
|        +--------+        |   1    |        +--------+          |
|        |   2    |        |  gold  |        |   3    |          |
|        | silver |        |        |        | bronze |          |
| -------+--------+--------+--------+--------+--------+--------  |
|  #   PLAYER                  SCORE    W    D    L              |
|  4   (o) alex                  7.5    7    1    3              |
|  5   (o) momo                  5.0    5    0    6              |
| >12  (o) you                   1.0    1    0    4   < YOU      |
+----------------------------------------------------------------+
```

**Dữ liệu khi mở màn:** gửi `RANKING_REQUEST`.

**Hành vi**
- Bục cho hạng 1–3: nhân vật `bomber_full` theo màu đại diện của người đó, tên, điểm. Hạng 1 ở giữa và cao nhất.
- Bảng từ hạng 4: số hạng, avatar + tên, SCORE (`totalScoreUnits ÷ 2`, một chữ số thập phân), W, D, L.
- Dòng của mình được tô sáng. Nếu nằm ngoài vùng đang thấy thì hiện thêm một dòng ghim ở cuối.
- Dùng đúng giá trị `rank` server trả về (đã xử lý đồng hạng).
- Chưa có ai: "No ranked players yet. Play a match!".

### 3.8 S8 · History (không có mẫu)

```
+----------------------------------------------------------------+
| [< BACK]           [ MATCH HISTORY ]                           |
|                                                                |
| +------------------------------------------------------------+ |
| |#| VICTORY  2026-09-26 20:15  03:42  (a)(b)(c)     +1    *  | |
| +------------------------------------------------------------+ |
| |#| DEFEAT   2026-09-26 20:05  01:58  (a)(b)        +0    *  | |
| +------------------------------------------------------------+ |
| |#| DRAW     2026-09-26 19:50  02:30  (a)(b)(c)(d)  +0.5  *  | |
| +------------------------------------------------------------+ |
+----------------------------------------------------------------+
```
`#` là dải màu bên trái theo kết quả: xanh lá (WIN), đỏ (LOSS), xám (DRAW).

**Dữ liệu khi mở màn:** gửi `HISTORY_REQUEST`.

**Hành vi**
- Mỗi trận một thẻ: kết quả của mình (`viewerResult`), ngày giờ kết thúc theo giờ máy (`yyyy-MM-dd HH:mm`), **thời lượng** `endedAt − startedAt` dạng `mm:ss`, avatar + tên người chơi (người thắng có vương miện nhỏ), điểm nhận được (`viewerScoreEarnedUnits ÷ 2`).
- Sắp xếp mới nhất lên trên (sắp ở client theo `endedAtEpochMillis`).
- Chưa có trận: "No matches yet. Go play!".

---

## 4. Popup và toast

Mọi popup dùng khuôn panel: tiêu đề tối, thân màu kem (lấy từ mẫu 3). Nền phía sau tối đi 60%. Nhấn ESC hoặc bấm ra ngoài để đóng, trừ popup CONNECTION LOST.

### 4.1 Settings (mở từ Home)
| Mục | Nội dung |
|---|---|
| SERVER | `host:port` chỉ đọc, kèm chấm trạng thái kết nối |
| DISPLAY | Công tắc Fullscreen (tương đương F11) |
| ACCOUNT | Nút LOG OUT (đỏ) → xác nhận → gửi `LOGOUT` → S1 |

Không có mục âm thanh vì app chưa có âm thanh.

### 4.2 Server Address (mở từ nút bánh răng ở Login)
- Ô HOST (không rỗng), ô PORT (1–65535). Nút RESET (về giá trị mặc định), CANCEL, SAVE.
- SAVE thì lưu vào tùy chọn của máy. Nếu đang có kết nối tới địa chỉ cũ thì đóng kết nối; lần gửi tiếp theo sẽ kết nối tới địa chỉ mới.

### 4.3 Help (nội dung hiển thị)
```
CONTROLS
  W A S D / Arrow keys   Move (hold to keep moving)
  SPACE                  Place a bomb
  ESC                    Match menu
  F11                    Fullscreen

RULES
  • 2–4 players per room. Everyone starts with 1 bomb and a blast range of 2 tiles.
  • Bombs explode after 3 seconds.
  • A blast stops at stone blocks and breaks the first crate it reaches.
  • A blast sets off any other bomb it reaches.
  • Players and bombs block the way. You can step off a bomb you just placed.
  • Anyone standing in a blast when it goes off is knocked out.
  • Last player standing wins. If the last players go down together, it's a draw.

SCORING
  Win +1 · Draw +0.5 · Loss 0
```
Các con số lấy từ luật server, xem [06](06-gameplay-va-hieu-ung.md) mục 1.

### 4.4 Confirm
Tiêu đề, câu hỏi, nút CANCEL (xanh dương) và nút xác nhận (đỏ nếu là hành động rời/thoát). Dùng cho: rời phòng, rời trận, đăng xuất.

### 4.5 Connection Lost
Tiêu đề "CONNECTION LOST", nội dung "Lost connection to <host>:<port>.", nút OK → S1. Không đóng được bằng ESC.

### 4.6 Toast
- Hiện ở giữa phía trên, tối đa 2 toast cùng lúc, cái mới đẩy cái cũ xuống.
- Loại: info (tím), success (xanh lá), error (đỏ).
- Tự ẩn sau 3 giây, riêng error sau 4 giây.
- Thay cho dòng `feedback` của client cũ.

---

## 5. Xử lý thông điệp từ server

### 5.1 Bảng xử lý

| Thông điệp | Cập nhật state | Điều hướng / hiển thị |
|---|---|---|
| `LOGIN_RESPONSE`, thành công | Lưu `userId`, `username`; nhớ username | → S2 |
| `LOGIN_RESPONSE`, thất bại | | Toast lỗi theo mục 6.2 |
| `REGISTER_RESPONSE`, thành công | | Toast success, chuyển tab LOGIN |
| `REGISTER_RESPONSE`, thất bại | | Toast lỗi theo mục 6.2 |
| `ONLINE_USERS_UPDATE` | `onlineUsers` | Cập nhật S2, S3 |
| `ROOM_LIST_UPDATE` | `rooms` | Cập nhật S2, S3 |
| `ROOM_STATE`, `member = false` | `room = null` | → S2 (nếu đang ở S4/S5/S6) |
| `ROOM_STATE`, `member = true`, `WAITING` | `room` | → S4, **trừ khi** đang ở S6: ở lại và bật cờ rematch (mục 3.6) |
| `ROOM_STATE`, `member = true`, `PLAYING` | `room`; bắt đầu theo dõi trận mới (06 mục 8) | → S5 |
| `ROOM_STATE`, `member = true`, `FINISHED` | `room` | Không chuyển màn |
| `GAME_STATE` | Snapshot mới nhất (ghi ngay trên luồng mạng); cập nhật bộ theo dõi trận | Nếu chưa ở S5/S6 → S5 |
| `GAME_OVER` | `gameOver` | Sau 1.2 giây → S6 |
| `RANKING_RESPONSE` | `rankingEntries` | Cập nhật S2, S7 |
| `HISTORY_RESPONSE` | `matchHistory` | Cập nhật S8 |
| `ERROR` | | Toast error với `message` của server. Ở S5, bỏ qua lỗi lệnh trong trận (`GameCommandResultCode`) để không bị toast liên tục |
| Mất kết nối | Xóa state đăng nhập | → S1 + popup CONNECTION LOST |

Ghi chú về server:
- Khi trận kết thúc, server chỉ gửi `GAME_OVER` rồi đưa phòng về `FINISHED`, **không** gửi `ROOM_STATE`. Vì vậy client đứng ở S5/S6 cho tới khi người chơi bấm nút.
- Khi rời phòng, server gửi riêng cho người rời một `ROOM_STATE` với `member = false`.

### 5.2 Theo dõi yêu cầu đang chờ
- Mỗi yêu cầu gửi đi có `requestId`. Phản hồi (`LOGIN_RESPONSE`, `ROOM_STATE`, `ERROR`…) trả lại đúng `requestId` đó cho người gửi.
- Nút đã gửi yêu cầu ở trạng thái chờ cho tới khi nhận phản hồi cùng `requestId`, hoặc hết **5 giây**. Hết giờ thì mở khóa nút và toast "Server is not responding.".

---

## 6. Kiểm tra dữ liệu nhập và thông báo lỗi

### 6.1 Kiểm tra ở client (khớp quy tắc server)

| Trường | Quy tắc | Thông báo |
|---|---|---|
| Username | Không rỗng hoặc toàn khoảng trắng, tối đa 50 ký tự | "Username must be 1–50 characters." |
| Password | Không rỗng hoặc toàn khoảng trắng | "Password is required." |
| Confirm password (REGISTER) | Trùng với password | "Passwords do not match." |
| Tên phòng | Trim, 1–60 ký tự | "Room name must be 1–60 characters." |
| Host | Không rỗng | "Host is required." |
| Port | Số nguyên 1–65535 | "Port must be between 1 and 65535." |

### 6.2 Mã lỗi đăng nhập/đăng ký → thông báo

| `AuthResultCode` | Thông báo |
|---|---|
| `INVALID_REQUEST` | "Invalid request. Please try again." |
| `INVALID_USERNAME` | "Username must be 1–50 characters." |
| `INVALID_PASSWORD` | "Password is required." |
| `USERNAME_ALREADY_EXISTS` | "That username is already taken." |
| `INVALID_CREDENTIALS` | "Wrong username or password." |
| `ACCOUNT_ALREADY_ONLINE` | "This account is already online on another device." |
| `SESSION_ALREADY_AUTHENTICATED` | "You are already logged in." |

### 6.3 Lỗi phòng và kết nối
- Lỗi phòng đến qua `ERROR`, server đã kèm câu tiếng Anh dễ đọc (ví dụ "At least two players are required", "All players must be ready"). Hiện nguyên văn.
- Không kết nối được: "Cannot connect to <host>:<port>.".
- Mất kết nối: popup ở mục 4.5.
