package com.polarbookshop.orderservice.domain;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class BookClient {

    private static final String BOOKS_ROOT_API="/books";
    private final WebClient webClient;

    public BookClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Book> getBookByIsbn(String isbn){
        return webClient
                .get()                                          // В запросе должен использоваться метод GET.
                .uri(BOOKS_ROOT_API+isbn)                   // Целевой URI запроса — /books/{isbn}.
                .retrieve()                                     // Отправляет запрос и получает ответ
                .bodyToMono(Book.class)                         // Возвращает полученный объект как Mono<Book>
                .timeout(Duration.ofSeconds(3),Mono.empty())    // Устанавливает 3-секундный тайм-аут для запроса GET
                                                                // Резервный вариант возвращает пустой объект Mono.

                // Возвращает пустой объект при получении ответа 404
                .onErrorResume(WebClientResponseException.NotFound.class,
                        exception->Mono.empty())

                // Экспоненциальная задержка используется в качестве стратегии повторных попыток. Допускается три попытки с начальной задержкой 100 мс.
                .retryWhen(
                        Retry.backoff(3,Duration.ofMillis(100)))

                // Если после 3 повторных попыток возникает какая-либо ошибка, перехватывайте исключение и возвращайте пустой объект
                .onErrorResume(Exception.class,exception->Mono.empty());
    }
}
