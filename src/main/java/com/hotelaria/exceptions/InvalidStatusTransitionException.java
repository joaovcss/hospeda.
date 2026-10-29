package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/** A operação não é permitida no status atual do recurso. */
public class InvalidStatusTransitionException extends ApiException {

    public InvalidStatusTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public InvalidStatusTransitionException(Enum<?> from, Enum<?> to) {
        this("Transição de status inválida: " + from + " -> " + to);
    }
}
