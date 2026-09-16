package com.shanyangcode.infinitechat.realtimecommunicationservice.routing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

@Configuration
public class RoutingConfiguration {

    @Bean
    public LocalNode localNode(@Value("${netty.port}") int nettyPort,
                               @Value("${server.port}") int serverPort,
                               OnlineRouteProperties properties) throws UnknownHostException {
        String host = InetAddress.getLocalHost().getHostAddress();
        String endpoint = properties.getAdvertisedHttpEndpoint()
                .orElse("http://" + host + ":" + serverPort);
        return new LocalNode(host + ":" + nettyPort, endpoint);
    }

    @Bean(name = "connectionTakeoverRestOperations")
    public RestOperations connectionTakeoverRestOperations(ConnectionTakeoverProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(toTimeoutMillis(properties.getConnectTimeout(), "connectTimeout"));
        requestFactory.setReadTimeout(toTimeoutMillis(properties.getReadTimeout(), "readTimeout"));
        return new RestTemplate(requestFactory);
    }

    private int toTimeoutMillis(Duration timeout, String name) {
        long millis = timeout.toMillis();
        if (millis <= 0 || millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " must be between 1 and " + Integer.MAX_VALUE + " milliseconds");
        }
        return (int) millis;
    }
}
