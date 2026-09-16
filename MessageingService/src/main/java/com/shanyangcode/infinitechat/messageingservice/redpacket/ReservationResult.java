package com.shanyangcode.infinitechat.messageingservice.redpacket;

import java.util.Objects;

/** The outcome of an attempt to reserve red-packet inventory. */
public final class ReservationResult {

    public enum Status {
        RESERVED,
        PENDING,
        EMPTY,
        ALREADY_CLAIMED
    }

    private final Status status;
    private final RedPacketReservation reservation;

    private ReservationResult(Status status, RedPacketReservation reservation) {
        this.status = Objects.requireNonNull(status, "status");
        this.reservation = reservation;
    }

    public static ReservationResult reserved(RedPacketReservation reservation) {
        return new ReservationResult(Status.RESERVED, Objects.requireNonNull(reservation, "reservation"));
    }

    public static ReservationResult of(Status status) {
        if (status == Status.RESERVED) {
            throw new IllegalArgumentException("RESERVED requires a reservation");
        }
        return new ReservationResult(status, null);
    }

    public Status getStatus() {
        return status;
    }

    public RedPacketReservation getReservation() {
        return reservation;
    }
}
