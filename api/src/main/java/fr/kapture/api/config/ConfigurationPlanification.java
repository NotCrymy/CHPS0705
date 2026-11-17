package fr.kapture.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active {@code @Scheduled} (ping SSE toutes les 15 s, voir
 * {@code FluxService}). Classe séparée plutôt qu'annotation sur
 * {@code ApiApplication}, pour ne pas l'activer dans les tests
 * {@code @WebMvcTest}.
 */
@Configuration
@EnableScheduling
public class ConfigurationPlanification {
}
