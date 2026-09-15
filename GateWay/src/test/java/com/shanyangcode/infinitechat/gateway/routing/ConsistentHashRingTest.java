package com.shanyangcode.infinitechat.gateway.routing;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentHashRingTest {

    @Test
    void selectsSameNodeWhenInstancesAreSuppliedInDifferentOrder() {
        List<ServiceInstance> original = nodes(
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080),
                node("10.0.0.3", 8080)
        );
        List<ServiceInstance> reordered = nodes(
                node("10.0.0.3", 8080),
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080)
        );

        ConsistentHashRing firstRing = new ConsistentHashRing(original, 128);
        ConsistentHashRing secondRing = new ConsistentHashRing(reordered, 128);

        assertEquals(id(firstRing.select("10001")), id(secondRing.select("10001")));
    }

    @Test
    void addingFourthNodeToThreeNodesMovesExpectedShareOfUsers() {
        ConsistentHashRing threeNodeRing = new ConsistentHashRing(nodes(
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080),
                node("10.0.0.3", 8080)
        ), 128);
        ConsistentHashRing fourNodeRing = new ConsistentHashRing(nodes(
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080),
                node("10.0.0.3", 8080),
                node("10.0.0.4", 8080)
        ), 128);

        int moved = movedUsers(threeNodeRing, fourNodeRing);

        assertTrue(moved > 1500, "moved users should be more than 1,500 but was " + moved);
        assertTrue(moved < 3500, "moved users should be fewer than 3,500 but was " + moved);
    }

    @Test
    void removingFourthNodeMovesOnlyUsersPreviouslyOwnedByThatNode() {
        ConsistentHashRing threeNodeRing = new ConsistentHashRing(nodes(
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080),
                node("10.0.0.3", 8080)
        ), 128);
        ConsistentHashRing fourNodeRing = new ConsistentHashRing(nodes(
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080),
                node("10.0.0.3", 8080),
                node("10.0.0.4", 8080)
        ), 128);

        for (int user = 0; user < 10000; user++) {
            String key = String.valueOf(user);
            String beforeRemoval = id(fourNodeRing.select(key));
            String afterRemoval = id(threeNodeRing.select(key));

            if (!"10.0.0.4:8080".equals(beforeRemoval)) {
                assertEquals(beforeRemoval, afterRemoval, "user " + key + " should stay on its previous node");
            }
        }
    }

    @Test
    void distributesTenThousandUsersAcrossAllThreeNodesWithinBoundedSkew() {
        ConsistentHashRing ring = new ConsistentHashRing(nodes(
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080),
                node("10.0.0.3", 8080)
        ), 128);

        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (int user = 0; user < 10000; user++) {
            String nodeId = id(ring.select(String.valueOf(user)));
            Integer current = counts.get(nodeId);
            counts.put(nodeId, current == null ? 1 : current + 1);
        }

        assertEquals(3, counts.size());
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Integer count : counts.values()) {
            min = Math.min(min, count);
            max = Math.max(max, count);
        }
        assertTrue(max - min < 1500, "distribution skew should be less than 1,500 but was " + (max - min));
    }

    @Test
    void emptyRingSelectsNoNodeAndExposesNoNodeIds() {
        ConsistentHashRing ring = new ConsistentHashRing(new ArrayList<ServiceInstance>(), 128);

        assertNull(ring.select("10001"));
        assertTrue(ring.nodeIds().isEmpty());
    }

    @Test
    void rejectsNonPositiveVirtualNodeCounts() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConsistentHashRing(nodes(node("10.0.0.1", 8080)), 0);
        });
    }

    @Test
    void exposesSortedImmutablePhysicalNodeIds() {
        ConsistentHashRing ring = new ConsistentHashRing(nodes(
                node("10.0.0.3", 8080),
                node("10.0.0.1", 8080),
                node("10.0.0.2", 8080)
        ), 128);

        assertEquals(Arrays.asList("10.0.0.1:8080", "10.0.0.2:8080", "10.0.0.3:8080"), ring.nodeIds());
        assertThrows(UnsupportedOperationException.class, () -> {
            ring.nodeIds().add("10.0.0.4:8080");
        });
    }

    @Test
    void propertiesDefaultToOneHundredTwentyEightVirtualNodes() {
        ConsistentHashProperties properties = new ConsistentHashProperties();

        assertEquals(128, properties.getVirtualNodes());
    }

    @Test
    void propertiesRejectNonPositiveVirtualNodeCounts() {
        ConsistentHashProperties properties = new ConsistentHashProperties();
        properties.setVirtualNodes(0);

        assertThrows(IllegalStateException.class, () -> {
            properties.validate();
        });
    }

    private int movedUsers(ConsistentHashRing firstRing, ConsistentHashRing secondRing) {
        int moved = 0;
        for (int user = 0; user < 10000; user++) {
            String key = String.valueOf(user);
            if (!id(firstRing.select(key)).equals(id(secondRing.select(key)))) {
                moved++;
            }
        }
        return moved;
    }

    private List<ServiceInstance> nodes(ServiceInstance... instances) {
        return Arrays.asList(instances);
    }

    private ServiceInstance node(String host, int port) {
        return new DefaultServiceInstance(id(host, port), "chat", host, port, false);
    }

    private String id(ServiceInstance instance) {
        assertNotNull(instance);
        return id(instance.getHost(), instance.getPort());
    }

    private String id(String host, int port) {
        return host + ":" + port;
    }

}
