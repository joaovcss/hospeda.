package com.hotelaria.services;

import com.hotelaria.dtos.request.AreaRequest;
import com.hotelaria.dtos.response.AreaResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.models.enums.AreaStatus;
import org.springframework.data.domain.Pageable;

public interface AreaService {

    AreaResponse create(AreaRequest request);

    PageResponse<AreaResponse> list(AreaStatus status, Pageable pageable);

    AreaResponse findById(Long id);

    AreaResponse update(Long id, AreaRequest request);

    AreaResponse updateStatus(Long id, AreaStatus status);
}
