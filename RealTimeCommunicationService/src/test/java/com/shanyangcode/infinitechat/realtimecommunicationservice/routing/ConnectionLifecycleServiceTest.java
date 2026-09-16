package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ChannelManager;
import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ConnectionBinding;
import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionLifecycleServiceTest {

    @Mock
    private ConnectionTakeoverClient takeoverClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private Channel newChannel;

    @Mock
    private Channel oldChannel;

    @Mock
    private Channel replacementChannel;

    private ChannelManager channels;
    private OnlineRouteRegistry routes;
    private ConnectionLifecycleService lifecycle;

    @BeforeEach
    void setUp() {
        routes = new OnlineRouteRegistry(redisTemplate, new OnlineRouteProperties());
        channels = new ChannelManager();
        lifecycle = new ConnectionLifecycleService(
                routes,
                channels,
                takeoverClient,
                new LocalNode("local:9000", "http://local:8083"));
    }

    @Test
    void establishClosesNewChannelAndLeavesNoMappingWhenOwnershipRecheckFails() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.eq(OnlineRouteRegistry.CLAIM_SCRIPT),
                org.mockito.ArgumentMatchers.eq(Collections.singletonList("user:session:42")),
                anyString(),
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(Collections.emptyList());
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("user:session:42", "connectionId")).thenReturn("stolen-id");

        boolean established = lifecycle.establish("42", newChannel);

        assertThat(established).isFalse();
        assertThat(channels.findByUserId("42")).isEmpty();
        assertThat(channels.findByChannel(newChannel)).isEmpty();
        verify(newChannel).close();
    }

    @Test
    void establishRequestsTakeoverForDifferentNodePriorRouteAfterOwnershipSucceeds() {
        OnlineRoute oldRoute = new OnlineRoute("other:9000", "http://other:8083", "old-id");
        AtomicReference<String> claimedConnectionId = new AtomicReference<String>();
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.eq(OnlineRouteRegistry.CLAIM_SCRIPT),
                org.mockito.ArgumentMatchers.eq(Collections.singletonList("user:session:42")),
                anyString(),
                anyString(),
                anyString(),
                anyString()))
                .thenAnswer(invocation -> {
                    claimedConnectionId.set((String) invocation.getArgument(4));
                    return Arrays.asList("other:9000", "http://other:8083", "old-id");
                });
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("user:session:42", "connectionId"))
                .thenAnswer(invocation -> claimedConnectionId.get());

        boolean established = lifecycle.establish("42", newChannel);

        assertThat(established).isTrue();
        verify(takeoverClient).requestTakeover(oldRoute, "42");
    }

    @Test
    void heartbeatClosesConnectionAndReturnsFalseWhenRenewalFails() {
        channels.register(new ConnectionBinding("42", "current-id", newChannel));
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.eq(OnlineRouteRegistry.RENEW_SCRIPT),
                org.mockito.ArgumentMatchers.eq(Collections.singletonList("user:session:42")),
                org.mockito.ArgumentMatchers.eq("current-id"),
                anyString()))
                .thenReturn(0L);

        boolean renewed = lifecycle.heartbeat(newChannel);

        assertThat(renewed).isFalse();
        assertThat(channels.findByUserId("42")).isEmpty();
        assertThat(channels.findByChannel(newChannel)).isEmpty();
        verify(newChannel).close();
    }

    @Test
    void disconnectingStaleOldChannelReleasesOnlyOldConnectionAndKeepsReplacementMapping() {
        channels.register(new ConnectionBinding("42", "old-id", oldChannel));
        ConnectionBinding replacement = new ConnectionBinding("42", "new-id", replacementChannel);
        channels.register(replacement);

        lifecycle.disconnect(oldChannel);

        ArgumentCaptor<String> releasedConnectionId = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).execute(
                org.mockito.ArgumentMatchers.eq(OnlineRouteRegistry.RELEASE_SCRIPT),
                org.mockito.ArgumentMatchers.eq(Collections.singletonList("user:session:42")),
                releasedConnectionId.capture());
        assertThat(releasedConnectionId.getValue()).isEqualTo("old-id");
        assertThat(channels.findByUserId("42")).contains(replacement);
        assertThat(channels.findByChannel(replacementChannel)).contains(replacement);
        verify(oldChannel).close();
        verify(replacementChannel, never()).close();
    }
}
