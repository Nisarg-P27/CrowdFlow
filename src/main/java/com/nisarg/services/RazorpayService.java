package com.nisarg.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nisarg.configs.RazorpayConfig;
import com.nisarg.dtos.responses.PaymentOrderResponse;
import com.nisarg.entities.PaymentEntity;
import com.nisarg.enums.PaymentStatus;
import com.nisarg.exceptions.BadRequestException;
import com.nisarg.exceptions.ResourceNotFoundException;
import com.nisarg.exceptions.UnauthorizedException;
import com.nisarg.services.contracts.PaymentProvider;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayService implements PaymentProvider {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final RazorpayClient razorpayClient;
    private final RazorpayConfig.RazorpayProperties razorpayProperties;


    @Override
    public PaymentOrderResponse createOrder(
            String receipt,
            BigDecimal amount
    ) {
        try {
            JSONObject options = new JSONObject();

            options.put(
                    "amount",
                    amount.multiply(BigDecimal.valueOf(100)).longValue()
            );

            options.put("currency", "INR");
            options.put("receipt", receipt);

            Order order = razorpayClient.orders.create(options);

            return PaymentOrderResponse
                    .builder()
                    .orderId(order.get("id"))
                    .key(razorpayProperties.keyId())
                    .amount(amount)
                    .currency("INR")
                    .build();

        } catch (RazorpayException exception) {
            throw new RuntimeException(
                    "Failed to create Razorpay order",
                    exception
            );
        }
    }

    public void handleWebhook(
            String payload,
            String signature
    ) {
        verifySignature(payload, signature);

        try {
            JsonNode root = objectMapper.readTree(payload);

            String event = root.path("event").asText();

            switch (event) {
                case "payment.captured" ->
                        handlePaymentCaptured(root, signature);

                default ->
                        log.info("Ignoring unsupported Razorpay event: {}", event);
            }

        } catch (JsonProcessingException exception) {
            throw new BadRequestException(
                    "Invalid webhook payload"
            );
        }
    }

    private void handlePaymentCaptured(
            JsonNode root,
            String signature
    ) {

        JsonNode paymentNode = root
                .path("payload")
                .path("payment")
                .path("entity");

        String providerOrderId =
                paymentNode.path("order_id").asText();

        String providerPaymentId =
                paymentNode.path("id").asText();

        paymentService.markPaymentSuccessful(
                providerOrderId,
                providerPaymentId,
                signature
        );
    }

    private void verifySignature(
            String payload,
            String signature
    ) {
        try {
            Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    razorpayProperties.webhookSecret()
            );
        } catch (RazorpayException exception) {
            throw new UnauthorizedException(
                    "Invalid Razorpay webhook signature"
            );
        }
    }

}
