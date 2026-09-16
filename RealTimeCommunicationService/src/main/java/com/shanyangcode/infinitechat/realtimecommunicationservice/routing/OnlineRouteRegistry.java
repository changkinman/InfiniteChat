package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public final class OnlineRouteRegistry {

    static final String KEY_PREFIX = "user:session:";
    static final DefaultRedisScript<List> CLAIM_SCRIPT = script("scripts/online-route/claim.lua", List.class);
    static final DefaultRedisScript<Long> RENEW_SCRIPT = script("scripts/online-route/renew.lua", Long.class);
    static final DefaultRedisScript<Long> RELEASE_SCRIPT = script("scripts/online-route/release.lua", Long.class);

    private final StringRedisTemplate redisTemplate;
    private final OnlineRouteProperties properties;

    public OnlineRouteRegistry(StringRedisTemplate redisTemplate, OnlineRouteProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public Optional<OnlineRoute> claim(String userId, OnlineRoute route) {
        String key = key(userId);
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }

        List previous = redisTemplate.execute(
                CLAIM_SCRIPT,
                Collections.singletonList(key),
                route.getNodeId(),
                route.getEndpoint(),
                route.getConnectionId(),
                String.valueOf(properties.getTtl().toMillis()));

        if (previous == null || previous.size() < 3) {
            return Optional.empty();
        }

        String oldNodeId = asString(previous.get(0));
        String oldEndpoint = asString(previous.get(1));
        String oldConnectionId = asString(previous.get(2));
        if (isBlank(oldNodeId) && isBlank(oldEndpoint) && isBlank(oldConnectionId)) {
            return Optional.empty();
        }
        return Optional.of(new OnlineRoute(oldNodeId, oldEndpoint, oldConnectionId));
    }

    public boolean isOwner(String userId, String connectionId) {
        String key = key(userId);
        requireText(connectionId, "connectionId");

        Object currentConnectionId = redisTemplate.opsForHash().get(key, "connectionId");
        return connectionId.equals(currentConnectionId);
    }

    public boolean renew(String userId, String connectionId) {
        String key = key(userId);
        requireText(connectionId, "connectionId");

        Long result = redisTemplate.execute(
                RENEW_SCRIPT,
                Collections.singletonList(key),
                connectionId,
                String.valueOf(properties.getTtl().toMillis()));
        return Long.valueOf(1L).equals(result);
    }

    public boolean release(String userId, String connectionId) {
        String key = key(userId);
        requireText(connectionId, "connectionId");

        Long result = redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(key),
                connectionId);
        return Long.valueOf(1L).equals(result);
    }

    public Optional<String> findEndpoint(String userId) {
        String key = key(userId);
        Object endpoint = redisTemplate.opsForHash().get(key, "endpoint");
        if (endpoint == null || isBlank(endpoint.toString())) {
            return Optional.empty();
        }
        return Optional.of(endpoint.toString());
    }

    private static String key(String userId) {
        return KEY_PREFIX + requireText(userId, "userId");
    }

    private static String requireText(String value, String name) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static <T> DefaultRedisScript<T> script(String path, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<T>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(resultType);
        return script;
    }
}
