package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import java.util.Objects;

public final class LocalNode {

    private final String nodeId;
    private final String endpoint;

    public LocalNode(String nodeId, String endpoint) {
        this.nodeId = requireText(nodeId, "nodeId");
        this.endpoint = requireText(endpoint, "endpoint");
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getEndpoint() {
        return endpoint;
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
        if (!(o instanceof LocalNode)) {
            return false;
        }
        LocalNode localNode = (LocalNode) o;
        return Objects.equals(nodeId, localNode.nodeId)
                && Objects.equals(endpoint, localNode.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, endpoint);
    }

    @Override
    public String toString() {
        return "LocalNode{"
                + "nodeId='" + nodeId + '\''
                + ", endpoint='" + endpoint + '\''
                + '}';
    }
}
