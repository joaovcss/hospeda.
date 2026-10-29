package com.hotelaria.models.enums;

public enum AccessDenialReason {
    AREA_UNAVAILABLE("A área não está em funcionamento"),
    OUTSIDE_OPENING_HOURS("Fora do horário de funcionamento da área"),
    NOT_AUTHORIZED("O hóspede não possui autorização para esta área"),
    AUTHORIZATION_REVOKED("A autorização de acesso foi revogada"),
    BOOKING_CANCELLED("A hospedagem foi cancelada"),
    BOOKING_NOT_ACTIVE("A hospedagem não está em andamento (check-in não realizado ou já finalizada)"),
    OUTSIDE_AUTHORIZATION_PERIOD("Fora do período válido da autorização");

    private final String description;

    AccessDenialReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
