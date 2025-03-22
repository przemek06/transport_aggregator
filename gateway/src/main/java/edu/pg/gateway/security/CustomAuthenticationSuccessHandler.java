package edu.pg.gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
class CustomAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

        @Override
        public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
            webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.OK);

            return webFilterExchange.getExchange().getResponse().writeWith(Mono.just(
                    webFilterExchange.getExchange().getResponse().bufferFactory().wrap("Authentication successful".getBytes())));
        }
    }