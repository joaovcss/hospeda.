package com.hotelaria.controllers;

import com.hotelaria.dtos.request.ChangePasswordRequest;
import com.hotelaria.dtos.request.LoginRequest;
import com.hotelaria.dtos.response.AuthenticatedUserResponse;
import com.hotelaria.dtos.response.LoginResponse;
import com.hotelaria.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login único para administradores, funcionários e hóspedes")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autentica com e-mail (qualquer usuário) ou CPF (hóspedes) e retorna o token JWT",
            description = "Público. Credenciais inválidas sempre retornam 401 com a mesma mensagem.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado", description = "Qualquer usuário autenticado.")
    public AuthenticatedUserResponse me() {
        return authService.getAuthenticatedUser();
    }

    @PutMapping("/password")
    @Operation(summary = "Altera a própria senha", description = "Qualquer usuário autenticado.")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
