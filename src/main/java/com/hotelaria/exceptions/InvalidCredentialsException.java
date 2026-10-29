package com.hotelaria.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Mensagem propositalmente genérica: não revela se o e-mail/CPF existe
 * nem se a conta está desativada.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
