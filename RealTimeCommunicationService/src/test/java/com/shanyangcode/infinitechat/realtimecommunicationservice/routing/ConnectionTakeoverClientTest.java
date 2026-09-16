package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import com.shanyangcode.infinitechat.realtimecommunicationservice.data.connection.ConnectionTakeoverRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionTakeoverClientTest {

    @Mock
    private RestOperations restOperations;

    @Test
    void requestTakeoverPostsOldConnectionToInternalEndpointAndReturnsTrueForOk() {
        ConnectionTakeoverClient client = new ConnectionTakeoverClient(restOperations);
        OnlineRoute oldRoute = new OnlineRoute("10.0.0.1:9000", "http://10.0.0.1:8083", "old-id");
        URI expectedUri = URI.create("http://10.0.0.1:8083/internal/connections/takeover");
        when(restOperations.postForEntity(eq(expectedUri), org.mockito.ArgumentMatchers.any(), eq(Void.class)))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));

        boolean accepted = client.requestTakeover(oldRoute, "42");

        ArgumentCaptor<ConnectionTakeoverRequest> request = ArgumentCaptor.forClass(ConnectionTakeoverRequest.class);
        verify(restOperations).postForEntity(eq(expectedUri), request.capture(), eq(Void.class));
        assertThat(accepted).isTrue();
        assertThat(request.getValue().getUserId()).isEqualTo("42");
        assertThat(request.getValue().getConnectionId()).isEqualTo("old-id");
    }

    @Test
    void requestTakeoverReturnsFalseForTransportFailure() {
        ConnectionTakeoverClient client = new ConnectionTakeoverClient(restOperations);
        OnlineRoute oldRoute = new OnlineRoute("10.0.0.1:9000", "http://10.0.0.1:8083", "old-id");
        URI expectedUri = URI.create("http://10.0.0.1:8083/internal/connections/takeover");
        when(restOperations.postForEntity(eq(expectedUri), org.mockito.ArgumentMatchers.any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        boolean accepted = client.requestTakeover(oldRoute, "42");

        assertThat(accepted).isFalse();
    }
}
