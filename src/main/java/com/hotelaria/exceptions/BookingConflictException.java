package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/** Já existe hospedagem confirmada para o mesmo quarto em período sobreposto. */
public class BookingConflictException extends ApiException {

    public static final String DEFAULT_MESSAGE =
            "O quarto já possui uma hospedagem confirmada em período conflitante";

    public BookingConflictException() {
        super(HttpStatus.CONFLICT, DEFAULT_MESSAGE);
    }
}
