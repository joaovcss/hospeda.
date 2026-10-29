package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Base das exceções de negócio. Cada subclasse define o status HTTP correspondente,
 * e a mensagem é segura para ser exibida ao cliente.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
