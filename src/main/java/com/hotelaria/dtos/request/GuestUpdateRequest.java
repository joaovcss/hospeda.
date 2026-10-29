package com.hotelaria.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Atualização dos dados cadastrais. O CPF é o identificador do hóspede e não pode ser alterado.
 */
public record GuestUpdateRequest(
        @NotBlank @Size(max = 150)
        String fullName,

        @NotBlank @Email @Size(max = 150)
        String email,

        @NotBlank @Pattern(regexp = "[0-9()+\\- ]{8,20}", message = "deve ser um telefone válido")
        String phone,

        @Past
        LocalDate birthDate,

        @Valid
        AddressRequest address
) {
}
