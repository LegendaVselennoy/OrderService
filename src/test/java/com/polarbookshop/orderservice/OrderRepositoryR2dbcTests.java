package com.polarbookshop.orderservice;

import com.polarbookshop.orderservice.config.DataConfig;
import com.polarbookshop.orderservice.domain.OrderStatus;
import com.polarbookshop.orderservice.order.OrderRepository;
import com.polarbookshop.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

// Определяет тестовый класс, ориентированный на компоненты R2DBC
@DataR2dbcTest
@Import(DataConfig.class)    // Импортирует конфигурацию R2DBC, необходимую для включения аудита
@Testcontainers             // Активирует автоматический запуск и очистку тестовых контейнеров
public class OrderRepositoryR2dbcTests {

    // Идентификация контейнера PostgreSQL для тестирования
    @Container
    static PostgreSQLContainer<?> postgresql=
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Autowired
    private OrderRepository orderRepository;

    // Перезаписывает конфигурацию R2DBC и Flyway, чтобы указать на тестовый экземпляр PostgreSQL
    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry){
        registry.add("spring.r2dbc.url", OrderRepositoryR2dbcTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
    }

    // Создает строку подключения R2DBC, так как Testcontainers не предоставляет ее из коробки, как это происходит с JDBC
    private static String r2dbcUrl(){
        return String.format("r2dbc:postgresql://%s:%s/%s",
        postgresql.getContainerIpAddress(),
        postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
        postgresql.getDatabaseName());
    }

    @Test
    void createRejectedOrder(){
        var rejectedOrder= OrderService.buildRejectedOrder("1234567890",3);

        StepVerifier
                .create(orderRepository.save(rejectedOrder)) // Инициализирует объект StepVerifier объектом, возвращаемым OrderRepository
                .expectNextMatches(                           // Подтверждает, что возвращенный заказ имеет правильный статус
                        order -> order.status().equals(OrderStatus.REJECTED))
                .verifyComplete();                           // Проверяет успешное завершение реактивного потока
    }
}
