package com.hotelaria.controllers;

import com.hotelaria.dtos.request.AccessValidationRequest;
import com.hotelaria.dtos.response.AccessLogResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.models.enums.AccessResult;
import com.hotelaria.services.AccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/access-logs")
@Tag(name = "Registros de acesso", description = "Validação de entrada nas áreas e histórico de tentativas")
public class AccessLogController {

    private final AccessLogService accessLogService;

    public AccessLogController(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('ACCESS_REGISTER')")
    @Operation(summary = "Valida e registra a entrada de um hóspede em uma área",
            description = "Permissão ACCESS_REGISTER. Sempre gera um registro (GRANTED ou DENIED com motivo).")
    public AccessLogResponse validate(@Valid @RequestBody AccessValidationRequest request) {
        return accessLogService.validateAndRegister(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACCESS_LOG_READ')")
    @Operation(summary = "Histórico de tentativas de acesso com filtros",
            description = "Permissão ACCESS_LOG_READ (administrador; pode ser concedida a funcionários).")
    public PageResponse<AccessLogResponse> list(
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) AccessResult result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(size = 20, sort = "attemptedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return accessLogService.list(guestId, areaId, bookingId, result, from, to, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Histórico de acessos do hóspede autenticado", description = "Somente hóspedes.")
    public PageResponse<AccessLogResponse> myLogs(
            @ParameterObject @PageableDefault(size = 20, sort = "attemptedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return accessLogService.listMine(pageable);
    }
}
