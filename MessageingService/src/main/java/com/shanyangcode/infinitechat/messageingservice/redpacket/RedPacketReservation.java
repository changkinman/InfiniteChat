package com.shanyangcode.infinitechat.messageingservice.redpacket;

import java.util.Objects;

/** A single, token-guarded reservation of one red-packet inventory unit. */
public final class RedPacketReservation {

    private final long redPacketId;
    private final long userId;
    private final String token;
    private final long createdAtEpochMillis;

    public RedPacketReservation(long redPacketId, long userId, String token, long createdAtEpochMillis) {
        this.redPacketId = redPacketId;
        this.userId = userId;
        this.token = Objects.requireNonNull(token, "token");
        this.createdAtEpochMillis = createdAtEpochMillis;
    }

    public long getRedPacketId() {
        return redPacketId;
    }

    public long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public long getCreatedAtEpochMillis() {
        return createdAtEpochMillis;
    }
}
