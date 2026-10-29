package com.hotelaria.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Fluxos críticos ponta a ponta contra um PostgreSQL real (migrations Flyway, constraints,
 * locks e segurança JWT). É ignorado automaticamente quando o Docker não está disponível.
 */
@SpringBootTest(properties = {
        "app.security.jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=",
        "app.bootstrap.admin.enabled=true",
        "app.bootstrap.admin.email=" + HotelFlowIntegrationTest.ADMIN_EMAIL,
        "app.bootstrap.admin.password=" + HotelFlowIntegrationTest.ADMIN_PASSWORD,
        "app.hotel.time-zone=America/Sao_Paulo"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class HotelFlowIntegrationTest {

    static final String ADMIN_EMAIL = "admin@hotel.test";
    static final String ADMIN_PASSWORD = "Admin#Senha2026";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Test
    @DisplayName("Fluxo completo: cadastro, reserva, confirmação, check-in, acesso às áreas e check-out")
    void fullHotelFlow() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        // Cadastros administrativos
        long roomId = createRoom(admin, "R" + suffix.substring(0, 4), 2);
        long areaId = id(call(admin, post("/api/areas"), """
                {"name":"Piscina %s","location":"Térreo"}""".formatted(suffix), 201));
        String guestACpf = "52998224725";
        long guestA = id(call(admin, post("/api/guests"), guestJson("Ana " + suffix, "ana" + suffix + "@mail.com",
                guestACpf), 201));
        call(admin, post("/api/guests"), guestJson("Outra", "outra" + suffix + "@mail.com", guestACpf), 409);
        long guestB = id(call(admin, post("/api/guests"), guestJson("Bruno " + suffix, "bruno" + suffix + "@mail.com",
                "11144477735"), 201));
        long employeeId = id(call(admin, post("/api/users"), """
                {"fullName":"Func %s","email":"func%s@hotel.test","password":"SenhaFunc123","userType":"EMPLOYEE"}"""
                .formatted(suffix, suffix), 201));

        // Login do hóspede por CPF formatado e do funcionário por e-mail
        String guestToken = login("529.982.247-25", "SenhaHospede1");
        String guestBToken = login("bruno" + suffix + "@mail.com", "SenhaHospede1");
        String employee = login("func" + suffix + "@hotel.test", "SenhaFunc123");
        assertThat(json(call(guestToken, get("/api/guests/me"), null, 200)).get("id").asLong()).isEqualTo(guestA);

        // Funcionário sem USER_CREATE recebe 403; após concessão pelo admin, consegue
        String newUser = """
                {"fullName":"Outro","email":"outro%s@hotel.test","password":"SenhaFunc123","userType":"EMPLOYEE"}"""
                .formatted(suffix);
        call(employee, post("/api/users"), newUser, 403);
        call(admin, put("/api/users/" + employeeId + "/permissions"), "{\"permissions\":[\"USER_CREATE\"]}", 200);
        call(employee, post("/api/users"), newUser, 201);
        call(employee, put("/api/users/" + employeeId + "/permissions"), "{\"permissions\":[]}", 403);

        // Hóspede solicita a reserva (PENDING) e não pode confirmá-la
        String bookingBody = bookingJson(null, roomId, today, today.plusDays(2), 2);
        long bookingId = id(call(guestToken, post("/api/bookings"), bookingBody, 201));
        assertThat(status(guestToken, bookingId)).isEqualTo("PENDING");
        call(guestToken, patch("/api/bookings/" + bookingId + "/confirm"), null, 403);

        // Outro hóspede não acessa a reserva
        call(guestBToken, get("/api/bookings/" + bookingId), null, 403);
        call(guestBToken, get("/api/guests/" + guestA), null, 403);

        // Funcionário confirma; reserva conflitante para o mesmo quarto é rejeitada
        call(employee, patch("/api/bookings/" + bookingId + "/confirm"), null, 200);
        call(employee, post("/api/bookings"), bookingJson(guestB, roomId, today.plusDays(1), today.plusDays(3), 1), 409);

        // Disponibilidade considera a reserva confirmada
        JsonNode available = json(call(guestToken, get("/api/rooms/available?checkIn=" + today
                + "&checkOut=" + today.plusDays(1) + "&guests=2"), null, 200));
        available.forEach(room -> assertThat(room.get("id").asLong()).isNotEqualTo(roomId));

        // Check-in ocupa o quarto
        call(employee, patch("/api/bookings/" + bookingId + "/check-in"), null, 200);
        assertThat(json(call(admin, get("/api/rooms/" + roomId), null, 200)).get("status").asText())
                .isEqualTo("OCCUPIED");

        // Acesso à área: negado sem autorização, permitido após concessão, negado após revogação
        String validation = "{\"guestId\":%d,\"areaId\":%d}".formatted(guestA, areaId);
        assertThat(json(call(employee, post("/api/access-logs/validate"), validation, 200))
                .get("denialReason").asText()).isEqualTo("NOT_AUTHORIZED");
        call(employee, post("/api/bookings/" + bookingId + "/areas"), "{\"areaId\":" + areaId + "}", 403);
        call(admin, post("/api/bookings/" + bookingId + "/areas"), "{\"areaId\":" + areaId + "}", 201);
        assertThat(json(call(employee, post("/api/access-logs/validate"), validation, 200))
                .get("result").asText()).isEqualTo("GRANTED");
        assertThat(json(call(guestToken, get("/api/guests/me/areas"), null, 200))).hasSize(1);
        call(admin, delete("/api/bookings/" + bookingId + "/areas/" + areaId), null, 204);
        assertThat(json(call(employee, post("/api/access-logs/validate"), validation, 200))
                .get("denialReason").asText()).isEqualTo("AUTHORIZATION_REVOKED");

        // Histórico: o hóspede vê apenas os próprios registros; funcionário sem ACCESS_LOG_READ não lista
        assertThat(json(call(guestToken, get("/api/access-logs/me"), null, 200)).get("totalElements").asLong())
                .isEqualTo(3);
        call(employee, get("/api/access-logs"), null, 403);
        assertThat(json(call(admin, get("/api/access-logs?guestId=" + guestA + "&result=GRANTED"), null, 200))
                .get("totalElements").asLong()).isEqualTo(1);

        // Check-out libera o quarto; não pode ser repetido nem cancelado
        call(employee, patch("/api/bookings/" + bookingId + "/check-out"), null, 200);
        call(employee, patch("/api/bookings/" + bookingId + "/check-out"), null, 409);
        call(employee, patch("/api/bookings/" + bookingId + "/cancel"), null, 409);
        assertThat(json(call(admin, get("/api/rooms/" + roomId), null, 200)).get("status").asText())
                .isEqualTo("AVAILABLE");
        assertThat(json(call(guestToken, get("/api/bookings/my-bookings?scope=PAST"), null, 200))
                .get("totalElements").asLong()).isEqualTo(1);

        // Usuário desativado perde o acesso imediatamente, mesmo com token válido
        call(admin, patch("/api/users/" + employeeId + "/status"), "{\"active\":false}", 200);
        call(employee, get("/api/auth/me"), null, 401);
    }

    @Test
    @DisplayName("Login inválido não revela se o usuário existe")
    void invalidLoginIsGeneric() throws Exception {
        MvcResult unknown = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"naoexiste@hotel.test\",\"password\":\"qualquer123\"}")).andReturn();
        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + ADMIN_EMAIL + "\",\"password\":\"senhaErrada123\"}")).andReturn();

        assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(unknown).get("message").asText()).isEqualTo(json(wrongPassword).get("message").asText());
    }

    @Test
    @DisplayName("Reservas simultâneas para o mesmo quarto e período: apenas uma é confirmada")
    void concurrentBookingsDoNotOverlap() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        long roomId = createRoom(admin, "C" + suffix.substring(0, 4), 2);
        long guestId = id(call(admin, post("/api/guests"), guestJson("Carla " + suffix,
                "carla" + suffix + "@mail.com", "39053344705"), 201));
        String body = bookingJson(guestId, roomId, today.plusDays(10), today.plusDays(12), 1);

        int attempts = 6;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                tasks.add(() -> {
                    int status = mockMvc.perform(authorized(admin, post("/api/bookings")).content(body))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) {
                        created.incrementAndGet();
                    } else if (status == 409) {
                        conflicts.incrementAndGet();
                    }
                    return null;
                });
            }
            for (Future<Void> future : executor.invokeAll(tasks)) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        assertThat(created.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(attempts - 1);
    }

    // ---------- utilitários ----------

    private long createRoom(String token, String number, int capacity) throws Exception {
        return id(call(token, post("/api/rooms"), """
                {"number":"%s","floor":1,"capacity":%d,"category":"STANDARD","dailyRate":300.00}"""
                .formatted(number, capacity), 201));
    }

    private String guestJson(String name, String email, String cpf) {
        return """
                {"fullName":"%s","email":"%s","cpf":"%s","phone":"11 98888-7777","password":"SenhaHospede1"}"""
                .formatted(name, email, cpf);
    }

    private String bookingJson(Long guestId, long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
        return """
                {"guestId":%s,"roomId":%d,"checkInDate":"%s","checkOutDate":"%s","numberOfGuests":%d}"""
                .formatted(guestId, roomId, checkIn, checkOut, guests);
    }

    private String login(String login, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"password\":\"%s\"}".formatted(login, password))).andReturn();
        assertThat(result.getResponse().getStatus()).as("login de " + login).isEqualTo(200);
        return json(result).get("accessToken").asText();
    }

    private String status(String token, long bookingId) throws Exception {
        return json(call(token, get("/api/bookings/" + bookingId), null, 200)).get("status").asText();
    }

    private MockHttpServletRequestBuilder authorized(String token, MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private MvcResult call(String token, MockHttpServletRequestBuilder builder, String body, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder request = authorized(token, builder);
        if (body != null) {
            request.content(body);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus())
                .as(result.getRequest().getMethod() + " " + result.getRequest().getRequestURI()
                        + " -> " + result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        return result;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long id(MvcResult result) throws Exception {
        return json(result).get("id").asLong();
    }
}
