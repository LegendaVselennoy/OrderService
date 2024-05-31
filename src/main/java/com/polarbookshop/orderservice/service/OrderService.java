package com.polarbookshop.orderservice.service;

import com.polarbookshop.orderservice.domain.Book;
import com.polarbookshop.orderservice.domain.BookClient;
import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderStatus;
import com.polarbookshop.orderservice.order.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private final BookClient bookClient;
    private final OrderRepository orderRepository;

    public OrderService(BookClient bookClient,OrderRepository orderRepository) {
        this.bookClient=bookClient;
        this.orderRepository = orderRepository;
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> submitOrder(String isbn, int quantity) {
        // return Mono.just(buildRejectedOrder(isbn, quantity))     // Создает “Mono” из объекта “Order”
        return bookClient.getBookByIsbn(isbn)          // Вызов службы каталогов для проверки доступности книги
                .map(book -> buildAcceptedOrder(book, quantity))  // Если книга есть в наличии, она принимает заказ.
                .defaultIfEmpty(
                        buildRejectedOrder(isbn, quantity)         // Если книги нет в наличии, заказ отклоняется
                )
                .flatMap(orderRepository::save);  // Сохраняет объект Order, созданный асинхронно
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

}
