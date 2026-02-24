package com.wolffsoft.jdrivenecommerce.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Value("${app.kafka.topics.product-events}")
    private String topic;

    @Value("${app.kafka.topics.product-events-dlt}")
    private String topicDlt;

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder
                .name(topic)
                .partitions(6)
                .replicas(1) // Production it would be 3
                .build();
    }

    @Bean
    public NewTopic productEventsTopicDlt() {
        return TopicBuilder
                .name(topicDlt)
                .partitions(6)
                .replicas(1) // Production it would be 3
                .build();
    }
}
