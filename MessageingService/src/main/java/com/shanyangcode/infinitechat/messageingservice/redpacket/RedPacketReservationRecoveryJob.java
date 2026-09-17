package com.shanyangcode.infinitechat.messageingservice.redpacket;

import com.shanyangcode.infinitechat.messageingservice.mapper.RedPacketReceiveMapper;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reconciles pending Redis reservations with persisted receive records. */
@Component
public class RedPacketReservationRecoveryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedPacketReservationRecoveryJob.class);
    private static final long RESERVATION_TIMEOUT_MILLIS = 60_000L;
    private static final int BATCH_SIZE = 100;

    private final RedPacketReservationRegistry registry;
    private final RedPacketReceiveMapper receiveMapper;
    private final Clock clock;

    public RedPacketReservationRecoveryJob(RedPacketReservationRegistry registry,
                                           RedPacketReceiveMapper receiveMapper,
                                           Clock clock) {
        this.registry = registry;
        this.receiveMapper = receiveMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 10_000L)
    public void recoverExpiredReservations() {
        long cutoff = clock.millis() - RESERVATION_TIMEOUT_MILLIS;
        for (RedPacketReservation reservation : registry.findExpiredPending(cutoff, BATCH_SIZE)) {
            recoverReservation(reservation);
        }
    }

    private void recoverReservation(RedPacketReservation reservation) {
        try {
            if (receiveMapper.selectByPacketIdAndReceiverId(reservation.getRedPacketId(), reservation.getUserId()) != null) {
                registry.confirm(reservation);
            } else {
                registry.release(reservation);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to recover red-packet reservation: packetId={}, userId={}",
                    reservation.getRedPacketId(), reservation.getUserId(), exception);
        }
    }
}
