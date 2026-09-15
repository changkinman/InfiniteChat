package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineRouteRegistryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private OnlineRouteRegistry registry;

    @BeforeEach
    void setUp() {
        OnlineRouteProperties properties = new OnlineRouteProperties();
        properties.validate();
        registry = new OnlineRouteRegistry(redisTemplate, properties);
    }

    @Test
    void claimReturnsPriorRouteFromRedisHash() {
        OnlineRoute route = new OnlineRoute("10.0.0.2:9000", "http://10.0.0.2:8083", "new-connection");
        when(redisTemplate.execute(
                eq(OnlineRouteRegistry.CLAIM_SCRIPT),
                eq(Collections.singletonList("user:session:42")),
                eq("10.0.0.2:9000"),
                eq("http://10.0.0.2:8083"),
                eq("new-connection"),
                eq("600000")))
                .thenReturn(Arrays.asList("10.0.0.1:9000", "http://10.0.0.1:8083", "old-connection"));

        Optional<OnlineRoute> claimed = registry.claim("42", route);

        assertThat(claimed).contains(new OnlineRoute("10.0.0.1:9000", "http://10.0.0.1:8083", "old-connection"));
    }

    @Test
    void claimWritesNewRouteWithDefaultTtlMilliseconds() {
        OnlineRoute route = new OnlineRoute("10.0.0.2:9000", "http://10.0.0.2:8083", "new-connection");
        when(redisTemplate.execute(
                eq(OnlineRouteRegistry.CLAIM_SCRIPT),
                eq(Collections.singletonList("user:session:42")),
                eq("10.0.0.2:9000"),
                eq("http://10.0.0.2:8083"),
                eq("new-connection"),
                eq("600000")))
                .thenReturn(Arrays.asList("", "", ""));

        registry.claim("42", route);

        verify(redisTemplate).execute(
                eq(OnlineRouteRegistry.CLAIM_SCRIPT),
                eq(Collections.singletonList("user:session:42")),
                eq("10.0.0.2:9000"),
                eq("http://10.0.0.2:8083"),
                eq("new-connection"),
                eq("600000"));
    }

    @Test
    void claimReturnsEmptyWhenRedisHadNoRoute() {
        OnlineRoute route = new OnlineRoute("10.0.0.2:9000", "http://10.0.0.2:8083", "new-connection");
        when(redisTemplate.execute(
                eq(OnlineRouteRegistry.CLAIM_SCRIPT),
                eq(Collections.singletonList("user:session:42")),
                eq("10.0.0.2:9000"),
                eq("http://10.0.0.2:8083"),
                eq("new-connection"),
                eq("600000")))
                .thenReturn(Arrays.asList("", "", ""));

        Optional<OnlineRoute> claimed = registry.claim("42", route);

        assertThat(claimed).isEmpty();
    }

    @Test
    void staleConnectionCannotRenewOrRelease() {
        when(redisTemplate.execute(
                eq(OnlineRouteRegistry.RENEW_SCRIPT),
                eq(Collections.singletonList("user:session:42")),
                eq("old"),
                eq("600000")))
                .thenReturn(0L);
        when(redisTemplate.execute(
                eq(OnlineRouteRegistry.RELEASE_SCRIPT),
                eq(Collections.singletonList("user:session:42")),
                eq("old")))
                .thenReturn(0L);

        assertThat(registry.renew("42", "old")).isFalse();
        assertThat(registry.release("42", "old")).isFalse();
    }

    @Test
    void isOwnerReadsOnlyConnectionIdAndDistinguishesCurrentFromOld() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("user:session:42", "connectionId")).thenReturn("current");

        assertThat(registry.isOwner("42", "current")).isTrue();
        assertThat(registry.isOwner("42", "old")).isFalse();

        verify(hashOperations, times(2)).get("user:session:42", "connectionId");
        verify(hashOperations, never()).get("user:session:42", "endpoint");
        verify(hashOperations, never()).get("user:session:42", "nodeId");
    }
}
