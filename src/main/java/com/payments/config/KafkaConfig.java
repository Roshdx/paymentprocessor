package com.payments.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentsProcessTopic() {
        return TopicBuilder.name("payments.process")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic paymentsDeadLetterTopic() {
        return TopicBuilder.name("payments.dead-letter")
            .partitions(1)
            .replicas(1)
            .build();
    }
}