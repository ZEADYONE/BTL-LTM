# Kế hoạch tính năng bạn bè và tương tác phòng

> **Trạng thái:** Phase 0–2 đã triển khai ngày 2026-10-12; Phase 3–5 chưa triển khai
> **Ngày lập:** 2026-10-12  
> **Phạm vi chính:** `common`, `server`, JavaFX client `client-fx`  
> **Mục tiêu:** Bổ sung danh sách bạn bè, gửi/duyệt lời mời kết bạn, mời bạn vào phòng và quyền kick thành viên của host mà vẫn giữ server là nguồn trạng thái authoritative.

## 1. Kết luận kiến trúc

Tính năng được chia thành hai loại dữ liệu:

- Quan hệ bạn bè là dữ liệu lâu dài, lưu trong MySQL.
- Lời mời vào phòng và danh sách người bị kick là dữ liệu tạm thời theo vòng đời phòng, lưu trong bộ nhớ server.

Mọi thao tác đều đi qua TCP protocol hiện có. Client chỉ gửi ý định; server xác thực danh tính từ `ClientSession`, kiểm tra quyền và trả snapshot mới. Client không được tự thêm bạn, tự vào phòng hoặc tự loại người chơi khỏi state cục bộ.

Client chính là `client-fx`. Module `client` (libGDX) chỉ cần tiếp tục build được và bỏ qua các message social chưa có UI; không triển khai giao diện tính năng mới cho module cũ trong đợt này.

## 2. Quy tắc sản phẩm chốt cho MVP

### 2.1 Bạn bè

1. Thêm bạn bằng **username chính xác**, sau khi trim; giữ nguyên quy tắc phân biệt hoa/thường hiện tại của tài khoản.
2. Không được tự gửi lời mời cho chính mình.
3. Một cặp người dùng chỉ có tối đa một quan hệ `PENDING` hoặc `ACCEPTED`.
4. Nếu A đã gửi B và B gửi lại A khi yêu cầu còn chờ, server tự chuyển thành `ACCEPTED`.
5. Người nhận có thể chấp nhận hoặc từ chối; người gửi có thể hủy yêu cầu đang chờ.
6. Bạn bè có thể xóa nhau. Xóa chỉ xóa quan hệ bạn bè, không kick khỏi phòng đang chơi.
7. Danh sách hiển thị trạng thái `OFFLINE`, `FREE`, `IN_ROOM`, `PLAYING`.
8. Giới hạn mặc định: 100 bạn, 20 yêu cầu đi đang chờ, tối đa 5 lần gửi yêu cầu/phút/tài khoản. Các giá trị đặt thành constant/config để đổi được.

### 2.2 Mời bạn vào phòng

1. Chỉ mời người đã là bạn bè.
2. Người mời phải đang ở phòng `WAITING`; người được mời phải online ở trạng thái `FREE`.
3. Mọi thành viên trong phòng được mời bạn của chính họ, không chỉ host.
4. Lời mời sống 60 giây, có `inviteId` ngẫu nhiên và chỉ được dùng một lần.
5. Khi người nhận bấm Accept, server kiểm tra lại phòng còn tồn tại, còn chỗ, vẫn `WAITING`, người nhận vẫn `FREE` và quan hệ bạn bè vẫn tồn tại.
6. Accept thực hiện join trực tiếp trên server; client không gửi tiếp một `JOIN_ROOM` bằng `roomId` để tránh race condition.
7. Lời mời tự mất hiệu lực khi phòng bị xóa, trận bắt đầu, phòng đầy, người mời rời phòng, người nhận vào phòng khác, đăng xuất hoặc hết hạn.
8. Một cặp `room + invitee` chỉ có một lời mời đang hoạt động. Giới hạn mặc định ba lời mời/phút từ một người tới cùng một người nhận.

### 2.3 Kick khỏi phòng

1. Chỉ host hiện tại được kick.
2. Chỉ kick khi phòng ở trạng thái `WAITING`; không kick khi trận đang `PLAYING` hoặc đã `FINISHED`.
3. Host không thể kick chính mình. Muốn rời phòng dùng `LEAVE_ROOM`; cơ chế chuyển host hiện có tiếp tục được giữ.
4. Người bị kick trở về Home, vẫn đăng nhập và có trạng thái `FREE`.
5. Người bị kick không thể join trực tiếp lại cùng phòng trong vòng đời phòng đó.
6. Chỉ lời mời mới do **host hiện tại** gửi mới cho phép người đã bị kick quay lại. Khi Accept, server xóa cờ kick cho người đó.
7. Thành viên khác không thể dùng invite để vô hiệu hóa quyết định kick của host.

## 3. Phạm vi và ngoài phạm vi

### 3.1 Phạm vi MVP

- Màn hình Friends có danh sách bạn, trạng thái online và yêu cầu đến/đi.
- Gửi yêu cầu bằng username; accept, decline, cancel và remove friend.
- Badge số yêu cầu đến trên Home.
- Popup mời bạn trong Room Lobby.
- Popup toàn cục khi nhận lời mời phòng; Accept/Decline.
- Nút kick trên thẻ thành viên dành riêng cho host và hộp xác nhận.
- Push cập nhật trạng thái bạn bè, yêu cầu, lời mời và room state theo thời gian thực.
- Unit test, integration test nhiều TCP client và smoke test JavaFX.

### 3.2 Ngoài phạm vi

- Chat riêng/chat nhóm, block user, report user.
- Gợi ý bạn bè, tìm kiếm gần đúng hoặc danh bạ người chơi toàn hệ thống.
- Thông báo offline, email/push notification.
- Party tồn tại ngoài phòng, spectator và chuyển quyền host thủ công.
- Lịch sử lời mời phòng.
- Social UI cho module libGDX `client` cũ.

## 4. Mô hình dữ liệu

### 4.1 Bảng `friend_relationships`

Đề xuất dùng một hàng cho mỗi cặp người dùng, chuẩn hóa cặp ID để khóa unique chặn cả hai chiều A→B và B→A.

| Cột | Kiểu đề xuất | Quy tắc |
|---|---|---|
| `id` | `BIGINT` | PK, auto increment |
| `user_low_id` | `BIGINT` | FK `users(id)`, ID nhỏ hơn |
| `user_high_id` | `BIGINT` | FK `users(id)`, ID lớn hơn |
| `requested_by_user_id` | `BIGINT` | FK `users(id)`, phải thuộc cặp |
| `status` | `VARCHAR(16)` | `PENDING` hoặc `ACCEPTED` |
| `created_at` | `TIMESTAMP` | thời điểm gửi lần đầu |
| `updated_at` | `TIMESTAMP` | thời điểm thay đổi gần nhất |
| `version` | `BIGINT` | optimistic locking bằng `@Version` |

Ràng buộc/index:

- `UNIQUE(user_low_id, user_high_id)` là bắt buộc.
- Index `(user_low_id, status)` và `(user_high_id, status)` để lấy danh sách của một user.
- Check `user_low_id < user_high_id` nếu MySQL version triển khai bảo đảm check constraint hoạt động.
- Không serialize entity JPA ra network; luôn map sang DTO trong `common`.

Khi decline, cancel hoặc remove, xóa hàng. Lịch sử quan hệ không nằm trong MVP. `@Version` cùng unique constraint xử lý hai request đồng thời; service vẫn bắt `DataIntegrityViolationException` và đọc lại trạng thái để trả kết quả idempotent.

### 4.2 Lời mời phòng

Không tạo bảng DB. `RoomInviteManager` giữ record bất biến trong bộ nhớ:

```java
RoomInvite(
    UUID inviteId,
    String roomId,
    long inviterUserId,
    long inviteeUserId,
    Instant createdAt,
    Instant expiresAt,
    RoomInviteStatus status
)
```

Chỉ các lời mời `PENDING` nằm trong map active. Cleanup chạy trên một scheduler duy nhất và cũng được gọi lười trước các thao tác đọc/ghi. Restart server làm mất invite là hành vi chấp nhận được; quan hệ bạn bè vẫn còn.

### 4.3 Danh sách bị kick

Thêm `Set<Long> kickedUserIds` vào `GameRoom`:

- Không đưa tập này vào `RoomStateDto`.
- `joinRoom` thông thường từ chối ID có trong tập.
- `joinRoomByInvite` chỉ bỏ cờ khi invite được gửi bởi host hiện tại.
- Khi phòng rỗng và bị xóa, tập tự biến mất cùng aggregate.

## 5. Mở rộng protocol trong `common`

### 5.1 `MessageType`

Thêm các loại message:

```text
FRIEND_LIST_REQUEST
FRIEND_LIST_UPDATE
FRIEND_REQUEST_SEND
FRIEND_REQUEST_RESPOND
FRIEND_REQUEST_CANCEL
FRIEND_REMOVE
ROOM_INVITE_SEND
ROOM_INVITE_RECEIVED
ROOM_INVITE_RESPOND
ROOM_INVITE_STATUS
KICK_ROOM_MEMBER
ROOM_MEMBER_REMOVED
```

Quy ước giữ nguyên:

- Response trực tiếp dùng cùng `requestId` với request.
- Push tự phát từ server có `requestId = null`.
- Lỗi tiếp tục trả `MessageType.ERROR` với `ErrorResponse`.
- Snapshot mới được áp dụng trước khi `PendingRequests.complete(message)` hoàn thành future.

### 5.2 DTO bạn bè

```java
enum FriendRelationshipStatus { PENDING, ACCEPTED }
enum FriendPresenceStatus { OFFLINE, FREE, IN_ROOM, PLAYING }

record FriendDto(
    long userId,
    String username,
    FriendPresenceStatus presence
) {}

record FriendRequestDto(
    long relationshipId,
    long userId,
    String username,
    FriendPresenceStatus presence,
    long requestedAtEpochMillis
) {}

record FriendListUpdate(
    List<FriendDto> friends,
    List<FriendRequestDto> incomingRequests,
    List<FriendRequestDto> outgoingRequests
) {}

record SendFriendRequest(String username) {}
record RespondFriendRequest(long relationshipId, boolean accept) {}
record CancelFriendRequest(long relationshipId) {}
record RemoveFriendRequest(long friendUserId) {}
```

`FRIEND_LIST_UPDATE` là snapshot đầy đủ và được sort ổn định:

1. bạn online trước, sau đó username;
2. yêu cầu đến/đi mới nhất trước.

Sau mỗi mutation, server gửi snapshot có `requestId` cho người thao tác và push snapshot không `requestId` cho người còn lại nếu đang online. Client phát hiện relationship ID mới trong `incomingRequests` để hiện toast/badge, không cần thêm một message notification riêng.

### 5.3 DTO lời mời phòng

```java
enum RoomInviteStatus { SENT, ACCEPTED, DECLINED, EXPIRED, CANCELLED }

record SendRoomInviteRequest(long friendUserId) {}

record RoomInviteDto(
    String inviteId,
    String roomId,
    String roomName,
    long inviterUserId,
    String inviterUsername,
    long expiresAtEpochMillis
) {}

record RespondRoomInviteRequest(String inviteId, boolean accept) {}

record RoomInviteStatusUpdate(
    String inviteId,
    RoomInviteStatus status
) {}
```

Luồng reply:

- Gửi thành công: người gửi nhận `ROOM_INVITE_STATUS(SENT)` có cùng `requestId`; người nhận nhận `ROOM_INVITE_RECEIVED` không `requestId`.
- Decline: người nhận nhận `ROOM_INVITE_STATUS(DECLINED)` có cùng `requestId`; người gửi nhận push cùng trạng thái.
- Accept thành công: người nhận nhận `ROOM_STATE` có cùng `requestId`; mọi thành viên nhận room snapshot mới. Người gửi nhận thêm push `ROOM_INVITE_STATUS(ACCEPTED)`.
- Hết hạn/hủy: hai phía đang online nhận push trạng thái tương ứng khi còn cần thiết.

### 5.4 DTO kick

```java
record KickRoomMemberRequest(long targetUserId) {}

enum RoomRemovalReason { HOST_KICK }

record RoomMemberRemovedNotice(
    String roomId,
    String roomName,
    RoomRemovalReason reason,
    long actorUserId,
    String actorUsername
) {}
```

Khi kick thành công:

1. Host và các thành viên còn lại nhận `ROOM_STATE` mới; bản gửi host mang `requestId` của lệnh kick.
2. Người bị kick nhận `ROOM_MEMBER_REMOVED`, sau đó nhận `ROOM_STATE(member=false, room=null)`.
3. Lobby nhận `ROOM_LIST_UPDATE`/presence update theo quy tắc hiện có.

### 5.5 Mã lỗi

Thêm `FriendResultCode`:

```text
INVALID_REQUEST, NOT_AUTHENTICATED, USER_NOT_FOUND, CANNOT_ADD_SELF,
ALREADY_FRIENDS, REQUEST_ALREADY_PENDING, REQUEST_NOT_FOUND,
NOT_REQUEST_RECIPIENT, NOT_REQUEST_SENDER, NOT_FRIENDS,
FRIEND_LIMIT_REACHED, PENDING_LIMIT_REACHED, RATE_LIMITED
```

Thêm `RoomInviteResultCode`:

```text
INVALID_REQUEST, NOT_AUTHENTICATED, NOT_FRIENDS, INVITER_NOT_IN_ROOM,
ROOM_NOT_WAITING, INVITEE_OFFLINE, INVITEE_BUSY, DUPLICATE_INVITE,
INVITE_NOT_FOUND, INVITE_EXPIRED, INVITE_NOT_RECIPIENT, ROOM_FULL,
TARGET_KICKED, RATE_LIMITED
```

Mở rộng `RoomResultCode` cho kick/join:

```text
TARGET_NOT_IN_ROOM, CANNOT_KICK_SELF, KICKED_FROM_ROOM
```

`NOT_HOST` và `ROOM_NOT_WAITING` được tái sử dụng. Human-readable message được đặt ở handler/server; client dùng `error.message()` và không tự diễn giải logic domain.

## 6. Kiến trúc server

### 6.1 Package mới `server/social`

```text
com.bomberman.server.social
├── FriendRelationship.java
├── FriendRelationshipRepository.java
├── FriendService.java
├── FriendSnapshot.java
├── FriendMessageHandler.java
├── FriendUpdatePublisher.java
├── SocialRateLimiter.java
├── RoomInvite.java
├── RoomInviteManager.java
└── RoomInviteMessageHandler.java
```

Trách nhiệm:

- `FriendService`: transaction, chuẩn hóa cặp ID, kiểm tra quyền và thay đổi DB.
- `FriendUpdatePublisher`: dựng snapshot có presence và gửi đúng session của các user liên quan.
- `SocialRateLimiter`: giới hạn friend request/invite trong bộ nhớ theo user/pair; cleanup định kỳ.
- `RoomInviteManager`: tạo, lấy, consume, expire và invalidation invite.
- Các message handler: parse DTO, lấy user từ session, gọi service, gửi response/push.

### 6.2 Bổ sung tra cứu session/presence

`OnlineUserRegistry` cần API đọc an toàn:

```java
Optional<OnlineUserPresence> find(long userId);
```

Không truyền `ClientSession` vào service persistence. Publisher lấy `sessionId` từ registry rồi dùng `ConnectionManager.find(sessionId)`. Cách này giữ nguyên quy tắc một tài khoản chỉ có một session online.

### 6.3 Presence của bạn bè

Sau các điểm đổi trạng thái sau, gọi `FriendUpdatePublisher.publishPresenceChanged(userId)`:

- login thành công;
- logout/disconnect;
- create/join/leave/kick room;
- start game;
- rematch/chuyển về `IN_ROOM` nếu cần;
- game kết thúc nhưng người chơi vẫn thuộc phòng.

Publisher chỉ gửi cho chính user và những người bạn đang online, không broadcast toàn bộ dữ liệu bạn bè tới mọi connection. Query repository chỉ lấy ID bạn của user vừa đổi trạng thái.

### 6.4 Thay đổi `RoomManager`

Thêm các operation atomic dưới `mutationLock` hiện có:

```java
RoomMemberRemovalResult kickMember(long requesterId, long targetId);
RoomOperationResult joinRoomByInvite(
    AuthenticatedUser invitee,
    String sessionId,
    String roomId,
    long inviterUserId
);
boolean isCurrentMember(String roomId, long userId);
```

`kickMember` trả cả snapshot phòng sau khi xóa và `RoomPlayerSnapshot` của target để handler biết session nào cần gửi notice. Không gọi network I/O khi đang giữ `mutationLock`.

`joinRoom` gọi chung một private method với `joinRoomByInvite`, nhưng chỉ nhánh invite hợp lệ từ host mới được bypass `kickedUserIds`. Tất cả điều kiện tồn tại phòng, trạng thái và capacity được kiểm tra trong cùng lock.

### 6.5 Invalidation invite theo lifecycle phòng

Sau khi mutation phòng đã hoàn tất và lock phòng đã nhả, handler gọi invite manager:

- leave/kick: hủy invite do user đó gửi nếu họ không còn trong phòng;
- phòng rỗng: hủy mọi invite theo `roomId`;
- start game: hủy mọi invite theo `roomId`;
- accept/join làm phòng đầy: hủy invite còn lại của phòng;
- logout: hủy invite gửi bởi hoặc gửi tới user.

Quy tắc thứ tự lock nếu một service cần cả hai: invite lock trước, room lock sau. Callback từ room sang invite luôn chạy sau khi room lock đã được nhả để tránh deadlock.

### 6.6 Dispatcher và cleanup

`MessageDispatcher` thêm route cho friend, invite và kick. `AuthMessageHandler.cleanupSession` gọi cleanup invite và publish friend presence sau khi trạng thái offline đã được cập nhật.

Các sender helper phải tiếp tục đóng session khi `IOException`, nhưng không để lỗi gửi tới một client làm hỏng mutation đã commit hoặc chặn push tới client khác.

## 7. Kiến trúc JavaFX client

### 7.1 State và controller

Mở rộng `client-fx/state/ClientState`:

```text
friends
incomingFriendRequests
outgoingFriendRequests
pendingRoomInvites
```

- Tất cả collection là defensive copy và chỉ đổi trên JavaFX Application Thread.
- `logout()` xóa toàn bộ social state.
- Invite đã accept/decline/expire phải bị remove idempotently theo `inviteId`.

Mở rộng `GameClientController` bằng các method trả `CompletableFuture<NetworkMessage>`:

```java
requestFriends()
sendFriendRequest(String username)
respondFriendRequest(long relationshipId, boolean accept)
cancelFriendRequest(long relationshipId)
removeFriend(long friendUserId)
sendRoomInvite(long friendUserId)
respondRoomInvite(String inviteId, boolean accept)
kickRoomMember(long targetUserId)
```

Mutation UI dùng future để loading/disable nút. Không suy luận thành công từ việc bấm; chỉ cập nhật sau message server.

### 7.2 Dispatcher

`ClientMessageDispatcher` xử lý:

- `FRIEND_LIST_UPDATE`: thay snapshot; nếu push chứa incoming relationship ID mới thì hiện toast.
- `ROOM_INVITE_RECEIVED`: thêm invite vào queue, kích hoạt popup presenter.
- `ROOM_INVITE_STATUS`: xóa/cập nhật invite và hiện feedback phù hợp.
- `ROOM_MEMBER_REMOVED`: hiện toast “Host removed you from the room”.
- `ROOM_STATE(member=false)`: dùng luồng hiện có để xóa room và về Home.

`pendingRequests.complete(message)` vẫn gọi sau khi state được áp dụng. `ERROR` cho action social đi qua feedback chung, trừ khi màn hình chủ động dùng quiet error cho một race dự kiến.

### 7.3 Màn hình Friends

Thêm `ScreenId.FRIENDS` và `FriendsScreen`, đăng ký trong `BombermanApp`.

Bố cục đề xuất:

```text
+--------------------------------------------------------------+
| < BACK       FRIENDS                         3 ONLINE         |
| [ username________________ ] [ ADD FRIEND ]                   |
| [ FRIENDS ] [ REQUESTS (2) ]                                 |
|--------------------------------------------------------------|
| avatar  Alice   FREE       [INVITE*] [REMOVE]                 |
| avatar  Bob     PLAYING              [REMOVE]                 |
| avatar  Carol   OFFLINE              [REMOVE]                 |
+--------------------------------------------------------------+
```

- Tab Friends: avatar/team color xác định từ `userId`, username, presence badge, Remove.
- Tab Requests: Incoming có Accept/Decline; Outgoing có Cancel.
- `INVITE` chỉ hiện khi current user đang trong phòng `WAITING`, target `FREE` và chưa ở trong phòng. Trường hợp thông thường từ Home không có room nên nút này ẩn.
- Empty state riêng cho “chưa có bạn” và “không có yêu cầu”.
- Add field dùng Enter để submit, trim, chặn rỗng ở client nhưng server vẫn kiểm tra lại.

Trên Home, thêm tile `FRIENDS` với badge yêu cầu đến. Để không làm tràn menu 1280×720, thay vị trí tile `HELP`; chuyển Help thành nút icon nhỏ ở header hoặc đặt trong Settings. Việc này phải được kiểm tra lại bằng Gallery/screenshot 1280×720.

### 7.4 Popup mời bạn trong Room Lobby

Thêm nút `INVITE FRIENDS` gần tên phòng hoặc phía trên lưới slot, không chen vào hàng READY/START ở đáy.

Popup hiển thị:

- bạn online `FREE` trước, có nút Invite;
- `IN_ROOM`, `PLAYING`, `OFFLINE` vẫn có thể hiển thị nhưng nút disabled và có lý do;
- người đã ở cùng phòng bị loại khỏi danh sách;
- sau khi gửi, nút chuyển `SENT` đến khi invite expire/decline hoặc popup đóng.

Nếu user chưa có bạn, popup có CTA mở Friends sau khi rời phòng; không tự điều hướng khỏi Room Lobby.

### 7.5 Popup nhận lời mời

Tạo `RoomInvitePopup` và `RoomInviteCoordinator`:

- Popup ghi người mời, tên phòng, thời điểm hết hạn và hai nút Accept/Decline.
- Queue theo `inviteId`; không ghi đè Settings, confirm kick hoặc popup đang mở.
- Mở invite kế tiếp khi modal hiện tại đóng; trước khi mở phải kiểm tra `expiresAt`.
- Accept/Decline đặt loading, disable cả hai nút để chống double click.
- Accept thành công đóng popup khi `ROOM_STATE` authoritative tới và navigator chuyển `ROOM_LOBBY`.

`AppShell` hiện chỉ có một modal. Cần thêm hàng đợi modal hoặc callback khi modal đóng; không gọi `showModal` trực tiếp làm mất popup đang hiển thị.

### 7.6 Kick trên slot

Mở rộng `SlotCard.showPlayer` nhận `userId`/`kickable` và callback kick:

- Chỉ hiện nút đỏ nhỏ khi current user là host, target không phải host/current user và room `WAITING`.
- Nút có tooltip/accessibility text “Remove <username>”.
- Bấm mở confirm: `Remove <username> from this room?`.
- Sau confirm, nút loading/disabled cho tới khi nhận room snapshot hoặc error.
- Không xóa slot trước khi server trả `ROOM_STATE`.

## 8. Luồng nghiệp vụ

### 8.1 Gửi và chấp nhận kết bạn

```text
A nhập username B
  -> FRIEND_REQUEST_SEND
  -> server tìm B, chuẩn hóa cặp ID, transaction insert PENDING
  -> FRIEND_LIST_UPDATE cho A (correlated)
  -> FRIEND_LIST_UPDATE push cho B nếu online

B bấm Accept
  -> FRIEND_REQUEST_RESPOND
  -> server kiểm tra B là người nhận, đổi ACCEPTED
  -> snapshot mới cho B (correlated) và A (push)
```

Hai request chéo được serialize bởi unique pair/transaction; request thứ hai đọc hàng pending và chuyển thẳng sang `ACCEPTED`.

### 8.2 Mời và accept vào phòng

```text
A đang ở room R, gửi invite tới bạn B
  -> server kiểm tra friendship + presence + room
  -> lưu invite TTL, ACK SENT cho A
  -> push ROOM_INVITE_RECEIVED tới B

B bấm Accept
  -> server atomically consume invite
  -> RoomManager.joinRoomByInvite kiểm tra lại R
  -> presence B = IN_ROOM
  -> ROOM_STATE correlated cho B, push cho thành viên còn lại
  -> lobby/friend presence updates
```

Nếu phòng vừa đầy hoặc vừa start trước Accept, join thất bại, invite bị invalid và B nhận `ERROR`; không có state cục bộ nào được giữ lại.

### 8.3 Kick

```text
Host H chọn kick B
  -> KICK_ROOM_MEMBER(targetUserId=B)
  -> RoomManager kiểm tra H là host và room WAITING
  -> xóa B + thêm B vào kickedUserIds trong cùng lock
  -> presence B = FREE
  -> ROOM_MEMBER_REMOVED + ROOM_STATE(member=false) cho B
  -> ROOM_STATE mới cho H/các thành viên
  -> cập nhật lobby, friend presence, invalid invite
```

## 9. Thay đổi theo file/package

### `common`

- `enums/MessageType.java`: thêm message social/room moderation.
- Thêm enum presence, relationship, invite và removal reason.
- Thêm DTO trong `common/dto` theo mục 5.
- `RoomResultCode.java`: thêm lỗi kick/rejoin.
- `MessageCodecTest`: round-trip DTO/message mới, null payload và enum.

### `server`

- `user/User.java`: không thêm collection hai chiều để tránh load graph lớn.
- `repository`: thêm `FriendRelationshipRepository` với query lấy quan hệ theo hai phía.
- `social/*`: entity, service, publisher, handlers, invite manager và rate limiter.
- `user/OnlineUserRegistry.java`: tra presence theo user ID.
- `network/MessageDispatcher.java`: route message mới.
- `room/GameRoom.java`: giữ `kickedUserIds`.
- `room/RoomManager.java`: kick và invited join atomic.
- `room/RoomMessageHandler.java`: xử lý kick, cập nhật presence, room/lobby/friend push.
- `auth/AuthMessageHandler.java`: cleanup invite và push presence khi login/logout/disconnect.
- `game/GameSessionManager` hoặc điểm kết thúc game: publish presence phù hợp với lifecycle hiện tại.
- `application.properties`: đưa TTL/limit thành property nếu không dùng constant.

### `client-fx`

- `network/GameClientController.java`: action methods.
- `network/ClientMessageDispatcher.java`: decode/publish social messages.
- `state/ClientState.java`: social snapshots và invite queue.
- `ui/ScreenId.java`, `ScreenNavigator`, `BombermanApp`: đăng ký Friends và coordinator.
- Thêm `ui/screen/FriendsScreen.java`.
- Thêm `ui/popup/FriendInvitePopup.java`, `RoomInvitePopup.java` hoặc tên tương đương.
- `ui/screen/HomeScreen.java`: Friends tile/badge.
- `ui/screen/RoomLobbyScreen.java`: invite button và kick callbacks.
- `ui/component/SlotCard.java`: kick affordance.
- `ui/AppShell.java`: modal queue/callback.
- `resources/css/game-theme.css`: friend row, presence badge, request badge, invite/kick controls.
- `asset/AssetIds.java`: chỉ thêm asset nếu icon hiện có/Ikonli không đáp ứng; ưu tiên icon vector hiện có.

### `client` cũ

- Không thêm UI.
- Bảo đảm switch có `default` và module compile/test với `MessageType` mới.
- Ghi rõ legacy client sẽ bỏ qua social push; invite gửi tới client này sẽ hết hạn.

## 10. Lộ trình triển khai

### Phase 0 — Chốt contract và test skeleton ✅

Mục tiêu: khóa hành vi trước khi sửa DB/UI.

- [x] Chốt toàn bộ quy tắc ở mục 2 và giới hạn cấu hình.
- [x] Thêm enums/DTO/message types vào `common`.
- [x] Viết codec tests cho payload mới.
- [x] Ghi test inventory cho FriendService, RoomManager kick/invited join và invite expiry; test executable sẽ được thêm cùng production service để không để test `@Disabled` trong suite.
- [x] Cập nhật tài liệu protocol tại `docs/social-protocol.md` và README.

**Exit gate:** `:common:test` pass; server/client đều compile với protocol mới; không có tên message/DTO chưa thống nhất.

**Kết quả Phase 0 (2026-10-12):** `gradlew test` pass toàn bộ: `common` 15,
`server` 60, `client-fx` 95 và `client` 1 test; không có failure, error hoặc test bị skip.

### Phase 1 — Friendship persistence và server API ✅

- [x] Tạo entity/repository với unique normalized pair, index, FK và optimistic version.
- [x] Cài đặt send/respond/cancel/remove/list transaction.
- [x] Cài rate limit và giới hạn số bạn/pending.
- [x] Tạo handler/dispatcher routes.
- [x] Push snapshot cho hai phía và presence updates qua login/logout, room và game lifecycle.
- [x] Test H2/MySQL-compatible constraint, request chéo, concurrent duplicate và lifecycle hai TCP client.

**Exit gate:** Hai TCP client có thể hoàn tất toàn bộ vòng đời kết bạn; restart server vẫn giữ friend list.

**Kết quả Phase 1 (2026-10-12):** friendship được lưu trong bảng
`friend_relationships`; integration test xác nhận hai TCP client gửi, accept, nhận push,
thấy presence `FREE/IN_ROOM/OFFLINE` và remove friend. Dữ liệu dùng JPA/MySQL thay vì bộ nhớ
nên không mất theo vòng đời TCP/server process. `gradlew test` pass: `common` 15, `server` 70,
`client-fx` 95 và `client` 1 test; không có failure, error hoặc skipped test.

### Phase 2 — Friends UI JavaFX ✅

- [x] Thêm state/controller/dispatcher.
- [x] Tạo Friends screen, tabs, add field và action loading.
- [x] Thêm Home tile + incoming badge.
- [x] Empty/loading/error states và CSS.
- [x] Unit test state/dispatcher; kiểm tra build/package trên khung logic 1280×720 có scale xuống cửa sổ tối thiểu 960×540.

**Exit gate:** Người dùng thêm, duyệt, hủy và xóa bạn từ UI; trạng thái online đổi realtime mà không refresh thủ công.

**Kết quả Phase 2 (2026-10-12):** JavaFX client lưu snapshot bạn bè trong `ClientState`,
áp dụng `FRIEND_LIST_UPDATE` trên UI thread và chỉ thông báo khi push chứa yêu cầu đến mới.
Màn Friends hỗ trợ add theo username, accept/decline, cancel, remove có xác nhận, presence
`OFFLINE/FREE/IN_ROOM/PLAYING`, empty/loading/error state; Home có tile Friends và badge số
yêu cầu đến. Help được chuyển thành nút icon trên header để menu không tràn khung 1280×720.
`gradlew test :client-fx:packageApp` pass: `common` 15, `server` 70, `client-fx` 99 và
`client` 1 test; không có failure, error hoặc skipped test. Windows app image được tạo thành công.

### Phase 3 — Kick trong Room Lobby

- Thêm `kickedUserIds`, `kickMember` và result record.
- Thêm handler/protocol notice và presence/lobby update.
- Thêm kick button + confirm trên `SlotCard`.
- Chặn join lại; cho phép host rời phòng/chuyển host như trước.
- Test quyền host, state phòng, concurrent leave/kick/start.

**Exit gate:** Chỉ host kick được trong `WAITING`; target về Home và không join trực tiếp lại phòng.

### Phase 4 — Room invite end-to-end

- Tạo invite manager TTL và invalidation hooks.
- Implement send/respond/accept atomic.
- Thêm invite popup trong Room Lobby.
- Thêm global incoming popup queue.
- Cho host mời lại người đã kick và chặn thành viên thường bypass kick.
- Test expiry, room full/start/closed và accept race.

**Exit gate:** Invite hoạt động với hai client thật; mọi race trả lỗi rõ ràng và không tạo membership sai.

### Phase 5 — Hardening, QA và tài liệu

- Chạy toàn bộ Gradle tests.
- Test 2–4 client: friend request, presence, invite, kick, disconnect/reconnect.
- Test DB MySQL thật và restart server.
- Kiểm tra memory cleanup invite/rate limiter/room.
- Package JavaFX app và smoke test bản `packageZip`.
- Cập nhật README, tài liệu màn hình/protocol và báo cáo nếu dùng cho đồ án.

**Exit gate:** Không còn lỗi severity cao; package Windows chạy được với server release tương ứng.

## 11. Ma trận kiểm thử bắt buộc

### 11.1 Friend service

- Add username tồn tại/không tồn tại/rỗng/chính mình.
- Trùng request cùng chiều, hai request ngược chiều.
- Hai thread gửi cùng cặp đồng thời chỉ tạo một hàng.
- Chỉ recipient được accept/decline; chỉ sender được cancel.
- Remove khi là bạn và khi không còn là bạn.
- Giới hạn friend/pending/rate limit.
- Offline friend hiển thị `OFFLINE`; login/logout/status room cập nhật đúng.
- Restart server vẫn giữ quan hệ.

### 11.2 Kick

- Host kick thành viên; non-host bị `NOT_HOST`.
- Kick self, kick host, target không ở phòng.
- Kick khi `PLAYING`/`FINISHED` bị từ chối.
- Kick đồng thời với target leave hoặc host start game.
- Room snapshot mọi client giống nhau sau mutation.
- Target không direct join lại; host invite lại thì được; member khác invite thì bị chặn.
- Kick không làm target logout và không ảnh hưởng friend relationship.

### 11.3 Invite

- Chỉ accepted friend, target online `FREE`.
- Duplicate invite; rate limit; TTL expiry.
- Decline/click hai lần/forged invite ID/invite của người khác.
- Accept khi room vừa full, start, delete hoặc inviter leave.
- Hai invite tới hai room, accept một thì invite còn lại bị hủy.
- Accept cập nhật `roomByUser` và presence đúng một lần.
- Disconnect sender/receiver cleanup đúng, không leak scheduler/map.

### 11.4 JavaFX

- Dispatcher áp dụng snapshot trước khi complete future.
- Incoming request mới tạo badge/toast đúng một lần; refresh cùng snapshot không lặp toast.
- Invite popup không ghi đè modal hiện tại và bỏ qua invite hết hạn.
- Double click action không gửi hai request.
- Kick button chỉ hiện đúng quyền/trạng thái.
- Logout/reconnect xóa state cũ.
- Layout không tràn ở 1280×720 và 960×540.

### 11.5 Regression

- Register/login/logout, room create/join/leave/ready/start/rematch.
- Host transfer khi host rời phòng.
- Gameplay và game-over persistence.
- `:common:test`, `:server:test`, `:client-fx:test`, `:client:test`.

## 12. Database rollout và tương thích

Repo hiện dùng `spring.jpa.hibernate.ddl-auto=update` và chưa có Flyway. Không đưa việc chuyển toàn bộ schema sang Flyway vào feature này vì đó là một migration nền tảng riêng, dễ làm tăng rủi ro cho bảng account/match hiện có.

Quy trình release đề xuất:

1. Backup MySQL trước deploy.
2. Sinh và review DDL `friend_relationships` tương ứng entity; lưu bản DDL trong `docs/database/` để audit/thực thi thủ công khi cần.
3. Chạy trên bản sao DB hoặc staging MySQL trước.
4. Deploy server và JavaFX client cùng release vì protocol có message type mới.
5. Kiểm tra unique/index/FK sau khi Hibernate update schema.

Không có dữ liệu bạn bè cũ nên rollback code có thể để lại bảng không được dùng; không xóa bảng khi rollback. Invite/kick state nằm trong RAM và tự mất khi restart.

## 13. Bảo mật và tính đúng đắn

- Không nhận requester/inviter/host ID từ payload; luôn lấy người thao tác từ authenticated session.
- Không tin presence hoặc room state do client gửi.
- Không trả danh sách toàn bộ tài khoản để tránh user enumeration; add friend chỉ trả lỗi chung `USER_NOT_FOUND` cho username cụ thể đã nhập.
- Không log payload nhạy cảm; log audit dạng user ID, target ID, room ID và result code.
- Validate độ dài/string trước query DB.
- Dùng unique DB constraint, transaction và room lock; kiểm tra UI không thay thế server validation.
- UUID invite có entropy đủ lớn, TTL ngắn, bound với đúng invitee.
- Mọi push chỉ gửi tới người liên quan; không broadcast friend graph.

## 14. Rủi ro và phương án giảm thiểu

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| Hai lời mời kết bạn chạy đồng thời | Trùng hàng hoặc trạng thái sai | Cặp ID chuẩn hóa, unique constraint, transaction, test concurrency |
| Accept invite đúng lúc phòng đầy/start | Membership lệch giữa client/server | Revalidate và join dưới `RoomManager.mutationLock` |
| Người bị kick join lại ngay | Kick không có tác dụng | `kickedUserIds` theo vòng đời phòng |
| Thành viên khác mời lại người bị kick | Bypass quyền host | Chỉ host invite mới gỡ kick ban |
| Popup invite ghi đè Settings/confirm | Mất thao tác hoặc modal kẹt | Queue modal, kiểm tra expiry trước khi show |
| Broadcast friend list gây tải DB | Chậm khi nhiều online user | Push theo adjacency, query ID liên quan, không global broadcast |
| Invite/rate-limit map bị leak | Tăng RAM lâu dài | TTL cleanup, disconnect/room lifecycle cleanup, metric/test |
| Binary client cũ không biết enum mới | Có thể bỏ qua hoặc lỗi decode | Release client/server đồng bộ; module legacy rebuild với `common` mới |
| `ddl-auto=update` khác MySQL/H2 | Test pass nhưng deploy lỗi | Review DDL và chạy integration trên MySQL staging |

## 15. Tiêu chí nghiệm thu cuối

1. Người dùng có thể gửi, accept, decline, cancel và remove friend; dữ liệu còn sau restart server.
2. Friend list hiển thị đúng `OFFLINE/FREE/IN_ROOM/PLAYING` gần thời gian thực.
3. Chỉ accepted friend online và rảnh mới nhận được room invite.
4. Accept invite chỉ vào phòng nếu server xác nhận còn hợp lệ; không vượt quá bốn người.
5. Chỉ host kick được trong waiting room; người bị kick về Home và không direct rejoin.
6. Host có thể chủ động mời lại người đã kick; thành viên khác không thể bypass.
7. Disconnect, room close, start game và invite expiry không để lại state tạm sai.
8. UI không optimistic-update dữ liệu domain; mọi danh sách/phòng phản ánh snapshot server.
9. Toàn bộ test bốn module pass và JavaFX package smoke test thành công.

## 16. Thứ tự bắt đầu đề xuất

Thực hiện theo thứ tự `Phase 0 → 1 → 2 → 3 → 4 → 5`. Không làm invite trước friendship vì invite phụ thuộc quyền `accepted friend`; không làm UI trước khi protocol và server contract ổn định. Kick có thể triển khai độc lập sau Friends UI và tạo nền cho quy tắc rejoin của invite.
