package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "infinitechat.routing.online-route")
public class OnlineRouteProperties {

    private Duration ttl = Duration.ofMinutes(10);
    private String advertisedHttpEndpoint;

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Optional<String> getAdvertisedHttpEndpoint() {
        if (advertisedHttpEndpoint == null || advertisedHttpEndpoint.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(advertisedHttpEndpoint);
    }

    public void setAdvertisedHttpEndpoint(String advertisedHttpEndpoint) {
        this.advertisedHttpEndpoint = advertisedHttpEndpoint;
    }

    @PostConstruct
    void validate() {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("infinitechat.routing.online-route.ttl must be positive");
        }
    }
}
