package com.nisarg.controllers;

import com.nisarg.dtos.responses.PaymentOrderResponse;
import com.nisarg.services.PaymentService;
import com.nisarg.services.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments/")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    @PostMapping("orders")
    public ResponseEntity<PaymentOrderResponse> createOrder(String receipt, BigDecimal amount){
// receipt being bookingid
        ResponseEntity<PaymentOrderResponse> orderResponse = ResponseEntity.ok(razorpayService.createOrder(receipt, amount));
        if(orderResponse.hasBody()){
        paymentService.updatePayment(receipt, orderResponse.getBody().orderId());
        }
        return orderResponse;
    }
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {

        razorpayService.handleWebhook(
                payload,
                signature
        );

        return ResponseEntity.ok().build();
    }
}
