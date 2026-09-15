package com.shanyangcode.infinitechat.gateway.routing;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketUserIdFilterTest {

    @Test
    void missingUserUuidOnWebSocketPathReturnsBadRequestAndDoesNotContinue() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/netty")
        );
        AtomicBoolean continued = new AtomicBoolean(false);

        new WebSocketUserIdFilter().filter(exchange, webExchange -> {
            continued.set(true);
            return Mono.empty();
        }).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertFalse(continued.get());
    }

    @Test
    void blankUserUuidOnWebSocketPathReturnsBadRequestAndDoesNotContinue() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/netty").header("userUuid", "   ")
        );
        AtomicBoolean continued = new AtomicBoolean(false);

        new WebSocketUserIdFilter().filter(exchange, webExchange -> {
            continued.set(true);
            return Mono.empty();
        }).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertFalse(continued.get());
    }

    @Test
    void validUserUuidOnWebSocketPathContinues() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/netty").header("userUuid", "10001")
        );
        AtomicBoolean continued = new AtomicBoolean(false);

        new WebSocketUserIdFilter().filter(exchange, webExchange -> {
            continued.set(true);
            return Mono.empty();
        }).block();

        assertTrue(continued.get());
    }

    @Test
    void nonWebSocketPathRemainsUnaffectedAndContinues() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/chat/messages")
        );
        AtomicBoolean continued = new AtomicBoolean(false);

        new WebSocketUserIdFilter().filter(exchange, webExchange -> {
            continued.set(true);
            return Mono.empty();
        }).block();

        assertTrue(continued.get());
    }
}
