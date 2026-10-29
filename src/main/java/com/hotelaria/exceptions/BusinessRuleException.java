package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/** Requisição bem formada, mas que viola uma regra de negócio. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
