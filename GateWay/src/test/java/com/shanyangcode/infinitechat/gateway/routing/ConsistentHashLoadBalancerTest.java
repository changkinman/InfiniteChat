package com.shanyangcode.infinitechat.gateway.routing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentHashLoadBalancerTest {

    @Test
    void userUuidSelectsSameServerForIdenticalMembershipInDifferentOrders() {
        Response<ServiceInstance> first = balancerWith(
                node("10.0.0.1", 9000),
                node("10.0.0.2", 9000),
                node("10.0.0.3", 9000)
        ).choose(request("10001")).block();
        Response<ServiceInstance> second = balancerWith(
                node("10.0.0.3", 9000),
                node("10.0.0.1", 9000),
                node("10.0.0.2", 9000)
        ).choose(request("10001")).block();

        assertTrue(first.hasServer());
        assertTrue(second.hasServer());
        assertEquals(id(first.getServer()), id(second.getServer()));
    }

    @Test
    void emptyNacosMembershipProducesResponseWithoutServer() {
        Response<ServiceInstance> response = balancerWith().choose(request("10001")).block();

        assertFalse(response.hasServer());
    }

    @Test
    void snapshotIdentityStaysForReorderedMembershipAndChangesWhenNodeAdded() {
        MutableSupplier supplier = new MutableSupplier(Arrays.asList(
                node("10.0.0.1", 9000),
                node("10.0.0.2", 9000)
        ));
        ConsistentHashLoadBalancer balancer = balancerWith(supplier);

        balancer.choose(request("10001")).block();
        ConsistentHashRing firstSnapshot = balancer.ringSnapshot();
        supplier.set(Arrays.asList(
                node("10.0.0.2", 9000),
                node("10.0.0.1", 9000)
        ));
        balancer.choose(request("10001")).block();
        assertSame(firstSnapshot, balancer.ringSnapshot());

        supplier.set(Arrays.asList(
                node("10.0.0.1", 9000),
                node("10.0.0.2", 9000),
                node("10.0.0.3", 9000)
        ));
        balancer.choose(request("10001")).block();
        assertNotSame(firstSnapshot, balancer.ringSnapshot());
    }

    private ConsistentHashLoadBalancer balancerWith(ServiceInstance... instances) {
        return balancerWith(new MutableSupplier(Arrays.asList(instances)));
    }

    private ConsistentHashLoadBalancer balancerWith(ServiceInstanceListSupplier supplier) {
        return new ConsistentHashLoadBalancer(new StaticObjectProvider<ServiceInstanceListSupplier>(supplier), 128);
    }

    private Request<RequestDataContext> request(String userUuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("userUuid", userUuid);
        RequestData data = new RequestData(
                HttpMethod.GET,
                URI.create("ws://localhost/api/v1/netty"),
                headers,
                new LinkedMultiValueMap<String, String>(),
                new HashMap<String, Object>()
        );
        return new DefaultRequest<RequestDataContext>(new RequestDataContext(data));
    }

    private ServiceInstance node(String host, int port) {
        return new DefaultServiceInstance(id(host, port), "NettyService", host, port, false);
    }

    private String id(ServiceInstance instance) {
        return id(instance.getHost(), instance.getPort());
    }

    private String id(String host, int port) {
        return host + ":" + port;
    }

    private static final class MutableSupplier implements ServiceInstanceListSupplier {
        private List<ServiceInstance> instances;

        private MutableSupplier(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        private void set(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        @Override
        public String getServiceId() {
            return "NettyService";
        }

        @Override
        public Flux<List<ServiceInstance>> get() {
            return Flux.just(instances);
        }

        @Override
        public Flux<List<ServiceInstance>> get(Request request) {
            return Flux.just(instances);
        }
    }

    private static final class StaticObjectProvider<T> implements ObjectProvider<T> {
        private final T instance;

        private StaticObjectProvider(T instance) {
            this.instance = instance;
        }

        @Override
        public T getObject(Object... args) throws BeansException {
            return instance;
        }

        @Override
        public T getIfAvailable() throws BeansException {
            return instance;
        }

        @Override
        public T getIfUnique() throws BeansException {
            return instance;
        }

        @Override
        public T getObject() throws BeansException {
            return instance;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singleton(instance).iterator();
        }
    }
}
