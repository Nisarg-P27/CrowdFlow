package com.nisarg.dtos.responses;

import com.nisarg.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID bookingId,
        UUID eventId,
        String eventTitle,
        Integer seatCount,
        BookingStatus bookingStatus,
        BigDecimal totalAmount,
        LocalDateTime bookedAt
) {
}