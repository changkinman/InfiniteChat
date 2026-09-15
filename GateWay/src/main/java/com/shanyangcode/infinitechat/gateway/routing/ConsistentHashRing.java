package com.shanyangcode.infinitechat.gateway.routing;

import org.springframework.cloud.client.ServiceInstance;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private final NavigableMap<BigInteger, ServiceInstance> ring;
    private final List<String> nodeIds;

    public ConsistentHashRing(List<ServiceInstance> instances, int virtualNodes) {
        if (virtualNodes <= 0) {
            throw new IllegalArgumentException("virtualNodes must be positive");
        }

        List<ServiceInstance> copiedInstances = new ArrayList<ServiceInstance>(instances);
        Collections.sort(copiedInstances, new Comparator<ServiceInstance>() {
            @Override
            public int compare(ServiceInstance first, ServiceInstance second) {
                return nodeId(first).compareTo(nodeId(second));
            }
        });

        TreeMap<BigInteger, ServiceInstance> mutableRing = new TreeMap<BigInteger, ServiceInstance>();
        List<String> mutableNodeIds = new ArrayList<String>();
        for (ServiceInstance instance : copiedInstances) {
            String nodeId = nodeId(instance);
            mutableNodeIds.add(nodeId);
            for (int replica = 0; replica < virtualNodes; replica++) {
                mutableRing.put(hash(nodeId + "#" + replica), instance);
            }
        }

        this.ring = Collections.unmodifiableNavigableMap(mutableRing);
        this.nodeIds = Collections.unmodifiableList(mutableNodeIds);
    }

    public ServiceInstance select(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        Map.Entry<BigInteger, ServiceInstance> entry = ring.ceilingEntry(hash(key));
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    public List<String> nodeIds() {
        return nodeIds;
    }

    private static String nodeId(ServiceInstance instance) {
        return instance.getHost() + ":" + instance.getPort();
    }

    private static BigInteger hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new BigInteger(1, digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
