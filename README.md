# Bomberman Online Mini

Đồ án Lập trình mạng: game Bomberman desktop 2–4 người chơi mỗi phòng, hỗ trợ nhiều
phòng chạy đồng thời. Server Spring Boot là authoritative server; client libGDX chỉ
gửi input và render snapshot nhận qua persistent TCP connection.

## Requirements

- JDK 21
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
├── docker-compose.yml              MySQL only
├── settings.gradle
└── build.gradle
```

## Configuration

| Variable | Used by | Default | Purpose |
|---|---|---:|---|
| `BOMBERMAN_DB_PASSWORD` | Docker, server | required | MySQL root password |
| `BOMBERMAN_DB_USERNAME` | server | `root` | Database username |
| `BOMBERMAN_DB_URL` | server | local `bomberman_online` database | JDBC URL |
| `BOMBERMAN_DB_PORT` | Docker | `3306` | Published MySQL port |
| `BOMBERMAN_TCP_PORT` | server, client | `8081` | Gameplay TCP port |
| `BOMBERMAN_SERVER_HOST` | client | `127.0.0.1` | TCP server hostname/IP |

Client defaults live in `client/src/main/resources/client.properties`. They can also
be overridden with Java system properties `bomberman.server.host` and
`bomberman.tcp.port`.

## Start MySQL

PowerShell:

```powershell
$env:BOMBERMAN_DB_PASSWORD = "choose-a-local-password"
docker compose up -d
docker compose ps
```

Linux/macOS:

```shell
export BOMBERMAN_DB_PASSWORD='choose-a-local-password'
docker compose up -d
docker compose ps
```

Only MySQL is containerized. Database data is retained in the
`bomberman_mysql_data` Docker volume.

## Run server

Set `BOMBERMAN_DB_PASSWORD` to the same value used by Docker, then run:

```powershell
.\gradlew.bat :server:bootRun
```

Linux/macOS:

```shell
./gradlew :server:bootRun
```

The server opens gameplay TCP port `8081` by default. Gameplay does not use an HTTP,
REST or WebSocket endpoint.

## Run desktop client

In another terminal:

```powershell
.\gradlew.bat :client:run
```

For a remote server:

```powershell
$env:BOMBERMAN_SERVER_HOST = "192.168.1.20"
$env:BOMBERMAN_TCP_PORT = "8081"
.\gradlew.bat :client:run
```

## Demo with four clients

1. Start MySQL and the server.
2. Build the reusable desktop distribution:

   ```powershell
   .\gradlew.bat :client:installDist
   ```

3. Open four terminals and run this command once in each terminal:

   ```powershell
   .\client\build\install\client\bin\client.bat
   ```

4. Register four different accounts and log in.
5. Client 1 creates a room; clients 2–4 join it.
6. All players press Ready; the host presses Start Game.
7. Use WASD/arrow keys to move and Space to place a bomb.
8. On game over, open Ranking and History from the Lobby to inspect persisted data.

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
