package com.nisarg.dtos.responses;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentOrderResponse(
        String orderId,
        String key,
        BigDecimal amount,
        String currency
) {
}
