package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import java.util.Objects;

public final class OnlineRoute {

    private final String nodeId;
    private final String endpoint;
    private final String connectionId;

    public OnlineRoute(String nodeId, String endpoint, String connectionId) {
        this.nodeId = requireText(nodeId, "nodeId");
        this.endpoint = requireText(endpoint, "endpoint");
        this.connectionId = requireText(connectionId, "connectionId");
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getConnectionId() {
        return connectionId;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OnlineRoute)) {
            return false;
        }
        OnlineRoute that = (OnlineRoute) o;
        return Objects.equals(nodeId, that.nodeId)
                && Objects.equals(endpoint, that.endpoint)
                && Objects.equals(connectionId, that.connectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, endpoint, connectionId);
    }

    @Override
    public String toString() {
        return "OnlineRoute{"
                + "nodeId='" + nodeId + '\''
                + ", endpoint='" + endpoint + '\''
                + ", connectionId='" + connectionId + '\''
                + '}';
    }
}
