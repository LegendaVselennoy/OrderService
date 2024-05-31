package com.polarbookshop.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfig {

    @Bean
        // Объект, автоматически сконфигурированный Spring Boot для сборки объектов WebClient
    WebClient webClient(ClientProperties clientProperties, WebClient.Builder webClientBuilder) {
        return webClientBuilder
                // Настраивает базовый URL-адрес WebClient на URL-адрес службы каталога, определенный как пользовательское свойство
                .baseUrl(clientProperties.catalogServiceUri().toString())
                .build();
    }

}
