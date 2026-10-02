# Bomberman Online Mini

Đồ án Lập trình mạng: game Bomberman desktop 2–4 người chơi mỗi phòng, hỗ trợ nhiều
phòng chạy đồng thời. Server Spring Boot là authoritative server; client JavaFX mới
và client libGDX tương thích ngược chỉ gửi input rồi render snapshot nhận qua kết nối
TCP duy trì liên tục.

## Requirements

- JDK 21 đầy đủ (`jpackage` cần thiết khi đóng gói bản Windows)
- Docker Desktop hoặc Docker Engine + Docker Compose
- Gradle Wrapper đi kèm project, không cần cài Gradle riêng
- Windows, Linux hoặc macOS để chạy server; desktop client cần môi trường đồ họa

## Project structure

```text
bomberman-online/
├── common/                         DTO, enum, protocol và length-prefixed codec
│   └── src/main/java/com/bomberman/common/
├── server/                         Spring Boot authoritative TCP server
│   └── src/main/java/com/bomberman/server/
│       ├── auth/                   register, login, logout
│       ├── network/                TCP server, session, dispatcher, writer
│       ├── lobby/                  presence và lobby broadcasts
│       ├── room/                   in-memory rooms
│       ├── game/                   game engine và per-room game loop
│       ├── match/                  match persistence và history
│       ├── ranking/                ranking queries
│       ├── repository/             Spring Data JPA repositories
│       └── user/                   User entity và online registry
├── client/                         libGDX desktop application
│   └── src/main/java/com/bomberman/client/
│       ├── network/                persistent TCP client và dispatcher
│       ├── screen/                 Login/Lobby/Room/Game/Ranking/History
│       ├── state/                  presentation state
│       └── renderer/               authoritative snapshot renderer
├── client-fx/                      JavaFX desktop application (giao diện chính)
│   └── src/main/java/com/bomberman/clientfx/
│       ├── game/                   renderer, input và hiệu ứng trận đấu
│       ├── network/                persistent TCP client và dispatcher
│       ├── state/                  presentation state
│       └── ui/                     screen, component, popup và theme
├── packaging/                      icon nguồn và icon nhiều kích thước cho Windows
├── website/                        landing page tĩnh (HTML/CSS/JS) + Nginx image
├── downloads/                      bản tải Windows do :client-fx:packageZip sinh ra (không commit)
├── Dockerfile                      Multi-stage image for the Java server
├── docker-compose.yml              MySQL + server + website deployment stack
├── env.template                    Environment variable template
├── settings.gradle
└── build.gradle
```

## Configuration

| Variable | Used by | Default | Purpose |
|---|---|---:|---|
| `BOMBERMAN_DB_NAME` | Docker | `bomberman_online` | Database name |
| `BOMBERMAN_DB_USERNAME` | Docker, server | `bomberman` | Application database user |
| `BOMBERMAN_DB_PASSWORD` | Docker, server | required | Application database password |
| `BOMBERMAN_DB_ROOT_PASSWORD` | Docker | required | MySQL administrative password |
| `BOMBERMAN_DB_URL` | server | local `bomberman_online` database | JDBC URL |
| `BOMBERMAN_DB_PORT` | Docker | `3306` | MySQL port bound to host loopback only |
| `BOMBERMAN_TCP_PORT` | Docker, server, clients | `8081` | Public gameplay TCP port |
| `BOMBERMAN_SERVER_HOST` | clients | `127.0.0.1` | TCP server hostname/IP |
| `BOMBERMAN_WEB_PORT` | Docker | `80` | Cổng HTTP của website |
| `BOMBERMAN_PUBLIC_HOST` | website | hostname của URL website | HOST hiển thị cho người chơi nhập vào game |
| `BOMBERMAN_PUBLIC_PORT` | website | `BOMBERMAN_TCP_PORT` | PORT hiển thị, chỉ đặt khi router forward cổng khác |
| `BOMBERMAN_REPO_URL` | website | trống (ẩn link) | Link mã nguồn ở footer |

Client defaults live in each module's `src/main/resources/client.properties`. They can
also be overridden with Java system properties `bomberman.server.host` and
`bomberman.tcp.port`. Trên client JavaFX, người chơi có thể nhập `host:port` ngay tại
màn hình đăng nhập; địa chỉ hợp lệ gần nhất sẽ được ghi nhớ.

## Run the deployment stack

Copy the environment template, replace both password placeholders in `.env`, then
build and start the database, game server and website. Build the Windows download
first (see [Website giới thiệu](#website-giới-thiệu)) so the website has a file to serve:

```powershell
Copy-Item env.template .env
docker compose up -d --build
docker compose ps
docker compose logs -f server
```

Linux/macOS:

```shell
cp env.template .env
docker compose up -d --build
docker compose ps
docker compose logs -f server
```

The game server is published on `${BOMBERMAN_TCP_PORT:-8081}`. MySQL is reachable
from the host only through `127.0.0.1:${BOMBERMAN_DB_PORT:-3306}` and is not exposed
to the LAN or Internet. Database data is retained in the `bomberman_mysql_data`
Docker volume. Do not commit `.env`.

If the volume was created by the older MySQL-only Compose configuration, the new
application database user will not be created automatically. For disposable local
data, recreate it once with `docker compose down -v` and then start the stack again.
Back up important data instead of deleting its volume.

For a home deployment, forward only the configured gameplay TCP port on the router
to the Docker host. Never forward the MySQL port.

## Run server locally during development

Start only MySQL from Compose:

```powershell
docker compose up -d mysql
$env:BOMBERMAN_DB_USERNAME = "bomberman"
$env:BOMBERMAN_DB_PASSWORD = "the-value-from-dot-env"
.\gradlew.bat :server:bootRun
```

Linux/macOS:

```shell
docker compose up -d mysql
export BOMBERMAN_DB_USERNAME='bomberman'
export BOMBERMAN_DB_PASSWORD='the-value-from-dot-env'
./gradlew :server:bootRun
```

The server opens gameplay TCP port `8081` by default. Gameplay does not use an HTTP,
REST or WebSocket endpoint.

## Run JavaFX desktop client

In another terminal:

```powershell
.\gradlew.bat :client-fx:run
```

Mở gallery component và tài nguyên giao diện:

```powershell
.\gradlew.bat :client-fx:run --args="--gallery"
```

Để kết nối server từ xa hoặc trong mạng LAN, chọn **Server Address** trên màn hình
đăng nhập rồi nhập `host:port`, ví dụ `192.168.1.20:8081`.

Client libGDX cũ vẫn có thể chạy bằng `.\gradlew.bat :client:run` để kiểm tra tương
thích giao thức.

## Demo with four clients

1. Start MySQL and the server.
2. Build the reusable desktop distribution:

   ```powershell
   .\gradlew.bat :client-fx:installDist
   ```

3. Open four terminals and run this command once in each terminal:

   ```powershell
   .\client-fx\build\install\client-fx\bin\client-fx.bat
   ```

4. Register four different accounts and log in.
5. Client 1 creates a room; clients 2–4 join it.
6. All players press Ready; the host presses Start Game.
7. Use WASD/arrow keys to move and Space to place a bomb.
8. On game over, open Ranking and History from the Lobby to inspect persisted data.

## Package the JavaFX client for Windows

Trên Windows có cài JDK 21 đầy đủ, chạy:

```powershell
.\gradlew.bat :client-fx:packageApp
```

Gradle tự tìm `jpackage` trong JDK 21 toolchain; không bắt buộc thêm `jpackage` vào
`PATH`. Kết quả nằm tại:

```text
client-fx/build/dist/BombermanOnline/
├── BombermanOnline.exe
├── app/
└── runtime/
```

Thư mục `runtime/` chứa Java dành riêng cho ứng dụng. Khi phát hành, nén và gửi
**toàn bộ thư mục `BombermanOnline`**, không gửi riêng file `.exe`.

Để nén sẵn cho website, dùng `packageZip` (tự chạy `packageApp` trước):

```powershell
.\gradlew.bat :client-fx:packageZip
# Đặt version khác version Gradle: -PreleaseVersion=0.2.0
```

Kết quả nằm trong `downloads/`: `BombermanOnline-<version>-Windows.zip`, file
`.sha256` và `release.json` (version, dung lượng, checksum mà website đọc để hiển
thị). Bản ZIP cũ khác version bị xóa để website chỉ phục vụ một bản.

## Website giới thiệu

Landing page tĩnh trong `website/` (HTML/CSS/JS thuần, không cần Node.js) dùng chính
màu, font và SVG của client JavaFX. Service `website` (Nginx) chạy cùng Compose:

- `BOMBERMAN_PUBLIC_HOST`/`BOMBERMAN_PUBLIC_PORT` trong `.env` là địa chỉ hiển thị và
  nút sao chép; Nginx trả về chúng qua `/config.js` khi container khởi động, nên sửa
  `.env` rồi `docker compose up -d website` là đủ, không cần build lại image.
- `downloads/` được mount chỉ đọc vào `/downloads/`. Cập nhật bản tải chỉ cần chạy lại
  `packageZip`; website đọc `release.json` nên version, dung lượng và SHA-256 luôn
  khớp file đang phục vụ.
- Website không phụ thuộc service `server`: trang tải game vẫn mở được khi game
  server tắt. Website không kiểm tra trạng thái server theo thời gian thực.

Triển khai từ đầu trên máy Windows có JDK 21 và Docker:

```powershell
.\gradlew.bat :client-fx:packageZip
Copy-Item env.template .env        # đặt mật khẩu, BOMBERMAN_PUBLIC_HOST
docker compose up -d --build
docker compose ps                  # mysql, server, website đều (healthy)
```

Kiểm tra nhanh:

```powershell
curl.exe -I http://localhost/                          # 200
curl.exe http://localhost/config.js                    # host/port đúng .env
curl.exe -I http://localhost/downloads/release.json    # 200
```

Cập nhật client mới: chạy lại `packageZip`, không cần restart container. Cập nhật mã
website: `docker compose up -d --build website`. Không dùng `docker compose down -v`
khi cập nhật vì lệnh này xóa volume MySQL.

### Router, DNS và HTTPS

| Dịch vụ | Cổng host | Forward trên router |
|---|---:|---|
| Website HTTP | `BOMBERMAN_WEB_PORT` (80) | Có |
| Game TCP | `BOMBERMAN_TCP_PORT` (8081) | Có |
| MySQL | `127.0.0.1:BOMBERMAN_DB_PORT` | Không bao giờ |

- Có thể dùng một hostname/DDNS cho cả hai; khi đó để trống `BOMBERMAN_PUBLIC_HOST`.
- Nếu website đi qua Cloudflare proxy, hostname game phải là bản ghi **DNS only** và
  phải đặt vào `BOMBERMAN_PUBLIC_HOST`, vì proxy HTTP không chuyển tiếp TCP thô.
- Kiểm tra IP WAN của router trùng IP public; nếu ISP dùng CGNAT, port forwarding
  không hoạt động (cần IPv4 public, tunnel hoặc VPS).
- Website hiện chạy HTTP nên Chrome/Edge cảnh báo khi tải ZIP; trang có checksum
  SHA-256 để người chơi tự kiểm tra. Nên bổ sung HTTPS (Caddy hoặc reverse proxy có TLS) khi domain ổn định.
- Kết nối game là TCP chưa mã hóa: người chơi nên dùng mật khẩu riêng cho game.

## Architecture

```text
libGDX input
    -> GameClientController
    -> ordered background TCP writer
    -> length-prefixed JSON frame
    -> ClientSession / MessageDispatcher
    -> validate authenticated session + room
    -> enqueue GameCommand
    -> single-threaded RoomGameLoop
    -> authoritative BombermanGame mutation
    -> immutable GAME_STATE snapshot
    -> SessionWriter lock
    -> client AtomicReference latest snapshot
    -> libGDX render thread
```

- `common` contains protocol models only and has no business logic.
- JPA entities stay inside `server` and are never serialized to clients.
- Room state is in RAM and guarded by `RoomManager`'s mutation lock.
- Each room owns one scheduled game-loop thread and one bounded command queue.
- Network threads never directly move players, place bombs or kill players.
- Each client session owns a locked `SessionWriter`, preventing concurrent frame
  interleaving on its `OutputStream`.
- Completed matches and ranking counters are written transactionally to MySQL.

## TCP protocol

Every message uses this framing:

```text
4-byte signed big-endian payload length
+
UTF-8 JSON NetworkMessage
```

The decoder uses exact reads, so it supports partial TCP reads and multiple messages
in one stream. Invalid lengths and malformed JSON close the offending session. Maximum
JSON payload size is 1 MiB.

`NetworkMessage` contains:

```json
{
  "type": "MOVE",
  "requestId": "uuid",
  "payload": { "direction": "UP" }
}
```

### Message types

- Connection: `PING`, `PONG`, `ERROR`
- Authentication: `REGISTER_REQUEST`, `REGISTER_RESPONSE`, `LOGIN_REQUEST`,
  `LOGIN_RESPONSE`, `LOGOUT`
- Lobby: `ONLINE_USERS_REQUEST`, `ONLINE_USERS_UPDATE`, `ROOM_LIST_REQUEST`,
  `ROOM_LIST_UPDATE`
- Room: `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `ROOM_STATE`, `READY`, `START_GAME`
- Gameplay input: `MOVE`, `PLACE_BOMB`
- Gameplay output: `GAME_STATE`, `PLAYER_DIED`, `GAME_OVER`
- Post-game: `PLAY_AGAIN`
- Ranking: `RANKING_REQUEST`, `RANKING_RESPONSE`
- History: `HISTORY_REQUEST`, `HISTORY_RESPONSE`

## Authoritative gameplay and concurrency

- Client sends direction or bomb intent, never a new coordinate.
- Server validates map bounds, walls, bombs, player state and room membership.
- TCP accept/read loops use Java 21 virtual threads.
- Per-room `ScheduledExecutorService` runs at 20 ticks/second.
- Full `GAME_STATE` snapshots are broadcast at 10 snapshots/second.
- Commands use a bounded `ArrayBlockingQueue`; only the room loop mutates game state.
- Disconnect is converted to `DisconnectGameCommand`, so death and winner evaluation
  remain on the authoritative loop.
- `ConnectionManager` uses snapshots of a concurrent session map for broadcasts.
- `SessionWriter` serializes all writes belonging to one socket.

## Match score and ranking

Scores are stored as integer half-point units to avoid floating-point errors:

| Result | Stored units | Displayed score |
|---|---:|---:|
| WIN | 2 | 1.0 |
| DRAW | 1 | 0.5 |
| LOSS | 0 | 0.0 |

Ranking order is `totalScore DESC`, then `totalWins DESC`.

## Tests and build

Run all unit and integration tests:

```powershell
.\gradlew.bat clean test build
```

The integration suite covers authentication, rooms, four-client authoritative state,
two independent four-player rooms, movement, bombs, disconnect-driven game over,
persistence, ranking and history.
