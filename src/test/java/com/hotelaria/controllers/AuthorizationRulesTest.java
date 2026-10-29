package com.hotelaria.controllers;

import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.dtos.response.RoomResponse;
import com.hotelaria.models.enums.RoomCategory;
import com.hotelaria.models.enums.RoomStatus;
import com.hotelaria.dtos.response.UserResponse;
import com.hotelaria.exceptions.ForbiddenOperationException;
import com.hotelaria.models.enums.UserType;
import com.hotelaria.security.AuthenticatedUserLoader;
import com.hotelaria.security.JwtService;
import com.hotelaria.security.SecurityConfig;
import com.hotelaria.security.SecurityErrorWriter;
import com.hotelaria.services.AccessLogService;
import com.hotelaria.services.AreaAccessService;
import com.hotelaria.services.AreaService;
import com.hotelaria.services.AuthService;
import com.hotelaria.services.BookingService;
import com.hotelaria.services.GuestService;
import com.hotelaria.services.RoomService;
import com.hotelaria.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica as regras de autorização dos endpoints (papéis x permissões) com a
 * configuração real do Spring Security e services simulados.
 */
@WebMvcTest
@Import({SecurityConfig.class, SecurityErrorWriter.class})
class AuthorizationRulesTest {

    private static final String NEW_EMPLOYEE_JSON = """
            {"fullName":"Novo Funcionário","email":"novo@hotel.com","password":"SenhaForte123","userType":"EMPLOYEE"}
            """;
    private static final String ROOM_JSON = """
            {"number":"201","floor":2,"capacity":2,"category":"STANDARD","dailyRate":250.00}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private AuthenticatedUserLoader authenticatedUserLoader;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private GuestService guestService;
    @MockitoBean
    private RoomService roomService;
    @MockitoBean
    private AreaService areaService;
    @MockitoBean
    private BookingService bookingService;
    @MockitoBean
    private AreaAccessService areaAccessService;
    @MockitoBean
    private AccessLogService accessLogService;

    @Test
    @DisplayName("Requisição sem token recebe 401 no formato padronizado")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/rooms"));
    }

    @Test
    @DisplayName("Funcionário sem USER_CREATE recebe 403 ao cadastrar usuário")
    @WithMockUser(authorities = {"ROLE_EMPLOYEE", "GUEST_CREATE", "BOOKING_CREATE"})
    void employeeWithoutUserCreateIsForbidden() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(NEW_EMPLOYEE_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(userService, never()).create(any());
    }

    @Test
    @DisplayName("Funcionário com USER_CREATE consegue cadastrar usuário")
    @WithMockUser(authorities = {"ROLE_EMPLOYEE", "USER_CREATE"})
    void employeeWithUserCreateIsAllowed() throws Exception {
        when(userService.create(any())).thenReturn(new UserResponse(9L, "Novo Funcionário", "novo@hotel.com",
                UserType.EMPLOYEE, true, Set.of(), Set.of(), null, null));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(NEW_EMPLOYEE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    @DisplayName("Permissão operacional não concede acesso administrativo: ROOM_CREATE sem ROLE_ADMIN é negado")
    @WithMockUser(authorities = {"ROLE_EMPLOYEE", "ROOM_CREATE"})
    void permissionWithoutAdminRoleIsNotEnough() throws Exception {
        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isForbidden());
        verify(roomService, never()).create(any());
    }

    @Test
    @DisplayName("Administrador cadastra quartos")
    @WithMockUser(authorities = {"ROLE_ADMIN", "ROOM_CREATE"})
    void adminCreatesRoom() throws Exception {
        when(roomService.create(any())).thenReturn(new RoomResponse(3L, "201", 2, 2, RoomCategory.STANDARD,
                new BigDecimal("250.00"), RoomStatus.AVAILABLE, null));

        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Hóspede não acessa funcionalidades administrativas")
    @WithMockUser(authorities = {"ROLE_GUEST", "AREA_READ"})
    void guestCannotUseAdministrativeEndpoints() throws Exception {
        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/guests")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(NEW_EMPLOYEE_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/5/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[\"USER_CREATE\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Hóspede não pode confirmar a própria reserva")
    @WithMockUser(authorities = {"ROLE_GUEST", "AREA_READ"})
    void guestCannotConfirmBooking() throws Exception {
        mockMvc.perform(patch("/api/bookings/1/confirm")).andExpect(status().isForbidden());
        verify(bookingService, never()).confirm(anyLong());
    }

    @Test
    @DisplayName("Hóspede acessa as próprias hospedagens")
    @WithMockUser(authorities = {"ROLE_GUEST", "AREA_READ"})
    void guestListsOwnBookings() throws Exception {
        when(bookingService.listMine(any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/bookings/my-bookings")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Negativa de propriedade do service vira 403 para o hóspede")
    @WithMockUser(authorities = {"ROLE_GUEST", "AREA_READ"})
    void ownershipViolationReturns403() throws Exception {
        when(bookingService.findById(99L))
                .thenThrow(new ForbiddenOperationException("Você não tem acesso a esta hospedagem"));

        mockMvc.perform(get("/api/bookings/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não tem acesso a esta hospedagem"));
    }

    @Test
    @DisplayName("Funcionário não gerencia permissões administrativas")
    @WithMockUser(authorities = {"ROLE_EMPLOYEE", "USER_CREATE", "USER_UPDATE"})
    void employeeCannotManagePermissions() throws Exception {
        mockMvc.perform(put("/api/users/5/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[\"USER_CREATE\"]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/users/5/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Dados inválidos retornam 400 com os erros por campo")
    @WithMockUser(authorities = {"ROLE_EMPLOYEE", "GUEST_CREATE"})
    void invalidPayloadReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/guests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"invalido\",\"cpf\":\"123\",\"phone\":\"x\",\"password\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(5));
    }
}
