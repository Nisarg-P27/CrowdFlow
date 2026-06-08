package com.nisarg.dtos.responses;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrganizerStatsResponse(
        long totalEvents,
        long totalBookings,
        long totalTicketsSold,
        BigDecimal totalRevenue
) {
}
