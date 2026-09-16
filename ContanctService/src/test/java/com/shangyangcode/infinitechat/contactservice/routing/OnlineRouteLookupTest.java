package com.shangyangcode.infinitechat.contactservice.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OnlineRouteLookupTest {

    private HashOperations<String, Object, Object> hashOperations;
    private OnlineRouteLookup routeLookup;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        routeLookup = new OnlineRouteLookup(redisTemplate);
    }

    @Test
    public void findEndpointReturnsHashEndpointForUser() {
        when(hashOperations.get("user:session:42", "endpoint")).thenReturn("http://10.0.0.2:8083");

        Optional<String> endpoint = routeLookup.findEndpoint(42L);

        assertEquals(Optional.of("http://10.0.0.2:8083"), endpoint);
    }

    @Test
    public void findEndpointTreatsBlankEndpointAsOffline() {
        when(hashOperations.get("user:session:42", "endpoint")).thenReturn("   ");

        Optional<String> endpoint = routeLookup.findEndpoint(42L);

        assertFalse(endpoint.isPresent());
    }

    @Test
    public void resolveUrlUsesExactlyOneSlashBetweenEndpointAndPath() {
        when(hashOperations.get("user:session:42", "endpoint")).thenReturn("http://10.0.0.2:8083/");

        Optional<String> url = routeLookup.resolveUrl(42L, "/api/v1/message/user/");

        assertEquals(Optional.of("http://10.0.0.2:8083/api/v1/message/user/"), url);
    }
}
