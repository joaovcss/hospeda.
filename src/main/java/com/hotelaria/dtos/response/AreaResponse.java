package com.hotelaria.dtos.response;

import com.hotelaria.models.enums.AreaStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

public record AreaResponse(
        Long id,
        String name,
        String description,
        String location,
        @Schema(type = "string", example = "08:00:00") LocalTime openingTime,
        @Schema(type = "string", example = "22:00:00") LocalTime closingTime,
        AreaStatus status
) {
}
