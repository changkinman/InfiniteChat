package com.shanyangcode.infinitechat.messageingservice.redpacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedPacketReservationRegistryTest {

    @Test
    void pendingReplyDoesNotIssueASecondInventoryDecrementRequest() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                .thenReturn(Arrays.<Object>asList(2L));
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, clock);

        ReservationResult result = registry.reserve(71L, 42L);

        assertEquals(ReservationResult.Status.PENDING, result.getStatus());
        ArgumentCaptor<List> keys = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(redis).execute(any(DefaultRedisScript.class), keys.capture(), any(), any(), any());
        assertEquals(Arrays.asList("red_packet:count:71", "red_packet:reservation:{71}:42", "red_packet:pending"), keys.getValue());
        verifyNoMoreInteractions(redis);
    }

    @Test
    void matchingReleaseReportsSuccess() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(1L);
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, Clock.systemUTC());
        RedPacketReservation reservation = new RedPacketReservation(71L, 42L, "token-1", 1_000L);

        assertTrue(registry.release(reservation));
        org.mockito.Mockito.verify(redis).execute(any(DefaultRedisScript.class),
                eq(Arrays.asList("red_packet:count:71", "red_packet:reservation:{71}:42", "red_packet:pending")),
                eq("token-1"), eq("71:42:token-1"));
    }

    @Test
    void oldTokenRejectedByLuaReportsFalse() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(0L);
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, Clock.systemUTC());
        RedPacketReservation oldReservation = new RedPacketReservation(71L, 42L, "old-token", 1_000L);

        assertFalse(registry.confirm(oldReservation));
        org.mockito.Mockito.verify(redis).execute(any(DefaultRedisScript.class),
                eq(Arrays.asList("red_packet:reservation:{71}:42", "red_packet:pending")),
                eq("old-token"), eq("71:42:old-token"));
    }

    @Test
    void expiredMemberParsingPreservesOriginalToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        TypedTuple<String> tuple = new DefaultTypedTuple<String>("71:42:original-token", 1_000D);
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScoreWithScores(eq("red_packet:pending"), any(Double.class), eq(2_000D), eq(0L), eq(10L)))
                .thenReturn(Collections.singleton(tuple));
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, Clock.systemUTC());

        List<RedPacketReservation> expired = registry.findExpiredPending(2_000L, 10);

        assertEquals(1, expired.size());
        assertEquals("original-token", expired.get(0).getToken());
        assertEquals(1_000L, expired.get(0).getCreatedAtEpochMillis());
    }

    @Test
    void missingReservationReleaseClearsExactStaleMemberAndUnblocksNextBatch() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        Set<TypedTuple<String>> pending = new LinkedHashSet<TypedTuple<String>>(Arrays.asList(
                new DefaultTypedTuple<String>("71:42:expired-token", 1_000D),
                new DefaultTypedTuple<String>("71:42:new-token", 1_500D)));
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScoreWithScores("red_packet:pending", Double.NEGATIVE_INFINITY,
                2_000D, 0L, 1L)).thenAnswer(invocation -> Collections.singleton(pending.iterator().next()));
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any())).thenAnswer(invocation -> {
            assertEquals(Arrays.asList("red_packet:count:71", "red_packet:reservation:{71}:42",
                    "red_packet:pending"), invocation.getArgument(1));
            assertEquals("expired-token", invocation.getArgument(2));
            assertEquals("71:42:expired-token", invocation.getArgument(3));
            // Model a missing hash's zero result only after checking that both stale Lua guards
            // remove this exact index member and contain no inventory or hash mutation.
            assertStaleGuardsOnlyRemoveExactMember(invocation.getArgument(0), "KEYS[2]", "KEYS[3]");
            pending.removeIf(tuple -> tuple.getValue().equals(invocation.getArgument(3)));
            return 0L;
        });
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, Clock.systemUTC());

        List<RedPacketReservation> firstBatch = registry.findExpiredPending(2_000L, 1);
        assertEquals("expired-token", firstBatch.get(0).getToken());
        assertFalse(registry.release(firstBatch.get(0)));

        List<RedPacketReservation> nextBatch = registry.findExpiredPending(2_000L, 1);
        assertEquals(1, pending.size());
        assertEquals(1, nextBatch.size());
        assertEquals("new-token", nextBatch.get(0).getToken());
    }

    @Test
    void staleConfirmationCleansExactMemberWithoutChangingHash() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any())).thenAnswer(invocation -> {
            assertEquals(Arrays.asList("red_packet:reservation:{71}:42", "red_packet:pending"),
                    invocation.getArgument(1));
            assertEquals("old-token", invocation.getArgument(2));
            assertEquals("71:42:old-token", invocation.getArgument(3));
            assertStaleGuardsOnlyRemoveExactMember(invocation.getArgument(0), "KEYS[1]", "KEYS[2]");
            return 0L;
        });
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, Clock.systemUTC());

        assertFalse(registry.confirm(new RedPacketReservation(71L, 42L, "old-token", 1_000L)));
    }

    @Test
    void malformedPendingMembersAreRemovedWhileValidMembersAreReturned() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        List<String> malformed = Arrays.asList(null, "not-a-reservation", "null:42:token",
                "71:null:token", "71:42:", "71::token", ":42:token");
        Set<TypedTuple<String>> pending = new LinkedHashSet<TypedTuple<String>>();
        for (String member : malformed) {
            pending.add(new DefaultTypedTuple<String>(member, 1_000D));
        }
        pending.add(new DefaultTypedTuple<String>("71:42:no-score", null));
        pending.add(new DefaultTypedTuple<String>("71:42:valid-token", 1_500D));
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScoreWithScores("red_packet:pending", Double.NEGATIVE_INFINITY,
                2_000D, 0L, 10L)).thenReturn(pending);
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, Clock.systemUTC());

        List<RedPacketReservation> expired = registry.findExpiredPending(2_000L, 10);

        assertEquals(1, expired.size());
        assertEquals("valid-token", expired.get(0).getToken());
        for (String member : malformed) {
            verify(zSetOperations).remove("red_packet:pending", member);
        }
        verify(zSetOperations).remove("red_packet:pending", "71:42:no-score");
        verify(zSetOperations).rangeByScoreWithScores("red_packet:pending", Double.NEGATIVE_INFINITY,
                2_000D, 0L, 10L);
        verifyNoMoreInteractions(zSetOperations);
    }

    private static void assertStaleGuardsOnlyRemoveExactMember(DefaultRedisScript<?> script,
                                                             String hashKey, String pendingKey) {
        String lua = script.getScriptAsString();
        List<String> guards = Arrays.asList(
                "if redis.call('HGET', " + hashKey + ", 'state') ~= '1' then",
                "if redis.call('HGET', " + hashKey + ", 'token') ~= ARGV[1] then");
        for (String guard : guards) {
            int start = lua.indexOf(guard);
            assertTrue(start >= 0, "The state and token guards must precede mutations");
            int bodyStart = start + guard.length();
            int end = lua.indexOf("end", bodyStart);
            assertEquals("redis.call('ZREM', " + pendingKey + ", ARGV[2]) return 0",
                    lua.substring(bodyStart, end).trim().replaceAll("\\s+", " "),
                    "A stale guard must remove only the exact pending member before returning zero");
        }
    }
}
