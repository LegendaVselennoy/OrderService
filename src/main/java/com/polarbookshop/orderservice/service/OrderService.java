package com.polarbookshop.orderservice.service;

import com.polarbookshop.orderservice.domain.Book;
import com.polarbookshop.orderservice.domain.BookClient;
import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderStatus;
import com.polarbookshop.orderservice.order.OrderRepository;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private final BookClient bookClient;
    private final OrderRepository orderRepository;
    private static final Logger log= LoggerFactory.getLogger(OrderService.class);
    private final StreamBridge streamBridge;

    public OrderService(BookClient bookClient,StreamBridge streamBridge,OrderRepository orderRepository) {
        this.bookClient=bookClient;
        this.orderRepository = orderRepository;
        this.streamBridge=streamBridge;
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        // Принимает реактивный поток объектов OrderDispatchedMessage в качестве входных данных
        return flux
        // Для каждого объекта, выдаваемого потоку, он считывает соответствующий порядок из базы данных
                .flatMap(message -> orderRepository.findById(message.orderId()))
        // Обновляет заказ со статусом «отправлено»
                .map(this::buildDispatchedOrder)
        // Сохраняет обновленный заказ в базе данных
                .flatMap(orderRepository::save);
    }

    // Получив заказ, он возвращает новую запись со статусом "отправлено"
    private Order buildDispatchedOrder(Order existingOrder) {
        return new Order(
                existingOrder.id(),
                existingOrder.bookIsbn(),
                existingOrder.bookName(),
                existingOrder.bookPrice(),
                existingOrder.quantity(),
                OrderStatus.DISPATCHED,
                existingOrder.createDate(),
                existingOrder.lastModifiedDate(),
                existingOrder.version()
        );
    }

    // Выполняет метод в локальной транзакции
    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity) {
        // return Mono.just(buildRejectedOrder(isbn, quantity))     // Создает “Mono” из объекта “Order”
        return bookClient.getBookByIsbn(isbn)          // Вызов службы каталогов для проверки доступности книги
                .map(book -> buildAcceptedOrder(book, quantity))  // Если книга есть в наличии, она принимает заказ.
                .defaultIfEmpty(
                        buildRejectedOrder(isbn, quantity)         // Если книги нет в наличии, заказ отклоняется
                )
                .flatMap(orderRepository::save)  // Сохраняет объект Order, созданный асинхронно. Сохраняет заказ в базе данных
                .doOnNext(this::publishOrderAcceptedEvent); // Публикует событие, если заказ принят
        // на предыдущем шаге реактивного потока, в базе данных. Сохраняет заказ (как принятый или отклоненный)
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        // При принятии заказа мы указываем ISBN, название книги (название + автор), количество и статус.
        // Spring Data позаботится о добавлении идентификатора, версии и метаданных аудита.
        return Order.of(book.isbn(), book.title() + " - " + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
    }

    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }

    private void publishOrderAcceptedEvent(Order order){
        if (!order.status().equals(OrderStatus.ACCEPTED)){
            return; // Если заказ не принят, он ничего не делает
        }
        var orderAcceptedMessage=new OrderAcceptedMessage(order.id());
        // Создает сообщение для уведомления о том, что заказ принят
        log.info("Отправка события order accepted с id: {}",order.id());
        // Явно отправляет сообщение в привязку acceptOrder-out-0
        var result=streamBridge.send("acceptOrder-out-0",orderAcceptedMessage);
        log.info("Результат отправки данных для заказа с id {}: {}",order.id(),result);
    }

}
