package com.hotelaria.controllers;

import com.hotelaria.dtos.request.UserCreateRequest;
import com.hotelaria.dtos.request.UserPermissionsRequest;
import com.hotelaria.dtos.request.UserStatusRequest;
import com.hotelaria.dtos.request.UserUpdateRequest;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.dtos.response.PermissionResponse;
import com.hotelaria.dtos.response.UserResponse;
import com.hotelaria.models.enums.UserType;
import com.hotelaria.services.UserService;
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
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Usuários", description = "Gestão de administradores e funcionários e de suas permissões")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Cadastra administrador ou funcionário",
            description = "Permissão USER_CREATE. Funcionários com USER_CREATE só criam funcionários, "
                    + "sem permissões extras; somente administradores criam administradores.")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Lista usuários", description = "Permissão USER_READ.")
    public PageResponse<UserResponse> list(@RequestParam(required = false) UserType userType,
                                           @RequestParam(required = false) Boolean active,
                                           @ParameterObject @PageableDefault(size = 20, sort = "fullName")
                                           Pageable pageable) {
        return userService.list(userType, active, pageable);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Consulta um usuário", description = "Permissão USER_READ.")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Atualiza dados de um usuário",
            description = "Permissão USER_UPDATE. Somente administradores alteram administradores ou o tipo do usuário.")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_STATUS_UPDATE')")
    @Operation(summary = "Ativa ou desativa um usuário", description = "Exclusivo do administrador.")
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return userService.updateStatus(id, request.active());
    }

    @PutMapping("/users/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERMISSION_MANAGE')")
    @Operation(summary = "Define as permissões extras de um funcionário (ex.: USER_CREATE)",
            description = "Exclusivo do administrador. Substitui o conjunto atual; envie [] para revogar todas.")
    public UserResponse updatePermissions(@PathVariable Long id,
                                          @Valid @RequestBody UserPermissionsRequest request) {
        return userService.updatePermissions(id, request.permissions());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista as permissões do sistema e quais podem ser concedidas a funcionários",
            description = "Exclusivo do administrador.")
    public List<PermissionResponse> listPermissions() {
        return userService.listPermissions();
    }
}
