package com.nisarg.repositories;

import com.nisarg.entities.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {


    Optional<PaymentEntity> findByProviderOrderId(String providerOrderId);

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    @Query("""
                SELECT COALESCE(SUM(p.amount), 0)
                FROM PaymentEntity p
                WHERE p.booking.event.id = :eventId
                AND p.paymentStatus = 'SUCCESS'
            """)
    BigDecimal getRevenue(UUID eventId);

    @Query("""
    SELECT COALESCE(SUM(p.amount), 0)
    FROM PaymentEntity p
    WHERE p.booking.event.organizer.id = :organizerId
    AND p.paymentStatus = 'SUCCESS'
""")
    BigDecimal getTotalRevenue(UUID organizerId);
}