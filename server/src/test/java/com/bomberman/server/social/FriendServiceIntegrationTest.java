package com.bomberman.server.social;

import com.bomberman.common.enums.FriendRelationshipStatus;
import com.bomberman.common.enums.FriendResultCode;
import com.bomberman.server.repository.UserRepository;
import com.bomberman.server.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "bomberman.tcp.port=0")
@Timeout(20)
class FriendServiceIntegrationTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private FriendRelationshipRepository relationshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void sendsAndAcceptsFriendRequest() {
        User alice = user("alice");
        User bob = user("bob");

        FriendCommandResult sent = friendService.sendRequest(alice.getId(), bob.getUsername());
        FriendSnapshot bobPending = friendService.snapshot(bob.getId());
        long relationshipId = bobPending.incomingRequests().getFirst().relationshipId();
        FriendCommandResult accepted = friendService.respond(bob.getId(), relationshipId, true);

        assertTrue(sent.isSuccess());
        assertTrue(accepted.isSuccess());
        assertTrue(friendService.areFriends(alice.getId(), bob.getId()));
        assertEquals(1, friendService.snapshot(alice.getId()).friends().size());
        assertTrue(friendService.snapshot(bob.getId()).incomingRequests().isEmpty());
    }

    @Test
    void reversePendingRequestAutomaticallyAcceptsRelationship() {
        User alice = user("alice");
        User bob = user("bob");

        assertTrue(friendService.sendRequest(alice.getId(), bob.getUsername()).isSuccess());
        FriendCommandResult reverse = friendService.sendRequest(bob.getId(), alice.getUsername());

        assertTrue(reverse.isSuccess());
        assertTrue(friendService.areFriends(alice.getId(), bob.getId()));
        assertEquals(
                FriendRelationshipStatus.ACCEPTED,
                relationshipRepository.findAllForUser(alice.getId()).getFirst().getStatus()
        );
    }

    @Test
    void validatesSelfUnknownAndDuplicateRequests() {
        User alice = user("alice");
        User bob = user("bob");

        assertEquals(
                FriendResultCode.CANNOT_ADD_SELF,
                friendService.sendRequest(alice.getId(), alice.getUsername()).error()
        );
        assertEquals(
                FriendResultCode.USER_NOT_FOUND,
                friendService.sendRequest(alice.getId(), unique("missing")).error()
        );
        assertTrue(friendService.sendRequest(alice.getId(), bob.getUsername()).isSuccess());
        assertEquals(
                FriendResultCode.REQUEST_ALREADY_PENDING,
                friendService.sendRequest(alice.getId(), bob.getUsername()).error()
        );
    }

    @Test
    void onlyRecipientCanRespondAndOnlySenderCanCancel() {
        User alice = user("alice");
        User bob = user("bob");
        User mallory = user("mallory");
        friendService.sendRequest(alice.getId(), bob.getUsername());
        long relationshipId = friendService.snapshot(bob.getId())
                .incomingRequests().getFirst().relationshipId();

        assertEquals(
                FriendResultCode.NOT_REQUEST_RECIPIENT,
                friendService.respond(alice.getId(), relationshipId, true).error()
        );
        assertEquals(
                FriendResultCode.NOT_REQUEST_RECIPIENT,
                friendService.respond(mallory.getId(), relationshipId, true).error()
        );
        assertEquals(
                FriendResultCode.NOT_REQUEST_SENDER,
                friendService.cancel(bob.getId(), relationshipId).error()
        );
        assertTrue(friendService.cancel(alice.getId(), relationshipId).isSuccess());
        assertTrue(friendService.snapshot(alice.getId()).outgoingRequests().isEmpty());
    }

    @Test
    void declineAndRemoveDeleteRelationship() {
        User alice = user("alice");
        User bob = user("bob");
        friendService.sendRequest(alice.getId(), bob.getUsername());
        long firstRelationship = friendService.snapshot(bob.getId())
                .incomingRequests().getFirst().relationshipId();

        assertTrue(friendService.respond(bob.getId(), firstRelationship, false).isSuccess());
        assertTrue(relationshipRepository.findAllForUser(alice.getId()).isEmpty());

        friendService.sendRequest(alice.getId(), bob.getUsername());
        long secondRelationship = friendService.snapshot(bob.getId())
                .incomingRequests().getFirst().relationshipId();
        friendService.respond(bob.getId(), secondRelationship, true);

        assertTrue(friendService.remove(alice.getId(), bob.getId()).isSuccess());
        assertFalse(friendService.areFriends(alice.getId(), bob.getId()));
        assertEquals(FriendResultCode.NOT_FRIENDS,
                friendService.remove(alice.getId(), bob.getId()).error());
    }

    @Test
    void concurrentDuplicateRequestsCreateOneRelationship() throws Exception {
        User alice = user("alice");
        User bob = user("bob");
        int attempts = 5;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<FriendCommandResult> results = java.util.Collections.synchronizedList(new ArrayList<>());

        try (var executor = Executors.newFixedThreadPool(attempts)) {
            for (int index = 0; index < attempts; index++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    results.add(friendService.sendRequest(alice.getId(), bob.getUsername()));
                    return null;
                });
            }
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(1, results.stream().filter(FriendCommandResult::isSuccess).count());
        assertEquals(1, relationshipRepository.findAllForUser(alice.getId()).size());
    }

    @Test
    void databaseConstraintRejectsDuplicateNormalizedPair() {
        User alice = user("alice");
        User bob = user("bob");
        User low = alice.getId() < bob.getId() ? alice : bob;
        User high = low == alice ? bob : alice;
        relationshipRepository.saveAndFlush(new FriendRelationship(low, high, alice));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> relationshipRepository.saveAndFlush(
                        new FriendRelationship(low, high, bob)
                )
        );
    }

    private User user(String prefix) {
        return userRepository.saveAndFlush(new User(unique(prefix), "unused-test-hash"));
    }

    private String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
