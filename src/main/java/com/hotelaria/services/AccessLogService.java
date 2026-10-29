package com.hotelaria.services;

import com.hotelaria.dtos.request.AccessValidationRequest;
import com.hotelaria.dtos.response.AccessLogResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.models.enums.AccessResult;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Validação e registro das tentativas de entrada de hóspedes nas áreas do hotel.
 */
public interface AccessLogService {

    /** Avalia a autorização no momento atual e registra a tentativa (permitida ou negada). */
    AccessLogResponse validateAndRegister(AccessValidationRequest request);

    PageResponse<AccessLogResponse> list(Long guestId, Long areaId, Long bookingId, AccessResult result,
                                         LocalDateTime from, LocalDateTime to, Pageable pageable);

    PageResponse<AccessLogResponse> listMine(Pageable pageable);
}
