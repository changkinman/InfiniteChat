package com.shanyangcode.infinitechat.gateway;

import com.shanyangcode.infinitechat.gateway.routing.NettyLoadBalancerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;

@SpringBootApplication
@LoadBalancerClient(name = "NettyService", configuration = NettyLoadBalancerConfiguration.class)
public class GateWayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GateWayApplication.class, args);
    }

}
