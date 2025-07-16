package kr.hhplus.be.server.common.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic ticketPurchaseEventsTopic() {
        return TopicBuilder.name("ticket-purchase-events")
                .partitions(3)      // ✅ 이 설정이 카프카 브로커에 적용됨
                .replicas(1)
                .build();
    }
}