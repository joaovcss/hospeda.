package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidBookingDatesException extends ApiException {

    public InvalidBookingDatesException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
