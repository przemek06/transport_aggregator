package edu.pg.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import com.netflix.appinfo.InstanceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class GatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

    @Autowired
    private EurekaClient eurekaClient;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        InstanceInfo queryServiceInstance = eurekaClient.getNextServerFromEureka("query", false);
        String queryServiceUrl = queryServiceInstance.getHomePageUrl();
        logger.info("Query service URL={}", queryServiceUrl);
        InstanceInfo bookingServiceInstance = eurekaClient.getNextServerFromEureka("booking", false);
        String bookingServiceUrl = bookingServiceInstance.getHomePageUrl();
        logger.info("Booking service URL={}", bookingServiceUrl);
        InstanceInfo toImportInstance = eurekaClient.getNextServerFromEureka("to-import", false);
        String toImportInstanceUrl = toImportInstance.getHomePageUrl();
        logger.info("TO Import URL={}", toImportInstanceUrl);

        InstanceInfo bookingEventServiceInstance = eurekaClient.getNextServerFromEureka("booking-event", false);
        String bookingEventServiceUrl = bookingEventServiceInstance.getHomePageUrl();
        logger.info("Booking Event service URL={}", bookingEventServiceUrl);

        InstanceInfo customerInsightsServiceInstance = eurekaClient.getNextServerFromEureka("customer-insights", false);
        String customerInsightsServiceUrl = customerInsightsServiceInstance.getHomePageUrl();
        logger.info("Customer Insights Service URL={}", customerInsightsServiceUrl);

        return builder.routes()
                .route("query-service-route", r -> r.path("/query/**")
                        .filters(f -> f.addRequestHeader("X-Gateway-Route", "query-service"))
                        .uri(queryServiceUrl))
                .route("booking-service-route", r -> r.path("/reservations/**")
                        .filters(f -> f.addRequestHeader("X-Gateway-Route", "booking-service"))
                        .uri(bookingServiceUrl))
                .route("to-import-route", r -> r.path("/import/**")
                        .filters(f -> f.addRequestHeader("X-Gateway-Route", "to-import"))
                        .uri(toImportInstanceUrl))
                .route("booking-event-service-route", r -> r.path("/stream/**")
                        .filters(f -> f.addRequestHeader("X-Gateway-Route", "booking-event-service"))
                        .uri(bookingEventServiceUrl))
                .route("customer-insights-service-route", r -> r.path("/insights/**")
                        .filters(f -> f.addRequestHeader("X-Gateway-Route", "customer-insights-service"))
                        .uri(customerInsightsServiceUrl))
                .build();
    }
}