package com.hotelaria.services.support;

import com.hotelaria.exceptions.InvalidBookingDatesException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regras de datas comuns à consulta de disponibilidade e à criação de hospedagens.
 */
@Component
public class BookingPeriodValidator {

    public static final int MAX_NIGHTS = 90;

    private final Clock clock;

    public BookingPeriodValidator(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public void validate(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidBookingDatesException("As datas de entrada e saída são obrigatórias");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidBookingDatesException("A data de saída deve ser posterior à data de entrada");
        }
        if (checkIn.isBefore(today())) {
            throw new InvalidBookingDatesException("A data de entrada não pode estar no passado");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new InvalidBookingDatesException("A hospedagem não pode exceder " + MAX_NIGHTS + " diárias");
        }
    }
}
