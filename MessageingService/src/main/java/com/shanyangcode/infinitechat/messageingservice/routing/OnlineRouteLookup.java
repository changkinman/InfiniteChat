package com.shanyangcode.infinitechat.messageingservice.routing;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public final class OnlineRouteLookup {

    private static final String USER_SESSION_KEY_PREFIX = "user:session:";
    private static final String ENDPOINT_FIELD = "endpoint";

    private final StringRedisTemplate redisTemplate;

    public OnlineRouteLookup(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> findEndpoint(Long userId) {
        Object endpoint = redisTemplate.opsForHash().get(USER_SESSION_KEY_PREFIX + userId, ENDPOINT_FIELD);
        if (!(endpoint instanceof String)) {
            return Optional.empty();
        }

        String trimmed = ((String) endpoint).trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    public Optional<String> resolveUrl(Long userId, String path) {
        return findEndpoint(userId).map(endpoint -> joinUrl(endpoint, path));
    }

    private String joinUrl(String endpoint, String path) {
        boolean endpointEndsWithSlash = endpoint.endsWith("/");
        boolean pathStartsWithSlash = path.startsWith("/");

        if (endpointEndsWithSlash && pathStartsWithSlash) {
            return endpoint + path.substring(1);
        }
        if (!endpointEndsWithSlash && !pathStartsWithSlash) {
            return endpoint + "/" + path;
        }
        return endpoint + path;
    }
}
