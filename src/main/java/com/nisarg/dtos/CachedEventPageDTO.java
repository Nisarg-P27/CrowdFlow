package com.nisarg.dtos;

import com.nisarg.dtos.responses.EventResponse;

import java.util.List;

public record CachedEventPageDTO(
        List<EventResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
