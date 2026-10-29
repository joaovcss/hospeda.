package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/** E-mail, CPF, número de quarto ou nome de área já cadastrados. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
