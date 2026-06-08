package com.nisarg.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateBookingRequest(

        @NotNull(message = "Event ID is required")
        UUID eventId,

        @NotNull(message = "Seat count is required")
        @Min(value = 1, message = "Seat count must be at least 1")
        Integer seatCount
) {
}