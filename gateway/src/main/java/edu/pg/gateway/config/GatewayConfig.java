package edu.pg.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("query-service-route", r -> r.path("/query/**")
                        .filters(f -> f.addRequestHeader("X-Gateway-Route", "query-service"))
                        .uri("http://query-service:8081"))
                .route("booking-service-route", r -> r.path("/reservations/**")
                        .filters(f -> f.addRequestHeader("X-Gateway-Route", "booking-service"))
                        .uri("http://booking-service:8090"))
                .build();
    }
}