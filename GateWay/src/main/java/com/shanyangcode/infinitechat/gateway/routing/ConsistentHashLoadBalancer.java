package com.shanyangcode.infinitechat.gateway.routing;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ConsistentHashLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    private final int virtualNodes;
    private final AtomicReference<ConsistentHashRing> snapshot = new AtomicReference<ConsistentHashRing>();

    public ConsistentHashLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> supplierProvider, int virtualNodes) {
        this.supplierProvider = supplierProvider;
        this.virtualNodes = virtualNodes;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = supplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }

        final String userUuid = userUuidFrom(request);
        return supplier.get(request)
                .next()
                .map(instances -> {
                    if (instances.isEmpty()) {
                        return new EmptyResponse();
                    }
                    ServiceInstance selected = ringFor(instances).select(userUuid);
                    if (selected == null) {
                        return new EmptyResponse();
                    }
                    return new DefaultResponse(selected);
                })
                .defaultIfEmpty(new EmptyResponse());
    }

    ConsistentHashRing ringSnapshot() {
        return snapshot.get();
    }

    private synchronized ConsistentHashRing ringFor(List<ServiceInstance> instances) {
        List<String> currentNodeIds = sortedNodeIds(instances);
        ConsistentHashRing current = snapshot.get();
        if (current != null && current.nodeIds().equals(currentNodeIds)) {
            return current;
        }

        ConsistentHashRing replacement = new ConsistentHashRing(instances, virtualNodes);
        snapshot.set(replacement);
        return replacement;
    }

    private String userUuidFrom(Request request) {
        Object context = request.getContext();
        if (context instanceof RequestDataContext) {
            RequestData data = ((RequestDataContext) context).getClientRequest();
            String userUuid = data.getHeaders().getFirst("userUuid");
            return userUuid == null ? "" : userUuid;
        }
        return "";
    }

    private List<String> sortedNodeIds(List<ServiceInstance> instances) {
        List<ServiceInstance> sorted = new ArrayList<ServiceInstance>(instances);
        Collections.sort(sorted, new Comparator<ServiceInstance>() {
            @Override
            public int compare(ServiceInstance first, ServiceInstance second) {
                return nodeId(first).compareTo(nodeId(second));
            }
        });

        List<String> nodeIds = new ArrayList<String>();
        for (ServiceInstance instance : sorted) {
            nodeIds.add(nodeId(instance));
        }
        return nodeIds;
    }

    private String nodeId(ServiceInstance instance) {
        return instance.getHost() + ":" + instance.getPort();
    }
}
