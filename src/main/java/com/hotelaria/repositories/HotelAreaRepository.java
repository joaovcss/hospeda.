package com.hotelaria.repositories;

import com.hotelaria.models.HotelArea;
import com.hotelaria.models.enums.AreaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelAreaRepository extends JpaRepository<HotelArea, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<HotelArea> findByStatus(AreaStatus status, Pageable pageable);
}
