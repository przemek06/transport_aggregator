package edu.pg.gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        logger.info("Is there a user?");
        return ReactiveSecurityContextHolder.getContext()
                .doOnNext(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    if (authentication != null && authentication.isAuthenticated()) {
                        String username = authentication.getName();
                        String authorities = authentication.getAuthorities().stream().map(a -> a.getAuthority())
                                .reduce((a, b) -> a + ", " + b)
                                .orElseGet(() -> "");
                        logger.info("Authenticated user: {}", username);
                        logger.info("Authenticated user authorities: {}", authorities);
                    }
                })
                .then(chain.filter(exchange)); // Continue with the chain of filters
    }
}
