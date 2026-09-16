package com.shanyangcode.infinitechat.messageingservice.redpacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        assertEquals(Arrays.asList("red_packet:count:{71}", "red_packet:reservation:{71}:42", "red_packet:pending"), keys.getValue());
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
    }

    @Test
    void oldTokenRejectedByLuaReportsFalse() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .thenReturn(0L);
        RedPacketReservationRegistry registry = new RedPacketReservationRegistry(redis, Clock.systemUTC());
        RedPacketReservation oldReservation = new RedPacketReservation(71L, 42L, "old-token", 1_000L);

        assertFalse(registry.confirm(oldReservation));
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
}
