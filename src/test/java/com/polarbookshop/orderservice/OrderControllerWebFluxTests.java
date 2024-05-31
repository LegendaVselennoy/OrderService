package com.polarbookshop.orderservice;

import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderStatus;
import com.polarbookshop.orderservice.service.OrderService;
import com.polarbookshop.orderservice.web.OrderController;
import com.polarbookshop.orderservice.web.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

// Определяет тестовый класс, который фокусируется на компонентах Spring WebFlux, ориентируясь на OrderController
@WebFluxTest(OrderController.class)
public class OrderControllerWebFluxTests {

    // Вариант WebClient с дополнительными функциями, упрощающими тестирование RESTful-сервисов
    @Autowired
    private WebTestClient webClient;

    // Добавляет макет OrderService в контекст приложения Spring
    @MockBean
    private OrderService orderService;

    @Test
    void whenBookNotAvailableThenRejectOrder() {
        var orderRequest = new OrderRequest("1234567890", 3);
        var expectedOrder = OrderService.buildRejectedOrder(
                orderRequest.isbn(), orderRequest.quantity());
        // Определяет ожидаемое поведение для макета bean-компонента OrderService
        given(orderService.submitOrder(
                orderRequest.isbn(), orderRequest.quantity())).willReturn(Mono.just(expectedOrder));

        webClient
                .post()
                .uri("/orders/")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                });
    }
}
