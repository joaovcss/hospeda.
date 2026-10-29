package com.hotelaria.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record GuestCreateRequest(
        @NotBlank @Size(max = 150)
        String fullName,

        @NotBlank @Email @Size(max = 150)
        String email,

        @Schema(description = "CPF com ou sem formatação", example = "529.982.247-25")
        @NotBlank @CPF
        String cpf,

        @Schema(example = "+55 11 98888-7777")
        @NotBlank @Pattern(regexp = "[0-9()+\\- ]{8,20}", message = "deve ser um telefone válido")
        String phone,

        @Past
        LocalDate birthDate,

        @Valid
        AddressRequest address,

        @Schema(description = "Senha inicial de acesso do hóspede")
        @NotBlank @Size(min = 8, max = 72)
        String password
) {
}
