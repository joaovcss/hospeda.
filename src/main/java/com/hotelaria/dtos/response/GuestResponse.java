package com.hotelaria.dtos.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GuestResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String cpf,
        String phone,
        LocalDate birthDate,
        AddressResponse address,
        boolean active,
        LocalDateTime createdAt
) {
}
