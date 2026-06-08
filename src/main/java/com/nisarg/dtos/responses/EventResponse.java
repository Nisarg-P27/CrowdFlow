package com.nisarg.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(

        UUID id,
        String organizerName,
        String title,
        String description,
        String venue,
        LocalDateTime eventDate,
        Integer totalSeats,
        Integer availableSeats,
        BigDecimal ticketPrice,
        LocalDateTime createdAt

) {
}