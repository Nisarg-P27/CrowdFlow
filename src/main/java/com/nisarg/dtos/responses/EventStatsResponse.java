package com.nisarg.dtos.responses;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record EventStatsResponse(
        UUID eventId,
        long bookingCount,
        long ticketsSold,
        int ticketsRemaining,
        BigDecimal revenue
) {
}
