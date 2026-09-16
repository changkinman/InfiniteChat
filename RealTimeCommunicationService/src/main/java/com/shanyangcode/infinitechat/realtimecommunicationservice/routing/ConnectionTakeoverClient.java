package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import com.shanyangcode.infinitechat.realtimecommunicationservice.data.connection.ConnectionTakeoverRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.net.URI;

@Component
@Slf4j
public class ConnectionTakeoverClient {

    private static final String TAKEOVER_PATH = "/internal/connections/takeover";

    private final RestOperations restOperations;

    public ConnectionTakeoverClient(@Qualifier("connectionTakeoverRestOperations") RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public boolean requestTakeover(OnlineRoute oldRoute, String userId) {
        URI uri = URI.create(oldRoute.getEndpoint() + TAKEOVER_PATH);
        ConnectionTakeoverRequest request = new ConnectionTakeoverRequest(userId, oldRoute.getConnectionId());
        try {
            ResponseEntity<Void> response = restOperations.postForEntity(uri, request, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            log.warn("Connection takeover request failed for user {} and old route {}: {}", userId, oldRoute, ex.getMessage());
            return false;
        }
    }
}
