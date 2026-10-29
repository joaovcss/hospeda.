-- Necessária para a exclusion constraint que impede reservas sobrepostas
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Hóspedes: nome e e-mail ficam em users (credenciais), evitando duplicação
CREATE TABLE guests (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    cpf                VARCHAR(11)  NOT NULL,
    phone              VARCHAR(20)  NOT NULL,
    birth_date         DATE,
    address_street     VARCHAR(150),
    address_number     VARCHAR(20),
    address_complement VARCHAR(100),
    address_district   VARCHAR(100),
    address_city       VARCHAR(100),
    address_state      VARCHAR(2),
    address_zip_code   VARCHAR(8),
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uk_guests_user UNIQUE (user_id),
    CONSTRAINT uk_guests_cpf UNIQUE (cpf),
    CONSTRAINT fk_guests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_guests_cpf_digits CHECK (cpf ~ '^[0-9]{11}$')
);

CREATE TABLE rooms (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    number      VARCHAR(10)    NOT NULL,
    floor       INTEGER        NOT NULL,
    capacity    INTEGER        NOT NULL,
    category    VARCHAR(20)    NOT NULL,
    daily_rate  NUMERIC(10, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP      NOT NULL,
    CONSTRAINT uk_rooms_number UNIQUE (number),
    CONSTRAINT ck_rooms_capacity CHECK (capacity > 0),
    CONSTRAINT ck_rooms_daily_rate CHECK (daily_rate > 0),
    CONSTRAINT ck_rooms_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'INACTIVE')),
    CONSTRAINT ck_rooms_category CHECK (category IN ('STANDARD', 'SUPERIOR', 'DELUXE', 'SUITE'))
);

CREATE INDEX idx_rooms_status ON rooms (status);

CREATE TABLE hotel_areas (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    location     VARCHAR(150) NOT NULL,
    opening_time TIME,
    closing_time TIME,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT ck_hotel_areas_status CHECK (status IN ('ACTIVE', 'UNAVAILABLE', 'INACTIVE')),
    CONSTRAINT ck_hotel_areas_hours CHECK (
        (opening_time IS NULL AND closing_time IS NULL)
        OR (opening_time IS NOT NULL AND closing_time IS NOT NULL)
    )
);

-- Nome único sem diferenciar maiúsculas/minúsculas ("Piscina" = "piscina")
CREATE UNIQUE INDEX uk_hotel_areas_name ON hotel_areas (LOWER(name));

CREATE TABLE bookings (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    guest_id            BIGINT         NOT NULL,
    room_id             BIGINT         NOT NULL,
    created_by_user_id  BIGINT         NOT NULL,
    check_in_date       DATE           NOT NULL,
    check_out_date      DATE           NOT NULL,
    number_of_guests    INTEGER        NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    total_amount        NUMERIC(12, 2) NOT NULL,
    notes               VARCHAR(500),
    confirmed_at        TIMESTAMP,
    checked_in_at       TIMESTAMP,
    checked_out_at      TIMESTAMP,
    cancelled_at        TIMESTAMP,
    cancellation_reason VARCHAR(255),
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP      NOT NULL,
    updated_at          TIMESTAMP      NOT NULL,
    CONSTRAINT fk_bookings_guest FOREIGN KEY (guest_id) REFERENCES guests (id),
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_bookings_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_bookings_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT ck_bookings_guests CHECK (number_of_guests > 0),
    CONSTRAINT ck_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    -- Última barreira contra requisições simultâneas: duas reservas confirmadas
    -- (ou em andamento) nunca podem ocupar o mesmo quarto em períodos sobrepostos.
    CONSTRAINT ex_bookings_room_period EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in_date, check_out_date, '[)') WITH &&
    ) WHERE (status IN ('CONFIRMED', 'CHECKED_IN'))
);

CREATE INDEX idx_bookings_guest ON bookings (guest_id);
CREATE INDEX idx_bookings_room_dates ON bookings (room_id, check_in_date, check_out_date);
CREATE INDEX idx_bookings_status ON bookings (status);

-- Autorização de acesso a áreas, sempre vinculada a uma hospedagem
CREATE TABLE booking_area_access (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id         BIGINT      NOT NULL,
    area_id            BIGINT      NOT NULL,
    valid_from         TIMESTAMP   NOT NULL,
    valid_until        TIMESTAMP   NOT NULL,
    status             VARCHAR(20) NOT NULL,
    granted_by_user_id BIGINT      NOT NULL,
    revoked_by_user_id BIGINT,
    revoked_at         TIMESTAMP,
    created_at         TIMESTAMP   NOT NULL,
    updated_at         TIMESTAMP   NOT NULL,
    CONSTRAINT uk_booking_area_access UNIQUE (booking_id, area_id),
    CONSTRAINT fk_booking_area_access_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_booking_area_access_area FOREIGN KEY (area_id) REFERENCES hotel_areas (id),
    CONSTRAINT fk_booking_area_access_granted_by FOREIGN KEY (granted_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_booking_area_access_revoked_by FOREIGN KEY (revoked_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_booking_area_access_period CHECK (valid_until > valid_from),
    CONSTRAINT ck_booking_area_access_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_booking_area_access_area ON booking_area_access (area_id);

-- Registro de tentativas de acesso (permitidas e negadas)
CREATE TABLE access_logs (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    guest_id              BIGINT      NOT NULL,
    booking_id            BIGINT,
    area_id               BIGINT      NOT NULL,
    attempted_at          TIMESTAMP   NOT NULL,
    result                VARCHAR(10) NOT NULL,
    denial_reason         VARCHAR(40),
    registered_by_user_id BIGINT,
    CONSTRAINT fk_access_logs_guest FOREIGN KEY (guest_id) REFERENCES guests (id),
    CONSTRAINT fk_access_logs_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_access_logs_area FOREIGN KEY (area_id) REFERENCES hotel_areas (id),
    CONSTRAINT fk_access_logs_registered_by FOREIGN KEY (registered_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_access_logs_result CHECK (result IN ('GRANTED', 'DENIED')),
    CONSTRAINT ck_access_logs_reason CHECK (
        (result = 'GRANTED' AND denial_reason IS NULL)
        OR (result = 'DENIED' AND denial_reason IS NOT NULL)
    )
);

CREATE INDEX idx_access_logs_guest ON access_logs (guest_id, attempted_at DESC);
CREATE INDEX idx_access_logs_area ON access_logs (area_id, attempted_at DESC);
