package com.hotelaria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio no fuso horário do hotel. Injetar o Clock (em vez de usar LocalDate.now())
 * torna as regras de datas determinísticas nos testes.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.hotel.time-zone:America/Sao_Paulo}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
