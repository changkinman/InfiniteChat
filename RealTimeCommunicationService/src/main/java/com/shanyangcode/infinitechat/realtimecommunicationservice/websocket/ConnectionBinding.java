package com.shanyangcode.infinitechat.realtimecommunicationservice.websocket;

import io.netty.channel.Channel;

import java.util.Objects;

public final class ConnectionBinding {
    private final String userId;
    private final String connectionId;
    private final Channel channel;

    public ConnectionBinding(String userId, String connectionId, Channel channel) {
        this.userId = userId;
        this.connectionId = connectionId;
        this.channel = channel;
    }

    public String getUserId() {
        return userId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectionBinding)) {
            return false;
        }
        ConnectionBinding that = (ConnectionBinding) o;
        return Objects.equals(userId, that.userId)
                && Objects.equals(connectionId, that.connectionId)
                && Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, connectionId, channel);
    }
}
