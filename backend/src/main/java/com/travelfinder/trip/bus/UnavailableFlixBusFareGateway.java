package com.travelfinder.trip.bus;

import com.travelfinder.trip.domain.TripOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Nie odpytuje prywatnych endpointów FlixBusa bez umowy partnerskiej. */
@Configuration(proxyBeanMethods = false)
public class UnavailableFlixBusFareGateway {
    @Bean
    @ConditionalOnMissingBean(FlixBusFareGateway.class)
    public FlixBusFareGateway noFareGateway() {
        return (TripOption trip) -> java.util.Optional.empty();
    }
}
