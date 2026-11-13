package cl.duoc.analytics;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@EnableKafka
@Import({KafkaAutoConfiguration.class, JacksonAutoConfiguration.class, SalesConsumer.class})
public class SalesAnalyticsApplication {
    public static void run(String[] args) {
        new SpringApplicationBuilder(SalesAnalyticsApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }

    @Bean
    public DefaultErrorHandler salesErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000, 2));
    }
}
