package com.hotelaria.dtos.request;

import com.hotelaria.models.enums.RoomCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomRequest(
        @Schema(example = "101")
        @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Za-z0-9-]+", message = "deve conter apenas letras, números e hífen")
        String number,

        @NotNull @Min(0) @Max(200)
        Integer floor,

        @NotNull @Min(1) @Max(20)
        Integer capacity,

        @NotNull
        RoomCategory category,

        @Schema(example = "350.00")
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)
        BigDecimal dailyRate,

        @Size(max = 500)
        String description
) {
}
