package com.shanyangcode.infinitechat.messageingservice.redpacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shanyangcode.infinitechat.messageingservice.mapper.RedPacketReceiveMapper;
import com.shanyangcode.infinitechat.messageingservice.model.RedPacketReceive;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RedPacketReservationRecoveryJobTest {

    private static final long RED_PACKET_ID = 71L;
    private static final long USER_ID = 42L;

    @Test
    void persistedReceiveConfirmsReservationWithoutReleasingIt() {
        Fixture fixture = new Fixture();
        RedPacketReservation reservation = reservation(RED_PACKET_ID, USER_ID);
        when(fixture.registry.findExpiredPending(anyLong(), eq(100)))
                .thenReturn(Collections.singletonList(reservation));
        when(fixture.receiveMapper.selectByPacketIdAndReceiverId(RED_PACKET_ID, USER_ID))
                .thenReturn(new RedPacketReceive());

        fixture.job.recoverExpiredReservations();

        verify(fixture.registry).confirm(reservation);
        verify(fixture.registry, never()).release(reservation);
    }

    @Test
    void absentReceiveReleasesReservationWithoutConfirmingIt() {
        Fixture fixture = new Fixture();
        RedPacketReservation reservation = reservation(RED_PACKET_ID, USER_ID);
        when(fixture.registry.findExpiredPending(anyLong(), eq(100)))
                .thenReturn(Collections.singletonList(reservation));
        when(fixture.receiveMapper.selectByPacketIdAndReceiverId(RED_PACKET_ID, USER_ID)).thenReturn(null);

        fixture.job.recoverExpiredReservations();

        verify(fixture.registry).release(reservation);
        verify(fixture.registry, never()).confirm(reservation);
    }

    @Test
    void failureForOneReservationDoesNotPreventFollowingReservationRecovery() {
        Fixture fixture = new Fixture();
        RedPacketReservation failedReservation = reservation(71L, 42L);
        RedPacketReservation followingReservation = reservation(72L, 43L);
        when(fixture.registry.findExpiredPending(anyLong(), eq(100)))
                .thenReturn(Arrays.asList(failedReservation, followingReservation));
        when(fixture.receiveMapper.selectByPacketIdAndReceiverId(71L, 42L))
                .thenThrow(new RuntimeException("database unavailable"));
        when(fixture.receiveMapper.selectByPacketIdAndReceiverId(72L, 43L)).thenReturn(null);

        fixture.job.recoverExpiredReservations();

        verify(fixture.registry).release(followingReservation);
        verify(fixture.registry, never()).confirm(failedReservation);
        verify(fixture.registry, never()).release(failedReservation);
    }

    @Test
    void scansOneHundredReservationsCreatedMoreThanSixtySecondsAgo() {
        Fixture fixture = new Fixture();
        when(fixture.registry.findExpiredPending(anyLong(), eq(100))).thenReturn(Collections.emptyList());

        fixture.job.recoverExpiredReservations();

        ArgumentCaptor<Long> cutoff = ArgumentCaptor.forClass(Long.class);
        verify(fixture.registry).findExpiredPending(cutoff.capture(), eq(100));
        assertEquals(Long.valueOf(60_000L), cutoff.getValue());
    }

    private static RedPacketReservation reservation(long redPacketId, long userId) {
        return new RedPacketReservation(redPacketId, userId, "token-" + redPacketId + "-" + userId, 1_000L);
    }

    private static final class Fixture {
        private final RedPacketReservationRegistry registry = mock(RedPacketReservationRegistry.class);
        private final RedPacketReceiveMapper receiveMapper = mock(RedPacketReceiveMapper.class);
        private final Clock clock = Clock.fixed(Instant.ofEpochMilli(120_000L), ZoneOffset.UTC);
        private final RedPacketReservationRecoveryJob job = new RedPacketReservationRecoveryJob(registry, receiveMapper, clock);
    }
}
