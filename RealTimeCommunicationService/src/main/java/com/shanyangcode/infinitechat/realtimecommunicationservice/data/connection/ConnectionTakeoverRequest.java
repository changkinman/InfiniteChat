package com.shanyangcode.infinitechat.realtimecommunicationservice.data.connection;

import javax.validation.constraints.NotBlank;

public class ConnectionTakeoverRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String connectionId;

    public ConnectionTakeoverRequest() {
    }

    public ConnectionTakeoverRequest(String userId, String connectionId) {
        this.userId = userId;
        this.connectionId = connectionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
}
