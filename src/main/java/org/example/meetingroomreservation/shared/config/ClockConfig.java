package org.example.meetingroomreservation.shared.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj se inyecta como dependencia para que las reglas temporales sean deterministas en los
 * tests. El dominio nunca llama a {@code OffsetDateTime.now()} sin reloj.
 */
@Configuration
class ClockConfig {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
