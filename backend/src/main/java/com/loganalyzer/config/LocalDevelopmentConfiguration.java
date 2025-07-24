package com.loganalyzer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Configuration for local development environment.
 * Disables Elasticsearch and other external dependencies for local development.
 * Provides necessary beans for local development.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    ElasticsearchRestClientAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    KafkaAutoConfiguration.class
})
public class LocalDevelopmentConfiguration {

    /**
     * Provides RestTemplate bean for local development.
     * This is needed by NotificationService for webhook notifications.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
