package com.nisarg.dtos.responses;

import com.nisarg.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentResponse(
        BigDecimal amount,
        PaymentStatus status
) {
}