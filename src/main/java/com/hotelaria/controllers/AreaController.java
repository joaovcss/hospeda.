package com.hotelaria.controllers;

import com.hotelaria.dtos.request.AreaRequest;
import com.hotelaria.dtos.request.AreaStatusRequest;
import com.hotelaria.dtos.response.AreaResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.models.enums.AreaStatus;
import com.hotelaria.services.AreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/areas")
@Tag(name = "Áreas do hotel", description = "Academia, piscina, restaurante, spa, etc.")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('AREA_CREATE')")
    @Operation(summary = "Cadastra uma área", description = "Exclusivo do administrador.")
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaRequest request) {
        AreaResponse created = areaService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AREA_READ')")
    @Operation(summary = "Lista áreas", description = "Permissão AREA_READ (todos os papéis).")
    public PageResponse<AreaResponse> list(@RequestParam(required = false) AreaStatus status,
                                           @ParameterObject @PageableDefault(size = 20, sort = "name")
                                           Pageable pageable) {
        return areaService.list(status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AREA_READ')")
    @Operation(summary = "Consulta uma área", description = "Permissão AREA_READ (todos os papéis).")
    public AreaResponse findById(@PathVariable Long id) {
        return areaService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('AREA_UPDATE')")
    @Operation(summary = "Atualiza uma área", description = "Exclusivo do administrador.")
    public AreaResponse update(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return areaService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('AREA_UPDATE')")
    @Operation(summary = "Ativa, indisponibiliza ou desativa uma área", description = "Exclusivo do administrador.")
    public AreaResponse updateStatus(@PathVariable Long id, @Valid @RequestBody AreaStatusRequest request) {
        return areaService.updateStatus(id, request.status());
    }
}
