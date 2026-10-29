package com.hotelaria.controllers;

import com.hotelaria.dtos.request.GuestCreateRequest;
import com.hotelaria.dtos.request.GuestUpdateRequest;
import com.hotelaria.dtos.response.AccessCheckResponse;
import com.hotelaria.dtos.response.BookingAreaAccessResponse;
import com.hotelaria.dtos.response.GuestResponse;
import com.hotelaria.dtos.response.GuestSummaryResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.services.AreaAccessService;
import com.hotelaria.services.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/guests")
@Tag(name = "Hóspedes", description = "Cadastro de hóspedes e consultas do próprio hóspede")
public class GuestController {

    private final GuestService guestService;
    private final AreaAccessService areaAccessService;

    public GuestController(GuestService guestService, AreaAccessService areaAccessService) {
        this.guestService = guestService;
        this.areaAccessService = areaAccessService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GUEST_CREATE')")
    @Operation(summary = "Cadastra hóspede e suas credenciais de acesso", description = "Permissão GUEST_CREATE.")
    public ResponseEntity<GuestResponse> create(@Valid @RequestBody GuestCreateRequest request) {
        GuestResponse created = guestService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GUEST_READ')")
    @Operation(summary = "Lista hóspedes (CPF mascarado)",
            description = "Permissão GUEST_READ. O filtro 'search' busca por nome, e-mail ou CPF.")
    public PageResponse<GuestSummaryResponse> list(@RequestParam(required = false) String search,
                                                   @ParameterObject @PageableDefault(size = 20, sort = "id")
                                                   Pageable pageable) {
        return guestService.list(search, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Dados cadastrais do hóspede autenticado", description = "Somente hóspedes.")
    public GuestResponse me() {
        return guestService.getMyProfile();
    }

    @GetMapping("/me/areas")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Áreas que o hóspede autenticado está autorizado a acessar", description = "Somente hóspedes.")
    public List<BookingAreaAccessResponse> myAreas() {
        return areaAccessService.listMine();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GUEST_READ') or hasRole('GUEST')")
    @Operation(summary = "Consulta um hóspede",
            description = "Permissão GUEST_READ. Hóspedes só podem consultar o próprio cadastro (403 caso contrário).")
    public GuestResponse findById(@PathVariable Long id) {
        return guestService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GUEST_UPDATE')")
    @Operation(summary = "Atualiza os dados de um hóspede", description = "Permissão GUEST_UPDATE.")
    public GuestResponse update(@PathVariable Long id, @Valid @RequestBody GuestUpdateRequest request) {
        return guestService.update(id, request);
    }

    @GetMapping("/{id}/areas")
    @PreAuthorize("hasAuthority('ACCESS_READ')")
    @Operation(summary = "Todas as autorizações de acesso às áreas de um hóspede", description = "Permissão ACCESS_READ.")
    public List<BookingAreaAccessResponse> guestAreas(@PathVariable Long id) {
        return areaAccessService.listByGuest(id);
    }

    @GetMapping("/{guestId}/areas/{areaId}/authorization")
    @PreAuthorize("hasAuthority('ACCESS_READ')")
    @Operation(summary = "Verifica se o hóspede pode acessar a área agora (sem registrar tentativa)",
            description = "Permissão ACCESS_READ.")
    public AccessCheckResponse checkAuthorization(@PathVariable Long guestId, @PathVariable Long areaId) {
        return areaAccessService.check(guestId, areaId);
    }
}
