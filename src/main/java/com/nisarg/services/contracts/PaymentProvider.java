package com.nisarg.services.contracts;

import com.nisarg.dtos.responses.PaymentOrderResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProvider {

    PaymentOrderResponse createOrder(
            String receipt,  //bookingId
            BigDecimal amount
    );
}
