# Kế hoạch bổ sung âm thanh cho Bomberman Online Mini

> **Trạng thái:** Phase 0 đã tạo asset và hoàn tất kiểm tra kỹ thuật ngày 2026-10-12; chờ nghe thử/chốt phong cách trước Phase 1  
> **Phạm vi chính:** JavaFX client (`client-fx`)  
> **Mục tiêu:** Bổ sung nhạc nền, hiệu ứng gameplay, phản hồi UI và cài đặt âm lượng mà không thay đổi server hoặc giao thức mạng.

## 1. Hiện trạng và cơ sở thiết kế

Client JavaFX hiện chưa có âm thanh. Các điểm móc cần thiết đã tồn tại:

- `MatchTracker.FrameEvents` đã phát hiện đúng một lần cho bom mới, vụ nổ mới, thùng bị phá và người chơi bị loại.
- `GameScreen` xử lý các frame event trên JavaFX Application Thread, phù hợp để kích hoạt hiệu ứng âm thanh.
- `Screen.onShow()` / `onHide()` và `ScreenNavigator` cung cấp vòng đời màn hình để chuyển nhạc nền.
- `Feedback.Kind` phân biệt thông báo info/success/error, có thể tạo âm thanh phản hồi.
- `UserPreferences` đã dùng `java.util.prefs.Preferences`, phù hợp để lưu volume/mute.
- `SettingsPopup` đã có nơi đặt các điều khiển âm thanh.

Quyết định quan trọng:

1. Âm thanh chỉ là presentation concern của client; không thêm message hay trạng thái audio vào `common`/`server`.
2. Dùng `AudioClip` cho SFX ngắn và `MediaPlayer` cho nhạc nền dài.
3. Mọi quyết định “phát âm nào” đi qua một lớp điều phối trung tâm, không để từng màn hình tự tải file.
4. Gameplay sound lấy từ snapshot authoritative và `FrameEvents`, không phát trực tiếp khi nhấn phím. Nhờ vậy lệnh bị server từ chối sẽ không tạo âm thanh sai.
5. Thiếu hoặc lỗi một asset âm thanh không được làm game crash; client ghi warning và tiếp tục ở chế độ im lặng cho cue đó.
6. MVP không phát tiếng bước chân theo từng snapshot và không phát tiếng tích tắc cho từng quả bom, nhằm tránh spam âm thanh trong trận 4 người.

## 2. Phạm vi tính năng

### 2.1 Bắt buộc cho bản đầu tiên

- Nhạc nền menu/lobby.
- Nhạc nền trong trận đấu.
- SFX nhấn nút.
- SFX bắt đầu trận.
- SFX đặt bom đã được server xác nhận.
- SFX bom nổ.
- SFX người chơi hiện tại bị loại.
- SFX chiến thắng, thất bại và hòa.
- SFX thông báo thành công/lỗi.
- Master volume, Music volume, SFX volume và Mute.
- Lưu cài đặt giữa các lần mở ứng dụng.
- Dừng/giải phóng media player đúng lúc ứng dụng thoát.
- Âm thanh được đóng gói trong bản `jpackage` và `packageZip`.

### 2.2 Nâng cấp sau MVP

- Âm thanh riêng khi người chơi khác bị loại.
- SFX thùng gỗ vỡ, vào phòng, rời phòng, ready/unready và đếm ngược.
- Nhiều biến thể ngẫu nhiên cho explosion/click để giảm lặp.
- Ducking: tự hạ nhạc nền ngắn hạn khi explosion hoặc result stinger phát.
- Âm thanh theo khoảng cách/pan stereo nếu sau này camera không còn hiển thị toàn bản đồ.
- Tạm dừng nhạc khi ứng dụng mất focus (tùy chọn, mặc định không bật vì trận online vẫn chạy).

## 3. Danh mục âm thanh

| ID | File đề xuất | Loại | Điểm kích hoạt | Quy tắc chống lặp |
|---|---|---|---|---|
| `UI_CLICK` | `sfx/ui-click.wav` | SFX | JavaFX `ActionEvent` từ button | Cooldown 30 ms |
| `UI_SUCCESS` | `sfx/ui-success.wav` | SFX | `Feedback.Kind.SUCCESS` | Cooldown 150 ms |
| `UI_ERROR` | `sfx/ui-error.wav` | SFX | `Feedback.Kind.ERROR` | Cooldown 250 ms |
| `MATCH_START` | `sfx/match-start.wav` | SFX | `FrameEvents.newMatch()` | Một lần/match |
| `BOMB_PLACE_SELF` | `sfx/bomb-place.wav` | SFX | `newBombs` có `ownerUserId == currentUserId` | Một lần/batch snapshot |
| `BOMB_PLACE_OTHER` | `sfx/bomb-place-soft.wav` | SFX | Chỉ có bom của người khác trong batch | Một lần/batch, volume thấp hơn |
| `EXPLOSION` | `sfx/explosion.wav` | SFX | `newExplosions` không rỗng | Một lần/batch snapshot |
| `PLAYER_DEFEATED` | `sfx/player-defeated.wav` | SFX | `eliminatedPlayers` chứa người hiện tại | Một lần/match |
| `VICTORY` | `sfx/victory.wav` | Stinger | `ResultScreen.onShow`, kết quả WIN | Một lần/lần hiển thị kết quả mới |
| `DEFEAT` | `sfx/defeat.wav` | Stinger | Kết quả LOSS | Một lần/lần hiển thị kết quả mới |
| `DRAW` | `sfx/draw.wav` | Stinger | Kết quả DRAW | Một lần/lần hiển thị kết quả mới |
| `MENU_LOOP` | `music/menu-loop.mp3` | Music | Login, Home, browser, lobby, leaderboard, history | Loop, không restart nếu đang phát |
| `BATTLE_LOOP` | `music/battle-loop.mp3` | Music | Game | Loop, fade khi đổi track |

Không phát riêng `crate-break` trong MVP vì nó thường xảy ra cùng explosion; xếp chồng cả hai dễ clipping và làm âm thanh đục.

## 4. Yêu cầu asset

Thư mục dự kiến:

```text
client-fx/src/main/resources/assets/audio/
├── README.md
├── LICENSES.md
├── music/
│   ├── menu-loop.mp3
│   └── battle-loop.mp3
└── sfx/
    ├── ui-click.wav
    ├── ui-success.wav
    ├── ui-error.wav
    ├── match-start.wav
    ├── bomb-place.wav
    ├── bomb-place-soft.wav
    ├── explosion.wav
    ├── player-defeated.wav
    ├── victory.wav
    ├── defeat.wav
    └── draw.wav
```

Chuẩn kỹ thuật:

- SFX: WAV PCM, 44.1 kHz, 16-bit, mono hoặc stereo, ưu tiên dưới 300 KB/file.
- Music: MP3, 44.1 kHz, 128–160 kbps, loop liền mạch, ưu tiên dưới 4 MB/track.
- Peak sau normalize không vượt `-1 dBFS`; loudness giữa các SFX phải được cân bằng trước khi đưa vào repo.
- Nhạc không lấn hiệu ứng gameplay; master gợi ý `80%`, music `45%`, SFX `80%`.
- `LICENSES.md` ghi tên asset, tác giả/nguồn, URL, license và thay đổi đã thực hiện. Chỉ dùng asset tự tạo, CC0 hoặc license cho phép phân phối trong game.
- Kiểm tra đầu/cuối loop không có khoảng lặng hoặc tiếng click.

## 5. Kiến trúc đề xuất

### 5.1 Các lớp mới

```text
com.bomberman.clientfx.audio
├── AudioCue.java
├── MusicTrack.java
├── AudioOutput.java
├── JavaFxAudioOutput.java
├── AudioManager.java
├── GameplayAudio.java
└── package-info.java
```

Vai trò:

- `AudioCue`: enum ánh xạ cue tới resource, gain mặc định và cooldown.
- `MusicTrack`: enum ánh xạ track tới resource và thông tin loop.
- `AudioOutput`: abstraction nhỏ (`play`, `playMusic`, `stopMusic`, `close`) để test không cần JavaFX media runtime.
- `JavaFxAudioOutput`: tải `AudioClip`, quản lý một `MediaPlayer` đang hoạt động và fade giữa hai track.
- `AudioManager`: giữ master/music/SFX/mute, tính effective volume, chống spam, điều phối screen music và cung cấp API cho UI.
- `GameplayAudio`: chuyển `FrameEvents + currentUserId` thành các cue. Lớp này thuần Java và unit-test được.

API dự kiến:

```java
public interface AudioOutput extends AutoCloseable {
    void play(AudioCue cue, double volume);
    void playMusic(MusicTrack track, double volume, boolean loop);
    void setMusicVolume(double volume);
    void stopMusic(Duration fadeOut);
    void close();
}

public final class AudioManager implements AutoCloseable {
    void play(AudioCue cue);
    void playFeedback(Feedback feedback);
    void onScreenChanged(ScreenId screen);
    void setMasterVolume(double value);
    void setMusicVolume(double value);
    void setSfxVolume(double value);
    void setMuted(boolean muted);
}
```

Giá trị volume luôn clamp trong `[0.0, 1.0]`; effective music = `mute ? 0 : master * music`, effective SFX = `mute ? 0 : master * sfx * cueGain`.

### 5.2 Luồng dữ liệu

```text
Server GAME_STATE
  -> ClientState.latestGameState
  -> GameScreen.frame()
  -> MatchTracker.accept()
  -> FrameEvents
  -> GameplayAudio.accept()
  -> AudioManager.play(cue)
  -> JavaFxAudioOutput

ScreenNavigator.show(ScreenId)
  -> AudioManager.onScreenChanged(ScreenId)
  -> MENU_LOOP / BATTLE_LOOP / giữ nguyên track

SettingsPopup sliders
  -> AudioManager setters
  -> cập nhật volume tức thời
  -> UserPreferences lưu giá trị
```

### 5.3 Chính sách nhạc theo màn hình

| Màn hình | Nhạc |
|---|---|
| LOGIN, HOME, ROOM_BROWSER, ROOM_LOBBY, LEADERBOARD, HISTORY | `MENU_LOOP` |
| GAME | `BATTLE_LOOP` |
| RESULT | Dừng/fade `BATTLE_LOOP`, phát result stinger; sau stinger có thể trở lại `MENU_LOOP` ở volume thấp |

Chuyển track fade-out/fade-in khoảng 250–400 ms. Điều hướng giữa các màn hình dùng cùng `MENU_LOOP` không được khởi động lại bài.

### 5.4 Đồng bộ và thread

- Chỉ gọi JavaFX media API trên JavaFX Application Thread.
- `GameScreen.frame`, `ScreenNavigator.show`, `SettingsPopup` và feedback hiện đều đi qua UI thread; không gọi audio trực tiếp trong network thread.
- Nếu về sau có nguồn gọi từ background thread, `AudioManager` phải marshal qua `Platform.runLater` hoặc fail-fast trong development.
- Audio không tham gia `ClientState` vì không phải dữ liệu domain/network.

## 6. Thay đổi theo file

### `client-fx/build.gradle`

- Thêm `media` vào danh sách module JavaFX: `['base', 'graphics', 'controls', 'media']`.
- Không cần thay task `packageApp`: runtime dependency mới sẽ tự được gom bởi `prepareJpackageInput`.
- Sau triển khai phải kiểm tra app image thực sự chứa native JavaFX media libraries.

### `UserPreferences.java`

Thêm key và getter/setter:

- `audioMasterVolume`, mặc định `0.80`.
- `audioMusicVolume`, mặc định `0.45`.
- `audioSfxVolume`, mặc định `0.80`.
- `audioMuted`, mặc định `false`.

Đọc giá trị hỏng/ngoài range phải clamp hoặc trả về mặc định. Không ghi mỗi frame; chỉ ghi khi người dùng thay đổi control.

### `BombermanApp.java`

- Khởi tạo `AudioManager` sau `UserPreferences`.
- Đăng ký listener feedback để vừa hiện toast vừa phát cue success/error.
- Gắn một `ActionEvent` filter vào `Scene` để tạo `UI_CLICK` cho button hợp lệ, tránh sửa từng `setOnAction`.
- Truyền `AudioManager`/`GameplayAudio` cho `GameScreen`, `ResultScreen`, `SettingsPopup` hoặc cung cấp listener hẹp đúng nhu cầu.
- Gọi `audioManager.close()` trong `stop()` kể cả khi một media asset lỗi.

### `ScreenNavigator.java`

- Thêm callback `Consumer<ScreenId>` hoặc listener danh sách nhỏ.
- Gọi callback đúng một lần sau khi màn hình mới được chọn.
- Không phát/chuyển nhạc nếu `show()` nhận đúng screen hiện tại và return sớm.

### `GameScreen.java`

- Sau `MatchTracker.accept(current)`, chuyển cùng `events` vào `GameplayAudio.accept(events, currentUserId)`.
- Reset state chống lặp audio trong `onShow()`/match mới.
- Không phát bomb sound trong `keyPressed`; chỉ phát khi bom xuất hiện trong snapshot.
- Khi người chơi hiện tại chết, chỉ phát `PLAYER_DEFEATED` một lần dù các snapshot tiếp theo vẫn chứa `alive=false`.

### `ResultScreen.java`

- Trong `renderResult()` hoặc `onShow()`, phát WIN/LOSS/DRAW dựa trên `GameOverPlayerDto` của người hiện tại.
- Dùng dấu nhận dạng kết quả/match để tránh phát lại do `ClientStateListener` render lại cùng một kết quả.
- Nhạc/result cue không được restart khi chỉ có cập nhật trạng thái rematch.

### `SettingsPopup.java`

- Bỏ mô tả “No audio settings”.
- Thêm các hàng `MASTER`, `MUSIC`, `SFX`, `MUTE`.
- Dùng `Slider` 0–100, hiện nhãn phần trăm và cập nhật live.
- Mute phải giữ nguyên ba giá trị volume; unmute khôi phục đúng mức trước đó.
- Có nút/test cue nhỏ cho SFX nếu bố cục cho phép; không bắt buộc cho MVP.
- Khi popup được mở lại sau thay đổi fullscreen, control phải đọc state hiện tại, không reset mặc định.

### `game-theme.css`

- Thêm style cho slider track/thumb, hàng audio và trạng thái muted theo design system hiện có.
- Bảo đảm focus indicator bằng bàn phím vẫn nhìn thấy rõ.

### Tài liệu

- Cập nhật `docs/ui-redesign/02-man-hinh-va-dieu-huong.md` vì hiện ghi game không có âm thanh.
- Cập nhật `docs/ui-redesign/README.md`, đưa “âm thanh” ra khỏi danh sách chưa hỗ trợ.
- Thêm ghi chú điều khiển audio/cấu trúc asset vào `README.md`.

## 7. Kế hoạch triển khai theo phase

### Phase 0 — Chốt nội dung và license

1. Chọn phong cách: arcade/cartoon, hiệu ứng ngắn, rõ, không quá nặng bass.
2. Chốt 13 file trong catalog MVP và quyền sử dụng.
3. Chuẩn hóa tên, format, loudness và loop.
4. Ghi nguồn vào `assets/audio/LICENSES.md` trước khi tích hợp.

**Hoàn thành khi:** mọi asset có license rõ ràng, mở được độc lập và đúng giới hạn dung lượng.

**Kết quả thực hiện (2026-10-12):**

- Đã tạo đủ 11 SFX WAV và 2 music loop MP3 bằng generator thuần Python, không dùng sample bên thứ ba.
- Đã thêm hồ sơ nguồn và CC0 1.0 trong `assets/audio/LICENSES.md`.
- Đã sinh `manifest.json` chứa duration, sample rate, channel, dung lượng và SHA-256.
- Tổng dung lượng runtime audio: 1.476.776 byte (~1,41 MiB), thấp hơn nhiều so với ngân sách 10–12 MB.
- SFX đạt peak `-1,0 dBFS`; music sau encode đạt tối đa `-2,4 dBFS`, không clipping.
- Generator đã được chạy lặp lại để xác nhận output deterministic.
- Còn một quality gate chủ quan: nghe thử trên loa/tai nghe và chốt phong cách trước khi bắt đầu Phase 1.

### Phase 1 — Nền tảng audio và preferences

1. Thêm JavaFX `media` dependency.
2. Tạo enum/abstraction/output/manager.
3. Thêm volume + mute vào `UserPreferences`.
4. Implement resource lookup, preload SFX, lazy-load music, logging và close.
5. Unit test clamp, effective volume, missing resource và persistence.

**Hoàn thành khi:** test chạy không cần loa; app khởi động được cả khi một asset bị thiếu.

### Phase 2 — UI settings và âm thanh chung

1. Thêm slider/mute vào `SettingsPopup`.
2. Bắt `ActionEvent` cho button click.
3. Nối feedback success/error.
4. Nối `ScreenNavigator` với chính sách menu/battle music.
5. Hoàn thiện CSS và keyboard accessibility.

**Hoàn thành khi:** chỉnh volume có hiệu lực tức thời, restart app vẫn giữ giá trị, mute không làm mất volume đã chọn.

### Phase 3 — Gameplay và kết quả trận

1. Tạo `GameplayAudio` từ `FrameEvents`.
2. Nối vào `GameScreen`.
3. Nối result stinger vào `ResultScreen` với cơ chế deduplicate.
4. Áp dụng cooldown/polyphony và kiểm tra trận 4 người nhiều bom.

**Hoàn thành khi:** mỗi sự kiện logic chỉ tạo đúng cue dự kiến, không phát âm thanh giả khi command bị reject và không lặp result cue.

### Phase 4 — QA, đóng gói và tài liệu

1. Chạy toàn bộ test `:client-fx:test`.
2. Chạy client với server local và ít nhất 2 client.
3. Build `:client-fx:packageApp`, chạy app không cần JDK ngoài.
4. Build `:client-fx:packageZip`, giải nén ở thư mục mới và smoke test.
5. Kiểm tra Windows volume mixer, tai nghe/loa, mute hệ thống và trường hợp không có output device.
6. Cập nhật tài liệu và license.

**Hoàn thành khi:** bản đóng gói phát đầy đủ audio, thoát app không còn process/media thread và không phát sinh lỗi khi thiết bị âm thanh không khả dụng.

## 8. Chiến lược test

### 8.1 Unit test tự động

Tạo mới:

- `AudioManagerTest`
  - volume clamp về 0/1;
  - mute tạo effective volume 0 nhưng không mất giá trị đã lưu;
  - cùng music track không restart;
  - cooldown chặn cue trùng quá gần;
  - `close()` idempotent.
- `GameplayAudioTest`
  - bom của mình và bom người khác chọn cue đúng;
  - nhiều explosion trong một snapshot chỉ phát một cue;
  - snapshot lặp không phát lại;
  - chỉ death của user hiện tại phát `PLAYER_DEFEATED`.
- Mở rộng `UserPreferencesTest`
  - default audio values;
  - round-trip;
  - corrupted/out-of-range values.
- `AudioResourcesTest`
  - mọi resource trong enum tồn tại trên classpath;
  - không trùng path và extension đúng quy ước.

Sử dụng fake `AudioOutput` để unit test không khởi tạo `MediaPlayer`, không phụ thuộc audio device và ổn định trên CI.

### 8.2 Kiểm thử tích hợp/thủ công

| Kịch bản | Kết quả mong đợi |
|---|---|
| Mở app -> login -> home -> lobby | Menu music chạy liên tục, không restart giữa màn hình |
| Lobby -> game | Menu fade out, battle music fade in, start cue đúng một lần |
| Nhấn SPACE nhưng server từ chối | Không có bomb-place cue |
| Bom xuất hiện trong snapshot | Bomb-place cue đúng một lần |
| Nhiều ô lửa trong cùng explosion | Chỉ một explosion cue, không phát theo từng ô |
| Người hiện tại bị loại | Defeated cue một lần; tiếp tục spectate không lặp |
| Hiện result | Battle music dừng/fade; đúng victory/defeat/draw cue |
| Kéo từng slider | Chỉ category tương ứng thay đổi và thay đổi ngay |
| Mute -> chỉnh volume -> unmute | Không phát khi mute; unmute dùng giá trị mới |
| Restart app | Cài đặt audio được giữ |
| Asset hỏng/thiếu trong bản dev | Có warning; game vẫn chơi được |
| Chạy bản `packageApp` trên máy sạch | Music/SFX hoạt động, không yêu cầu cài codec/JDK riêng |

### 8.3 Kiểm tra hiệu năng

- Không đọc file hoặc tạo `AudioClip` mới trong mỗi frame.
- Preload SFX một lần; music chỉ có tối đa một active player và một player trong giai đoạn cross-fade.
- Không tạo số lượng transition/timer không giới hạn.
- Theo dõi CPU/FPS bằng F3 trong tình huống nhiều explosion; audio không được làm tụt FPS đáng kể.
- Giới hạn explosion tối đa một cue mỗi snapshot và thêm cooldown khoảng 80–120 ms nếu playtest vẫn quá dày.

## 9. Tiêu chí nghiệm thu

Tính năng được xem là hoàn tất khi:

1. Đủ toàn bộ cue và hai music track trong catalog MVP.
2. Không thay đổi protocol, server behavior hay kết quả game.
3. Button, feedback, gameplay và result đều phát đúng sự kiện, không lặp ngoài ý muốn.
4. Có master/music/SFX/mute, lưu được qua lần mở ứng dụng tiếp theo.
5. Mute đưa toàn bộ output về im lặng ngay lập tức.
6. Chuyển màn hình không làm restart cùng một track và có fade hợp lý khi đổi track.
7. Thiếu asset hoặc audio device không làm ứng dụng crash.
8. Unit test mới và test hiện có đều pass.
9. `packageApp` và `packageZip` chứa đủ file/audio runtime và chạy trên máy Windows sạch.
10. Mọi asset có thông tin license/phân phối rõ ràng.

## 10. Rủi ro và cách giảm thiểu

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| JavaFX Media codec/native library khác nhau theo OS | Dev chạy nhưng bản đóng gói không phát | Dùng WAV PCM + MP3 phổ biến; smoke test chính bản jpackage |
| Snapshot đến dồn hoặc bỏ qua | Cue dày/lỡ một sự kiện | Dùng `FrameEvents`, batch cue, cooldown; chấp nhận audio là best-effort |
| Result/state listener gọi nhiều lần | Stinger phát lặp | Deduplicate bằng match/result identity |
| Explosion nhiều làm clipping | Âm lượng khó chịu | Một cue/batch, cue gain riêng, limiter bằng thiết kế asset |
| File nhạc làm tăng ZIP | Tải bản game chậm | MP3 128–160 kbps, giới hạn tổng audio mục tiêu dưới 10–12 MB |
| Asset không rõ license | Không thể phát hành | License gate bắt buộc ở Phase 0 |
| Test headless lỗi vì media toolkit | CI không ổn định | Test qua `AudioOutput` fake; media thật chỉ smoke test |

## 11. Ước lượng và thứ tự ưu tiên

| Hạng mục | Ước lượng |
|---|---:|
| Chọn/chỉnh asset và license | 0.5–1.5 ngày |
| Audio core + preferences + test | 1 ngày |
| Settings + navigation/feedback sound | 0.5–1 ngày |
| Gameplay/result integration | 0.5–1 ngày |
| QA multiplayer + package Windows + tài liệu | 1 ngày |
| **Tổng** | **3.5–5.5 ngày công** |

Thứ tự ưu tiên nếu cần rút gọn: explosion/bomb/result SFX -> volume/mute -> battle music -> menu/UI sound -> hiệu ứng nâng cao.

## 12. Definition of Done cho từng pull request

Đề xuất chia thành ba PR/commit độc lập:

1. **Audio foundation:** dependency, asset catalog, output/manager, preferences và unit test.
2. **Audio UI:** settings, global button/feedback cues và screen music.
3. **Gameplay audio:** `FrameEvents`, result stinger, integration test thủ công, packaging và docs.

Mỗi phần phải build/test được độc lập; không commit audio asset chưa có license và không để code tham chiếu resource chưa tồn tại.
