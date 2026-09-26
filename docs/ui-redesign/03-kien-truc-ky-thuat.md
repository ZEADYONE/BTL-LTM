# 03 · Kiến trúc kỹ thuật

## 1. Tổng quan

- Tạo module mới **`client-fx`**, phụ thuộc `:common`. Sửa `settings.gradle` thành `include 'common', 'server', 'client', 'client-fx'`.
- **Không sửa** `server` và `common`. **Không sửa** `client` cũ; giữ lại tới khi client mới xong (quyết định D9).
- Giao thức mạng giữ nguyên, nên client cũ và client mới chơi chung được trên cùng một server.

## 2. Thư viện

| Thư viện | Tọa độ Maven | Phiên bản | Dùng cho | Ghi chú |
|---|---|---|---|---|
| JavaFX | `org.openjfx:javafx-base`, `javafx-graphics`, `javafx-controls` | 21.0.x (LTS) | UI, Canvas | Khai báo **cả 3** artifact kèm classifier theo hệ điều hành (`win`, `linux`, `mac`). Gradle không tự suy ra classifier cho các phụ thuộc bắc cầu của JavaFX |
| JSVG | `com.github.weisj:jsvg` | 2.2.0 | Vẽ file SVG thành ảnh | Không kéo theo thư viện nào khác. JavaFX chỉ có `SVGPath` (một đường), không đọc được file SVG nhiều lớp |
| Ikonli | `org.kordamp.ikonli:ikonli-javafx` + `ikonli-materialdesign2-pack` | 12.4.0 | Icon đơn sắc: mũi tên, ✔, ✖, home, replay, logout, chevron | Icon là font nên không cần file ảnh |
| JUnit 5 | `org.junit:junit-bom` | 5.12.2 (giống các module khác) | Unit test | |
| jpackage | Có sẵn trong JDK 21 | | Đóng gói exe | |

Không dùng theme có sẵn như AtlantaFX: đó là phong cách ứng dụng văn phòng, khác xa phong cách game trong mẫu. CSS được viết riêng (xem [04](04-design-system.md)).

## 3. Lệnh Gradle

| Lệnh | Tác dụng |
|---|---|
| `.\gradlew :client-fx:run` | Chạy client |
| `.\gradlew :client-fx:run --args="--gallery"` | Mở màn Gallery để xem mọi component và asset. Chỉ dùng khi phát triển |
| `.\gradlew :client-fx:test` | Chạy unit test |
| `.\gradlew :client-fx:packageApp` | Tạo `client-fx/build/dist/BombermanOnline/BombermanOnline.exe` kèm JRE |
| `.\gradlew :client-fx:packageInstaller` | *(Tùy chọn)* Tạo bộ cài `.exe`. Cần cài WiX Toolset 3 |

Vẫn ghi đè được địa chỉ server bằng `-Dbomberman.server.host`, `-Dbomberman.tcp.port` hoặc biến môi trường như client cũ.

## 4. Cấu trúc package

Tên lớp dưới đây là đề xuất, có thể đổi khi làm.

```
client-fx/
├── build.gradle
├── packaging/
│   └── app_icon.ico
└── src/
    ├── main/java/com/bomberman/clientfx/
    │   ├── Launcher.java                 main(): gọi Application.launch (bắt buộc khi JavaFX nằm trên classpath)
    │   ├── BombermanApp.java             khởi tạo state, network, font, asset, cửa sổ
    │   ├── ServerRules.java              hằng số sao chép từ server (mục 9)
    │   ├── network/
    │   │   ├── GameNetworkClient.java        [port] socket TCP + luồng đọc
    │   │   ├── ServerListener.java           [port]
    │   │   ├── ClientNetworkConfig.java      [port] + đọc địa chỉ đã lưu
    │   │   ├── ClientMessageDispatcher.java  [port] Platform.runLater + quy tắc điều hướng mới
    │   │   ├── GameClientController.java     [port] + quickPlay, đổi server, theo dõi requestId
    │   │   └── PendingRequests.java          yêu cầu đang chờ + hết giờ 5 giây
    │   ├── state/
    │   │   ├── ClientState.java              [port] + các trường mới cho UI
    │   │   ├── ClientStateListener.java      [port]
    │   │   └── UserPreferences.java          host, port, username gần nhất, fullscreen
    │   ├── asset/
    │   │   ├── SvgAssets.java                SVG → Image, cache, dự phòng PNG, ảnh thay thế
    │   │   ├── SvgRecolor.java               thay màu khóa theo màu đội
    │   │   └── AssetIds.java                 tên file, khớp bảng trong 05
    │   ├── ui/
    │   │   ├── AppShell.java                 khung 1280×720, scale, lớp toast/popup, chuyển màn
    │   │   ├── Navigator.java                S1…S8
    │   │   ├── theme/      Palette, Fonts
    │   │   ├── component/  GameButton, MenuTileButton, OutlinedText, TitleBanner, Panel, Pill,
    │   │   │               PlayerBadge, PlayerHead, SlotCard, RoomCard, RankRow, HudPlayerCard,
    │   │   │               Checklist, Toast, Modal, Confetti, SunRays
    │   │   ├── screen/     LoginScreen, HomeScreen, RoomBrowserScreen, RoomLobbyScreen,
    │   │   │               GameScreen, ResultScreen, LeaderboardScreen, HistoryScreen, GalleryScreen
    │   │   └── popup/      SettingsPopup, HelpPopup, ConfirmPopup, ServerAddressPopup,
    │   │                   ConnectionLostPopup, MatchMenuPopup
    │   └── game/
    │       ├── GameRenderer.java             vẽ sân lên Canvas theo thứ tự ở 06
    │       ├── BoardLayout.java              tính cỡ ô, vị trí sân, trang trí hai bên
    │       ├── InputController.java          giữ phím, lặp MOVE
    │       ├── PlayerVisual.java             nội suy vị trí, hướng nhìn
    │       ├── MatchTracker.java             màu slot, thứ tự bị loại, phát hiện sự kiện
    │       ├── QuickPlay.java                chọn phòng cho nút PLAY
    │       ├── MapPreview.java               dựng map mặc định cho Room Lobby
    │       └── fx/         BombFx, ExplosionFx, DebrisFx, DustFx, DeathFx, ScreenShake, ParticlePool
    ├── main/resources/
    │   ├── client.properties                 địa chỉ server mặc định (giống client cũ)
    │   ├── css/game-theme.css
    │   ├── fonts/                            xem 05 mục 7
    │   └── assets/svg/{characters,tiles,fx,icons,deco,bg}/
    └── test/java/com/bomberman/clientfx/     xem 08 mục 1
```

## 5. Phần lấy lại từ client cũ

| File cũ trong `client/…/client/` | Cách làm |
|---|---|
| `network/GameNetworkClient.java` | Copy nguyên |
| `network/ServerListener.java` | Copy nguyên |
| `network/ClientNetworkConfig.java` | Copy. Thứ tự ưu tiên: system property → biến môi trường → địa chỉ đã lưu (popup Server Address) → `client.properties` |
| `network/ClientMessageDispatcher.java` | Copy. Đổi `Gdx.app.postRunnable` thành `Platform.runLater` ([dòng 134](../../client/src/main/java/com/bomberman/client/network/ClientMessageDispatcher.java#L134)). Thay logic chuyển màn theo [02](02-man-hinh-va-dieu-huong.md) mục 5 |
| `network/GameClientController.java` | Copy. Đổi `Gdx.app.postRunnable` ([dòng 125](../../client/src/main/java/com/bomberman/client/network/GameClientController.java#L125)). Thêm quick play, đổi server, theo dõi `requestId` |
| `state/ClientState.java`, `state/ClientStateListener.java` | Copy, thêm trường mới: địa chỉ server, trạng thái kết nối, cờ rematch… |
| Test `ClientNetworkConfigTest` | Copy, sửa package |
| `asset/`, `screen/`, `renderer/`, `BombermanClient`, `DesktopLauncher` | Viết mới |

Tầng network của client cũ chỉ phụ thuộc libGDX ở 2 chỗ `postRunnable` nói trên. Phần còn lại là Java thuần.

## 6. Khung hình, scale và độ nét

- **Các màn menu** (S1–S4, S6–S8): dựng trên một pane cố định 1280×720, áp `Scale = min(W/1280, H/720)` và căn giữa. Phần thừa hai bên tô nền tối có họa tiết. Nhờ vậy bố cục luôn giống mẫu ở mọi kích thước cửa sổ.
- **Màn chơi** (S5): HUD vẫn nằm trong khung đã scale, nhưng Canvas vẽ sân **không** nằm dưới phép scale. Canvas lấy kích thước pixel thật, `BoardLayout` tự tính cỡ ô. Lý do: scale một Canvas bằng transform là phóng to bitmap, hình sẽ bị mờ.
- **Màn hình DPI cao:** ảnh được vẽ ở kích thước *logic × hệ số scale UI × outputScale của màn hình*. Khi đổi kích thước cửa sổ hoặc chuyển màn hình, vẽ lại sau 150 ms (debounce) rồi mới thay ảnh.
- **Cửa sổ:** mặc định 1280×720 (thu về 90% màn hình nếu màn hình nhỏ hơn), nhỏ nhất 960×540. F11 bật/tắt toàn màn hình, trạng thái này được lưu lại.

## 7. Nạp hình ảnh (`SvgAssets`)

1. Đọc `/assets/svg/<nhóm>/<tên>.svg` từ classpath.
2. Riêng nhân vật: `SvgRecolor` thay 3 màu khóa bằng màu của đội (thay chuỗi, không phân biệt hoa thường). Màu khóa ở [05](05-tai-nguyen-svg-va-font.md) mục 3.
3. JSVG đọc SVG, vẽ vào `BufferedImage` (ARGB, bật khử răng cưa) đúng kích thước pixel cần dùng.
4. Chuyển `BufferedImage` sang `WritableImage` của JavaFX bằng `PixelWriter`, không cần module `javafx-swing`.
5. Cache theo khóa *(file, màu đội, rộng px, cao px)*, giới hạn khoảng 64 MB, bỏ ảnh ít dùng nhất khi đầy.
6. **Dự phòng** theo thứ tự:
   - Không có SVG → dùng PNG cùng tên (`<tên>.png`; nhân vật dùng `<tên>_<màu>.png`, ví dụ `bomber_full_red.png`).
   - Không có cả hai, hoặc file lỗi → ảnh thay thế (ô caro kèm tên file), ghi log WARN một lần cho mỗi file.
7. **Nạp trước:** khi vào Room Lobby, vẽ sẵn mọi sprite trong trận theo cỡ ô hiện tại ở luồng nền, để lúc bắt đầu trận không bị khựng.
8. Việc parse và vẽ SVG chạy ở luồng nền. Luồng giao diện chỉ gắn ảnh đã vẽ xong.

## 8. Màu người chơi

| Vị trí | Màu đội | Điểm xuất phát (cột, hàng) | Góc sân |
|---|---|---|---|
| Slot 1 | Đỏ | (1, 1) | Trên trái |
| Slot 2 | Xanh dương | (11, 1) | Trên phải |
| Slot 3 | Xanh lá | (1, 9) | Dưới trái |
| Slot 4 | Vàng | (11, 9) | Dưới phải |

- Slot i = người thứ i trong `RoomStateDto.players` (thứ tự vào phòng) = người thứ i trong `GameStateDto.players` = điểm xuất phát thứ i. Đã kiểm tra trên server:
  - Danh sách người chơi của phòng giữ thứ tự vào phòng ([GameRoom.java:17](../../server/src/main/java/com/bomberman/server/room/GameRoom.java#L17)).
  - Người tham gia trận lấy theo đúng thứ tự đó ([GameSessionManager.java:71](../../server/src/main/java/com/bomberman/server/game/GameSessionManager.java#L71)).
  - Người thứ i xuất phát ở điểm thứ i ([BombermanGame.java:79](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L79)), và thứ tự được giữ trong snapshot ([BombermanGame.java:30](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L30)).
- Trong trận, `MatchTracker` chốt ánh xạ `userId → slot` từ snapshot đầu tiên, nên màu không đổi giữa trận.
- Ngoài trận (Home, Leaderboard, History, danh sách online): màu đại diện là `palette[userId % 4]` (quyết định D7).

## 9. `ServerRules` – hằng số sao chép từ server

Client cần một số luật của server để vẽ và hiển thị. Tất cả gom vào lớp `ServerRules`. **Server đổi luật thì phải cập nhật lớp này và bảng dưới.**

| Hằng số | Giá trị | Nguồn trong server |
|---|---|---|
| Tick mỗi giây | 20 | [RoomGameLoop.java:19](../../server/src/main/java/com/bomberman/server/game/RoomGameLoop.java#L19) |
| Tần suất gửi snapshot | Mỗi 2 tick, tức 10 lần/giây | [RoomGameLoop.java:90](../../server/src/main/java/com/bomberman/server/game/RoomGameLoop.java#L90) |
| Kích thước map | 13 cột × 11 hàng | [GameMap.java:9](../../server/src/main/java/com/bomberman/server/game/GameMap.java#L9) |
| Điểm xuất phát | (1,1), (11,1), (1,9), (11,9) | [GameMap.java:12](../../server/src/main/java/com/bomberman/server/game/GameMap.java#L12) |
| Ô đá (`HARD_WALL`) | Viền ngoài, và ô có cột chẵn và hàng chẵn | [GameMap.java:91](../../server/src/main/java/com/bomberman/server/game/GameMap.java#L91) |
| Ô thùng (`BREAKABLE_WALL`) | `(cột × 31 + hàng × 17) % 4 == 0`, trừ ô xuất phát và 4 ô kề | [GameMap.java:31](../../server/src/main/java/com/bomberman/server/game/GameMap.java#L31), [GameMap.java:99](../../server/src/main/java/com/bomberman/server/game/GameMap.java#L99) |
| Thời gian ngòi bom | 3000 ms | [BombermanGame.java:26](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L26) |
| Thời gian lửa tồn tại | 500 ms | [BombermanGame.java:27](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L27) |
| Số bom / tầm nổ ban đầu | 1 / 2 ô | [BomberPlayer.java:8](../../server/src/main/java/com/bomberman/server/game/BomberPlayer.java#L8) |
| Số người mỗi trận | 2–4 | [BombermanGame.java:24](../../server/src/main/java/com/bomberman/server/game/BombermanGame.java#L24) |
| Điểm (đơn vị ½) | WIN 2, DRAW 1, LOSS 0 | [MatchScoring.java:8](../../server/src/main/java/com/bomberman/server/match/MatchScoring.java#L8) |
| Username | 1–50 ký tự | [AuthenticationService.java:88](../../server/src/main/java/com/bomberman/server/auth/AuthenticationService.java#L88) |
| Tên phòng | Trim, 1–60 ký tự | [RoomManager.java:36](../../server/src/main/java/com/bomberman/server/room/RoomManager.java#L36) |

## 10. Các luồng xử lý

| Luồng | Việc |
|---|---|
| Virtual thread `server-listener` | Đọc socket và giải mã. `GAME_STATE` được chuyển thành DTO và ghi ngay vào `AtomicReference`. Các thông điệp khác đẩy sang luồng giao diện bằng `Platform.runLater` |
| Executor ghi (1 luồng) | Gửi thông điệp, giữ nguyên như client cũ |
| JavaFX Application Thread | Giao diện, `AnimationTimer` vẽ trận (đọc snapshot từ `AtomicReference`), xử lý phím |
| Executor nền cho asset | Parse và vẽ SVG |

Quy tắc: state của giao diện chỉ được sửa trên luồng JavaFX. Snapshot trận là DTO bất biến, chia sẻ qua `AtomicReference`.

## 11. Lưu tùy chọn

Dùng `java.util.prefs.Preferences`, nhánh `com/bomberman/clientfx`: `serverHost`, `serverPort`, `lastUsername`, `fullscreen`. **Không bao giờ lưu mật khẩu.**

## 12. Đóng gói exe

- Lớp chạy chính là `Launcher` (không kế thừa `Application`). Đây là cách chạy JavaFX từ classpath. Khi chạy, JavaFX sẽ in cảnh báo "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'"; cảnh báo này vô hại.
- Task `packageApp`:
  1. Build jar của `client-fx`.
  2. Copy jar và toàn bộ `runtimeClasspath` (JavaFX bản `win`, JSVG, Ikonli, `common`, Jackson) vào `build/jpackage/input`.
  3. Gọi `jpackage --type app-image --name BombermanOnline --input build/jpackage/input --main-jar <jar> --main-class com.bomberman.clientfx.Launcher --icon packaging/app_icon.ico --dest build/dist --java-options -Dfile.encoding=UTF-8`.
- Kết quả là thư mục `client-fx/build/dist/BombermanOnline/` gồm `BombermanOnline.exe`, `app/`, `runtime/`. Nén **cả thư mục** để gửi cho người khác. Dung lượng khoảng 60–90 MB.
- Phải build trên Windows mới ra exe Windows, vì JavaFX có thư viện native riêng cho từng hệ điều hành.
- `packageInstaller` (tùy chọn) dùng `--type exe`, cần WiX Toolset 3 trong PATH. Máy hiện tại chưa cài.
- Địa chỉ server mặc định lấy từ `client.properties` trong jar. Người chơi đổi ở màn Login, giá trị được lưu lại cho lần sau.
- Tùy chọn giảm dung lượng: giới hạn module JDK bằng `--add-modules`. Chỉ làm sau khi bản đầy đủ đã chạy ổn.
