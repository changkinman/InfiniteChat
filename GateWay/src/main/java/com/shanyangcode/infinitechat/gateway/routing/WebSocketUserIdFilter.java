package com.shanyangcode.infinitechat.gateway.routing;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class WebSocketUserIdFilter implements WebFilter {

    private static final String NETTY_PATH = "/api/v1/netty";
    private static final String USER_UUID_HEADER = "userUuid";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!NETTY_PATH.equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        String userUuid = exchange.getRequest().getHeaders().getFirst(USER_UUID_HEADER);
        if (!StringUtils.hasText(userUuid)) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
