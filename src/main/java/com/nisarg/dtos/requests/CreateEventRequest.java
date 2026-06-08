package com.nisarg.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CreateEventRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 60)
        String title,

        @Size(max = 300)
        String description,

        @NotBlank(message = "Venue is required")
        String venue,

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime eventDate,

        @NotNull(message = "Total seats required")
        @Min(value = 1, message = "Total seats must be at least 1")
        Integer totalSeats,

        @NotNull(message = "Ticket price required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
        BigDecimal ticketPrice
) {
}