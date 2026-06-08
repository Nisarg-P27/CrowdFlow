package com.nisarg.services;

import com.nisarg.dtos.responses.PaymentOrderResponse;
import com.nisarg.dtos.responses.PaymentResponse;
import com.nisarg.entities.BookingEntity;
import com.nisarg.entities.PaymentEntity;
import com.nisarg.enums.BookingStatus;
import com.nisarg.enums.PaymentGateway;
import com.nisarg.enums.PaymentStatus;
import com.nisarg.exceptions.ResourceNotFoundException;
import com.nisarg.repositories.BookingRepository;
import com.nisarg.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {


    private final PaymentRepository paymentRepository;

    public PaymentResponse createPayment(BookingEntity booking, BigDecimal amount) {

        log.info("Payment processing started. bookingId={}, amount={}",
                booking.getId(),
                amount);

        PaymentEntity payment = new PaymentEntity();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentStatus(PaymentStatus.CREATED);

        payment = paymentRepository.save(payment);
        booking.setPayment(payment);

        return new PaymentResponse(
                amount,
                payment.getPaymentStatus()
        );
    }

    public void updatePayment(String receipt, String orderId) {

        UUID bookingId = UUID.fromString(receipt);

        log.info("Linking Booking ID: {} with Razorpay Order ID: {}", bookingId, orderId);

        PaymentEntity payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment entity not found for Booking ID: " + bookingId));

        payment.setProvider(PaymentGateway.RAZORPAY);
        payment.setProviderOrderId(orderId);


        // Explicitly save and flush so the data hits the database immediately before the controller returns
        paymentRepository.saveAndFlush(payment);
    }

    public void markPaymentSuccessful(String providerOrderId, String providerPaymentId, String signature){

        log.info("Processing webhook payment capture success for Razorpay Order ID: {}", providerOrderId);

        // 1. Retrieve the existing transaction tracking entry linked during Stage 2
        PaymentEntity payment = paymentRepository.findByProviderOrderId(providerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment tracking record not found for Provider Order ID: " + providerOrderId));

        // 2. Short-circuit if this webhook event was already processed (Idempotency check)
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment reference {} is already marked SUCCESSFUL. Skipping duplicate execution.", providerOrderId);
            return;
        }

        // 3. Update transaction audit data and status state markers
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setProviderPaymentId(providerPaymentId);
        payment.setProviderSignature(signature);
        payment.setPaidAt(LocalDateTime.now());

        // Save changes to the payments table
        paymentRepository.save(payment);
        log.info("Payment table entity updated successfully for ID: {}", payment.getId());

        // 4. Trace the lazy-loaded structural relationship up to the main booking aggregate
        BookingEntity booking = payment.getBooking();
        if (booking == null) {
            throw new ResourceNotFoundException("No associated booking found for payment entry ID: " + payment.getId());
        }

        // 5. Mutate the core domain flow booking state to ALLOW entry confirmation
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        log.info("State engine advanced: Booking ID {} status transitioned to CONFIRMED.", booking.getId());
    }
}
