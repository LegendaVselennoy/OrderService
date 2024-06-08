package com.polarbookshop.orderservice.order.event;

import com.polarbookshop.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.io.Flushable;
import java.util.function.Consumer;

@Configuration
public class OrderFunctions {

    private static final Logger log= LoggerFactory.getLogger(OrderFunctions.class);

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService){
        return flux->
        // Для каждого отправленного сообщения обновляется соответствующий порядок в базе данных
                orderService.consumeOrderDispatchedEvent(flux)
                        .doOnNext(order -> log.info("Заказ с идентификатором {} отправлен.",
        // Для каждого заказа, обновленного в базе данных, он регистрирует сообщение
                                order.id()))
        // Подписывается на реактивный поток, чтобы активировать его. Без подписчика данные не проходят через поток.
                        .subscribe();
    }
}
