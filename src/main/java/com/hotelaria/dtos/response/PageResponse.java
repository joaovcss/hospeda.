package com.hotelaria.dtos.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope estável de paginação (evita serializar a implementação interna de Page).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
