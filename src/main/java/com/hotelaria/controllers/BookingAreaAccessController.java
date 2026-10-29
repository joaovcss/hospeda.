package com.hotelaria.controllers;

import com.hotelaria.dtos.request.AreaAccessGrantRequest;
import com.hotelaria.dtos.response.BookingAreaAccessResponse;
import com.hotelaria.services.AreaAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings/{bookingId}/areas")
@Tag(name = "Acesso às áreas", description = "Autorizações de acesso vinculadas à hospedagem")
public class BookingAreaAccessController {

    private final AreaAccessService areaAccessService;

    public BookingAreaAccessController(AreaAccessService areaAccessService) {
        this.areaAccessService = areaAccessService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCESS_GRANT')")
    @Operation(summary = "Concede acesso da hospedagem a uma área",
            description = "Permissão ACCESS_GRANT (administrador; pode ser concedida a funcionários).")
    public ResponseEntity<BookingAreaAccessResponse> grant(@PathVariable Long bookingId,
                                                           @Valid @RequestBody AreaAccessGrantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(areaAccessService.grant(bookingId, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACCESS_READ') or hasRole('GUEST')")
    @Operation(summary = "Áreas autorizadas para a hospedagem",
            description = "Permissão ACCESS_READ. Hóspedes só consultam as próprias hospedagens.")
    public List<BookingAreaAccessResponse> list(@PathVariable Long bookingId) {
        return areaAccessService.listByBooking(bookingId);
    }

    @DeleteMapping("/{areaId}")
    @PreAuthorize("hasAuthority('ACCESS_REVOKE')")
    @Operation(summary = "Revoga o acesso da hospedagem à área",
            description = "Permissão ACCESS_REVOKE. O registro é mantido com status REVOKED para auditoria.")
    public ResponseEntity<Void> revoke(@PathVariable Long bookingId, @PathVariable Long areaId) {
        areaAccessService.revoke(bookingId, areaId);
        return ResponseEntity.noContent().build();
    }
}
