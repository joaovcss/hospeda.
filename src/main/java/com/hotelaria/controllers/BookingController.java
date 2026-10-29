package com.hotelaria.controllers;

import com.hotelaria.dtos.request.BookingCancelRequest;
import com.hotelaria.dtos.request.BookingCreateRequest;
import com.hotelaria.dtos.request.BookingScope;
import com.hotelaria.dtos.response.BookingResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.models.enums.BookingStatus;
import com.hotelaria.services.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Hospedagens", description = "Reservas, confirmação, check-in, check-out e cancelamento")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BOOKING_CREATE') or hasRole('GUEST')")
    @Operation(summary = "Cria uma hospedagem",
            description = "Hóspede: cria uma solicitação PENDING para si mesmo. "
                    + "Funcionário/administrador (BOOKING_CREATE): cria reserva CONFIRMED para o guestId informado.")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingCreateRequest request) {
        BookingResponse created = bookingService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BOOKING_READ')")
    @Operation(summary = "Lista hospedagens com filtros opcionais",
            description = "Permissão BOOKING_READ. 'from'/'to' retornam hospedagens que se sobrepõem ao período.")
    public PageResponse<BookingResponse> list(
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 20, sort = "checkInDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return bookingService.list(guestId, roomId, status, from, to, pageable);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Hospedagens do hóspede autenticado (CURRENT, PAST ou ALL)", description = "Somente hóspedes.")
    public PageResponse<BookingResponse> myBookings(
            @RequestParam(defaultValue = "ALL") BookingScope scope,
            @ParameterObject @PageableDefault(size = 20, sort = "checkInDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return bookingService.listMine(scope, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOKING_READ') or hasRole('GUEST')")
    @Operation(summary = "Detalhes de uma hospedagem",
            description = "Permissão BOOKING_READ. Hóspedes só acessam as próprias hospedagens (403 caso contrário).")
    public BookingResponse findById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('BOOKING_CONFIRM')")
    @Operation(summary = "Confirma uma solicitação PENDING", description = "Permissão BOOKING_CONFIRM (hóspedes não confirmam).")
    public BookingResponse confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('CHECKIN')")
    @Operation(summary = "Realiza o check-in de uma reserva CONFIRMED", description = "Permissão CHECKIN.")
    public BookingResponse checkIn(@PathVariable Long id) {
        return bookingService.checkIn(id);
    }

    @PatchMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('CHECKOUT')")
    @Operation(summary = "Realiza o check-out de uma hospedagem CHECKED_IN", description = "Permissão CHECKOUT.")
    public BookingResponse checkOut(@PathVariable Long id) {
        return bookingService.checkOut(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('BOOKING_CANCEL') or hasRole('GUEST')")
    @Operation(summary = "Cancela uma hospedagem PENDING ou CONFIRMED",
            description = "Permissão BOOKING_CANCEL. Hóspedes podem cancelar apenas as próprias reservas.")
    public BookingResponse cancel(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) BookingCancelRequest request) {
        return bookingService.cancel(id, request == null ? null : request.reason());
    }
}
