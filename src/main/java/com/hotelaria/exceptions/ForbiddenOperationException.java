package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/** O usuário está autenticado, mas não pode executar a operação sobre o recurso. */
public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
