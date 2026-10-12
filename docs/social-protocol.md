# Social and room-moderation protocol

> Contract version: Phase 0, 2026-10-12  
> Implementation status: friends, host kick and room invitations are available end-to-end through Phase 4.

This document is the implementation contract for friends, room invitations and host kick.
The server remains authoritative. A client sends an intent and only changes domain state after
receiving a server snapshot or status message.

## Shared transport rules

- Messages use the existing length-prefixed `NetworkMessage` format.
- A direct response copies the request's `requestId`.
- An unsolicited push has `requestId = null`.
- A rejected command returns `ERROR` with `ErrorResponse(code, message)`.
- Timestamps crossing the protocol are UTC epoch milliseconds. The protocol intentionally does
  not use `Instant`, so every existing plain Jackson `ObjectMapper` can encode it without an
  additional Java Time module.
- IDs identify resources only. The acting user is always taken from the authenticated
  `ClientSession`, never from a payload.

## Friend messages

| Client message | Payload | Successful response |
|---|---|---|
| `FRIEND_LIST_REQUEST` | none | `FRIEND_LIST_UPDATE` |
| `FRIEND_REQUEST_SEND` | `SendFriendRequest` | `FRIEND_LIST_UPDATE` |
| `FRIEND_REQUEST_RESPOND` | `RespondFriendRequest` | `FRIEND_LIST_UPDATE` |
| `FRIEND_REQUEST_CANCEL` | `CancelFriendRequest` | `FRIEND_LIST_UPDATE` |
| `FRIEND_REMOVE` | `RemoveFriendRequest` | `FRIEND_LIST_UPDATE` |

`FRIEND_LIST_UPDATE` always contains a complete snapshot for its recipient: accepted friends,
incoming requests and outgoing requests. After a mutation, the actor receives a correlated
snapshot and the other account receives an unsolicited snapshot when online.

Presence is one of `OFFLINE`, `FREE`, `IN_ROOM`, `PLAYING`. A friendship row is either
`PENDING` or `ACCEPTED`; decline, cancel and remove delete the row. A reverse request against an
existing pending request accepts it atomically.

## Room invitation messages

| Client message | Payload | Successful response/push |
|---|---|---|
| `ROOM_INVITE_SEND` | `SendRoomInviteRequest` | correlated `ROOM_INVITE_STATUS(SENT)`; invitee gets `ROOM_INVITE_RECEIVED` |
| `ROOM_INVITE_RESPOND` with `accept=false` | `RespondRoomInviteRequest` | correlated `ROOM_INVITE_STATUS(DECLINED)`; inviter gets the same status as a push |
| `ROOM_INVITE_RESPOND` with `accept=true` | `RespondRoomInviteRequest` | correlated `ROOM_STATE`; room members get the authoritative room snapshot |

An invite is bound to its invitee, room and inviter, expires after 60 seconds by default and is
single-use. Accept revalidates friendship, presence, room state and capacity. It performs the
join on the server rather than asking the client to send a second `JOIN_ROOM`.

Terminal invite status values are `ACCEPTED`, `DECLINED`, `EXPIRED` and `CANCELLED`. `SENT`
acknowledges successful creation.

## Host kick messages

The host sends `KICK_ROOM_MEMBER` with `KickRoomMemberRequest(targetUserId)`. It is valid only
while the room is `WAITING`, for another member of the same room.

On success:

1. The host gets a correlated `ROOM_STATE` and remaining members get the same snapshot as pushes.
2. The target gets `ROOM_MEMBER_REMOVED` with reason `HOST_KICK`.
3. The target then gets `ROOM_STATE(member=false, room=null)` and returns to Home.

The room keeps an internal kick ban for its lifetime. A normal `JOIN_ROOM` by that account fails
with `KICKED_FROM_ROOM`. Only a new invitation sent by the current host can clear the ban when it
is accepted.

## Contract test inventory

Phase 0 provides executable codec tests for:

- complete friend snapshots and their defensive list copies;
- every friend command payload;
- invite send, receive, respond and status payloads;
- kick command and removal notice payloads;
- a payload-free `FRIEND_LIST_REQUEST`;
- UTF-8 framing and the existing malformed/partial-frame cases.

The following server behavior tests are the implementation skeleton for later phases:

| Phase | Test class target | Required cases |
|---|---|---|
| 1 ✅ | `FriendServiceIntegrationTest` | self request, unknown user, duplicate, reverse auto-accept, permissions, lifecycle, concurrent pair creation |
| 1 ✅ | `FriendMessageHandlerIntegrationTest` | correlation IDs, two-party pushes, `FREE/IN_ROOM/OFFLINE` presence and full add/accept/remove lifecycle |
| 1 ✅ | `SocialRateLimiterTest` | per-user isolation, request ceiling and window expiry |
| 2 ✅ | `ClientStateSocialTest` | defensive snapshots, listener notification and logout cleanup |
| 2 ✅ | `ClientMessageDispatcherTest` | apply snapshot before completing request; notify only for a new unsolicited incoming request |
| 3 ✅ | `RoomManagerModerationTest` | host/non-host, self/unknown target, room status, kick ban and leave/start races |
| 3 ✅ | `RoomModerationIntegrationTest` | correlated host snapshot, target notice/home state, `FREE` presence and blocked direct rejoin |
| 4 ✅ | `RoomInviteManagerTest` | duplicate, expiry, one-time consume, cleanup by room/user |
| 4 ✅ | `RoomInviteIntegrationTest` | real two-client accept/join, decline and replay rejection |

These test classes should be created with the production service in their phase rather than kept
as disabled tests, so the suite never reports intentionally skipped social tests.
