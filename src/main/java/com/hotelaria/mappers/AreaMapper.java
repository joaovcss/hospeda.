package com.hotelaria.mappers;

import com.hotelaria.dtos.request.AreaRequest;
import com.hotelaria.dtos.response.AreaResponse;
import com.hotelaria.models.HotelArea;
import org.springframework.stereotype.Component;

@Component
public class AreaMapper {

    public HotelArea toEntity(AreaRequest request) {
        return new HotelArea(
                request.name().trim(),
                request.description(),
                request.location().trim(),
                request.openingTime(),
                request.closingTime());
    }

    public void updateEntity(HotelArea area, AreaRequest request) {
        area.setName(request.name().trim());
        area.setDescription(request.description());
        area.setLocation(request.location().trim());
        area.setOpeningTime(request.openingTime());
        area.setClosingTime(request.closingTime());
    }

    public AreaResponse toResponse(HotelArea area) {
        return new AreaResponse(
                area.getId(),
                area.getName(),
                area.getDescription(),
                area.getLocation(),
                area.getOpeningTime(),
                area.getClosingTime(),
                area.getStatus());
    }
}
