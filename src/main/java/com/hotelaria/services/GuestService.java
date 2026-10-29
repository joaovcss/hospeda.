package com.hotelaria.services;

import com.hotelaria.dtos.request.GuestCreateRequest;
import com.hotelaria.dtos.request.GuestUpdateRequest;
import com.hotelaria.dtos.response.GuestResponse;
import com.hotelaria.dtos.response.GuestSummaryResponse;
import com.hotelaria.dtos.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface GuestService {

    GuestResponse create(GuestCreateRequest request);

    PageResponse<GuestSummaryResponse> list(String search, Pageable pageable);

    GuestResponse findById(Long id);

    GuestResponse getMyProfile();

    GuestResponse update(Long id, GuestUpdateRequest request);
}
