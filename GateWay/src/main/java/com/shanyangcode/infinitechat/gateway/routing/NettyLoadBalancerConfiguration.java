package com.shanyangcode.infinitechat.gateway.routing;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class NettyLoadBalancerConfiguration {

    @Bean
    public ReactorServiceInstanceLoadBalancer nettyLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory,
            ConsistentHashProperties properties) {
        String serviceName = LoadBalancerClientFactory.getName(environment);
        ObjectProvider<ServiceInstanceListSupplier> supplierProvider =
                loadBalancerClientFactory.getLazyProvider(serviceName, ServiceInstanceListSupplier.class);
        return new ConsistentHashLoadBalancer(supplierProvider, properties.getVirtualNodes());
    }
}
