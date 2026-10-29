package com.hotelaria.services.impl;

import com.hotelaria.dtos.request.AreaRequest;
import com.hotelaria.dtos.response.AreaResponse;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.exceptions.BusinessRuleException;
import com.hotelaria.exceptions.DuplicateResourceException;
import com.hotelaria.exceptions.ResourceNotFoundException;
import com.hotelaria.mappers.AreaMapper;
import com.hotelaria.models.HotelArea;
import com.hotelaria.models.enums.AreaStatus;
import com.hotelaria.repositories.HotelAreaRepository;
import com.hotelaria.services.AreaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AreaServiceImpl implements AreaService {

    private final HotelAreaRepository areaRepository;
    private final AreaMapper areaMapper;

    public AreaServiceImpl(HotelAreaRepository areaRepository, AreaMapper areaMapper) {
        this.areaRepository = areaRepository;
        this.areaMapper = areaMapper;
    }

    @Override
    @Transactional
    public AreaResponse create(AreaRequest request) {
        validateHours(request);
        if (areaRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("Já existe uma área com este nome");
        }
        return areaMapper.toResponse(areaRepository.save(areaMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AreaResponse> list(AreaStatus status, Pageable pageable) {
        Page<HotelArea> page = status == null
                ? areaRepository.findAll(pageable)
                : areaRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(areaMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AreaResponse findById(Long id) {
        return areaMapper.toResponse(findArea(id));
    }

    @Override
    @Transactional
    public AreaResponse update(Long id, AreaRequest request) {
        validateHours(request);
        HotelArea area = findArea(id);
        if (areaRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new DuplicateResourceException("Já existe uma área com este nome");
        }
        areaMapper.updateEntity(area, request);
        return areaMapper.toResponse(area);
    }

    @Override
    @Transactional
    public AreaResponse updateStatus(Long id, AreaStatus status) {
        HotelArea area = findArea(id);
        area.setStatus(status);
        return areaMapper.toResponse(area);
    }

    private void validateHours(AreaRequest request) {
        if ((request.openingTime() == null) != (request.closingTime() == null)) {
            throw new BusinessRuleException("Informe os horários de abertura e fechamento juntos");
        }
        if (request.openingTime() != null && request.openingTime().equals(request.closingTime())) {
            throw new BusinessRuleException("Os horários de abertura e fechamento devem ser diferentes");
        }
    }

    private HotelArea findArea(Long id) {
        return areaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Área", id));
    }
}
