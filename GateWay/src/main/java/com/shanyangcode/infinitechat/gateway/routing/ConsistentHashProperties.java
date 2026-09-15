package com.shanyangcode.infinitechat.gateway.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "infinitechat.routing.consistent-hash")
public class ConsistentHashProperties {

    private int virtualNodes = 128;

    public int getVirtualNodes() {
        return virtualNodes;
    }

    public void setVirtualNodes(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    @PostConstruct
    public void validate() {
        if (virtualNodes <= 0) {
            throw new IllegalStateException("consistent hash virtual nodes must be positive");
        }
    }
}
