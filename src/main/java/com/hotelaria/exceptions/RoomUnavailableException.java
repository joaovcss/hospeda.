package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/** Quarto em manutenção, inativo ou sem condições de receber a hospedagem. */
public class RoomUnavailableException extends ApiException {

    public RoomUnavailableException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
