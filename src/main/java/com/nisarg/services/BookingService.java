package com.nisarg.services;

import com.nisarg.dtos.requests.CreateBookingRequest;
import com.nisarg.dtos.responses.BookingResponse;
import com.nisarg.dtos.responses.PaymentResponse;
import com.nisarg.entities.BookingEntity;
import com.nisarg.entities.EventEntity;
import com.nisarg.entities.UserEntity;
import com.nisarg.enums.BookingStatus;
import com.nisarg.enums.PaymentStatus;
import com.nisarg.exceptions.ConflictException;
import com.nisarg.exceptions.ResourceNotFoundException;
import com.nisarg.repositories.BookingRepository;
import com.nisarg.repositories.EventRepository;
import com.nisarg.repositories.UserRepository;
import com.nisarg.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EventEntity event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        log.info("Booking request received. userId={}, eventId={}, seatCount={}",
                userId,
                request.eventId(),
                request.seatCount());

        if (event.getAvailableSeats() < request.seatCount()) {
            log.warn("Booking failed due to insufficient seats. userId={}, eventId={}, requested={}, available={}",
                    userId,
                    event.getId(),
                    request.seatCount(),
                    event.getAvailableSeats());
            throw new ConflictException("Not enough seats available");
        }
        try {
            event.setAvailableSeats(event.getAvailableSeats() - request.seatCount());

            BookingEntity booking = new BookingEntity();

            booking.setUser(user);
            booking.setEvent(event);
            booking.setSeatCount(request.seatCount());
            booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);

            BigDecimal totalAmount = event.getTicketPrice()
                    .multiply(BigDecimal.valueOf(request.seatCount()));

            booking.setTotalAmount(totalAmount);
            booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

            booking = bookingRepository.save(booking);

            PaymentResponse paymentResponse = paymentService.createPayment(booking, totalAmount);

            // Force Hibernate flush so optimistic locking conflicts surface inside this method.
            bookingRepository.flush();
            return mapToResponse(booking);
        } catch (OptimisticLockingFailureException e) {

            log.warn("Booking conflict due to concurrent seat update. userId={}, eventId={}",
                    userId,
                    request.eventId());

            throw new ConflictException("Seat availability changed. Please refresh the site.");
        }
    }

    public List<BookingResponse> getMyBookings() {

        UUID userId = SecurityUtils.getCurrentUserId();

        return bookingRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void cancelBooking(UUID bookingId) {

        UUID userId = SecurityUtils.getCurrentUserId();

        BookingEntity booking = bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {

            log.warn("Duplicate cancellation attempt. bookingId={}, userId={}",
                    bookingId,
                    userId);

            throw new ConflictException("Booking already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);

        log.info("Booking cancelled. bookingId={}, userId={}",
                bookingId,
                userId);

        EventEntity event = booking.getEvent();
        event.setAvailableSeats(
                event.getAvailableSeats() + booking.getSeatCount()
        );
    }

    public BookingResponse mapToResponse(BookingEntity booking) {


        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getEvent().getTitle(),
                booking.getSeatCount(),
                booking.getBookingStatus(),
                booking.getTotalAmount(),
                booking.getBookedAt()
        );
    }
}