package com.nisarg.repositories;


import com.nisarg.entities.BookingEntity;
import com.nisarg.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    List<BookingEntity> findByUserId(UUID userId);

    Page<BookingEntity> findByEventId(UUID eventId, Pageable pageable);

    Optional<BookingEntity> findByIdAndUserId(UUID bookingId, UUID userId);

    List<BookingEntity> findByBookingStatus(BookingStatus bookingStatus);

    long countByEventId(UUID eventId);



    @Query("""
                SELECT COALESCE(SUM(b.seatCount), 0)
                FROM BookingEntity b
                WHERE b.event.id = :eventId
                AND b.bookingStatus = 'CONFIRMED'
            """)
    long getTicketsSoldByEventId(UUID eventId);

    @Query("""
    SELECT COUNT(b)
    FROM BookingEntity b
    WHERE b.event.organizer.id = :organizerId
    AND b.bookingStatus = 'CONFIRMED'
""")
    long getTotalBookings(UUID organizerId);

    @Query("""
    SELECT COALESCE(SUM(b.seatCount), 0)
    FROM BookingEntity b
    WHERE b.event.organizer.id = :organizerId
    AND b.bookingStatus = 'CONFIRMED'
""")
    long getTotalTicketsSold(UUID organizerId);


}
