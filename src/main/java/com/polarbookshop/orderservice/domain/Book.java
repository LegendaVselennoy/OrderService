package com.polarbookshop.orderservice.domain;

public record Book(

        String isbn,
        String title,
        String author,
        Double price

) {}
