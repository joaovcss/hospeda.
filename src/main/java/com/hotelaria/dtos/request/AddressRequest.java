package com.hotelaria.dtos.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 150) String street,
        @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @Size(max = 100) String district,
        @Size(max = 100) String city,
        @Pattern(regexp = "[A-Za-z]{2}", message = "deve conter a sigla do estado com 2 letras") String state,
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "deve ser um CEP válido") String zipCode
) {
}
