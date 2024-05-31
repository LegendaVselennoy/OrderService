package com.polarbookshop.orderservice.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(

        @NotBlank(message = "Необходимо указать ISBN книги.")
        String isbn,

        @NotNull(message = "Необходимо определить количество книг.")
        @Min(value = 1, message = "Вы должны заказать хотя бы 1 товар.")
        @Max(value = 5, message = "Вы не можете заказать более 5 позиций.")
        Integer quantity
) {
}
