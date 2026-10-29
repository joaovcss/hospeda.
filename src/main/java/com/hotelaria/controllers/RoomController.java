package com.hotelaria.controllers;

import com.hotelaria.dtos.request.RoomRequest;
import com.hotelaria.dtos.request.RoomStatusRequest;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.dtos.response.RoomResponse;
import com.hotelaria.models.enums.RoomStatus;
import com.hotelaria.services.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Quartos", description = "Cadastro de quartos e consulta de disponibilidade")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ROOM_CREATE')")
    @Operation(summary = "Cadastra um quarto", description = "Exclusivo do administrador.")
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        RoomResponse created = roomService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROOM_READ')")
    @Operation(summary = "Lista quartos, opcionalmente por status (ex.: OCCUPIED, AVAILABLE)",
            description = "Permissão ROOM_READ (administradores e funcionários).")
    public PageResponse<RoomResponse> list(@RequestParam(required = false) RoomStatus status,
                                           @ParameterObject @PageableDefault(size = 20, sort = "number")
                                           Pageable pageable) {
        return roomService.list(status, pageable);
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('ROOM_READ') or hasRole('GUEST')")
    @Operation(summary = "Quartos disponíveis no período [checkIn, checkOut) para a quantidade de hóspedes",
            description = "Permissão ROOM_READ ou hóspede autenticado. Considera as datas das hospedagens "
                    + "confirmadas/em andamento, não apenas o status atual do quarto.")
    public List<RoomResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests) {
        return roomService.findAvailable(checkIn, checkOut, guests);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_READ')")
    @Operation(summary = "Consulta um quarto", description = "Permissão ROOM_READ.")
    public RoomResponse findById(@PathVariable Long id) {
        return roomService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ROOM_UPDATE')")
    @Operation(summary = "Atualiza um quarto", description = "Exclusivo do administrador.")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ROOM_UPDATE')")
    @Operation(summary = "Altera o status do quarto (AVAILABLE, MAINTENANCE, INACTIVE)",
            description = "Exclusivo do administrador.")
    public RoomResponse updateStatus(@PathVariable Long id, @Valid @RequestBody RoomStatusRequest request) {
        return roomService.updateStatus(id, request.status());
    }
}
