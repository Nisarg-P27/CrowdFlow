package com.nisarg.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record UpdateEventRequest(

        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotBlank(message = "Venue is required")
        String venue,

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime eventDate,

        @NotNull(message = "Ticket price required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        BigDecimal ticketPrice
) {
}