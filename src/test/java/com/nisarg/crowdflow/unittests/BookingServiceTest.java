package com.nisarg.crowdflow.unittests;

import com.nisarg.dtos.requests.CreateBookingRequest;
import com.nisarg.dtos.responses.BookingResponse;
import com.nisarg.dtos.responses.PaymentResponse;
import com.nisarg.entities.BookingEntity;
import com.nisarg.entities.EventEntity;
import com.nisarg.entities.PaymentEntity;
import com.nisarg.entities.UserEntity;
import com.nisarg.enums.BookingStatus;
import com.nisarg.enums.PaymentStatus;
import com.nisarg.exceptions.ConflictException;
import com.nisarg.exceptions.ResourceNotFoundException;
import com.nisarg.repositories.BookingRepository;
import com.nisarg.repositories.EventRepository;
import com.nisarg.repositories.UserRepository;
import com.nisarg.security.SecurityUtils;
import com.nisarg.services.BookingService;
import com.nisarg.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private UUID currentUserId;
    private UUID eventId;
    private UUID bookingId;

    private CreateBookingRequest createBookingRequest;

    private UserEntity selectedUser;
    private EventEntity selectedEvent;

    private PaymentResponse paymentResSuccess;
    private PaymentResponse paymentResFail;

    private PaymentEntity paymentSuccess;
    private PaymentEntity paymentFail;

    private BookingEntity savedBooking;
    private BookingEntity existingConfirmedBooking;
    private BookingEntity existingCancelledBooking;

    @BeforeEach
    void setup() {
        currentUserId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        createBookingRequest = CreateBookingRequest.builder()
                .eventId(eventId)
                .seatCount(3)
                .build();

        selectedUser = new UserEntity();
        selectedUser.setId(currentUserId);

        selectedEvent = new EventEntity();
        selectedEvent.setId(eventId);
        selectedEvent.setTitle("Rolling Loud 2026");
        selectedEvent.setTotalSeats(1800);
        selectedEvent.setAvailableSeats(100);
        selectedEvent.setTicketPrice(BigDecimal.valueOf(999));

        paymentResSuccess = PaymentResponse.builder()
                .status(PaymentStatus.SUCCESS)
                .transactionReference("txn-success-123")
                .build();

        paymentResFail = PaymentResponse.builder()
                .status(PaymentStatus.FAILED)
                .transactionReference("txn-fail-456")
                .build();

        paymentSuccess = new PaymentEntity();
        paymentSuccess.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentSuccess.setTransactionReference("txn-success-123");

        paymentFail = new PaymentEntity();
        paymentFail.setPaymentStatus(PaymentStatus.FAILED);
        paymentFail.setTransactionReference("txn-fail-456");

        savedBooking = new BookingEntity();
        savedBooking.setId(bookingId);
        savedBooking.setUser(selectedUser);
        savedBooking.setEvent(selectedEvent);
        savedBooking.setSeatCount(3);
        savedBooking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        savedBooking.setTotalAmount(BigDecimal.valueOf(2997));

        existingConfirmedBooking = new BookingEntity();
        existingConfirmedBooking.setId(UUID.randomUUID());
        existingConfirmedBooking.setUser(selectedUser);
        existingConfirmedBooking.setEvent(selectedEvent);
        existingConfirmedBooking.setSeatCount(2);
        existingConfirmedBooking.setBookingStatus(BookingStatus.CONFIRMED);
        existingConfirmedBooking.setPayment(paymentSuccess);
        existingConfirmedBooking.setTotalAmount(BigDecimal.valueOf(1998));
        existingConfirmedBooking.setBookedAt(LocalDateTime.now());

        existingCancelledBooking = new BookingEntity();
        existingCancelledBooking.setId(UUID.randomUUID());
        existingCancelledBooking.setUser(selectedUser);
        existingCancelledBooking.setEvent(selectedEvent);
        existingCancelledBooking.setSeatCount(2);
        existingCancelledBooking.setBookingStatus(BookingStatus.CANCELLED);
        existingCancelledBooking.setPayment(paymentFail);
        existingCancelledBooking.setTotalAmount(BigDecimal.valueOf(1998));
    }

    @Test
    @DisplayName("createBooking -> should return confirmed booking response when payment succeeds")
    void createBooking_shouldReturnConfirmedBooking_WhenPaymentSucceeds() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(selectedUser));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(selectedEvent));
            when(bookingRepository.save(any(BookingEntity.class))).thenReturn(savedBooking);
            when(paymentService.processPayment(any(BookingEntity.class), any(BigDecimal.class)))
                    .thenAnswer(invocation -> {
                        BookingEntity booking = invocation.getArgument(0);
                        booking.setPayment(paymentSuccess);
                        return paymentResSuccess;
                    });

            BookingResponse response = bookingService.createBooking(createBookingRequest);

            assertSoftly(softly -> {
                softly.assertThat(response).isNotNull();
                softly.assertThat(response.bookingId()).isEqualTo(savedBooking.getId());
                softly.assertThat(response.eventId()).isEqualTo(eventId);
                softly.assertThat(response.eventTitle()).isEqualTo("Rolling Loud 2026");
                softly.assertThat(response.seatCount()).isEqualTo(3);
                softly.assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
                softly.assertThat(response.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
                softly.assertThat(response.totalAmount()).isEqualTo(BigDecimal.valueOf(2997));
                softly.assertThat(response.bookedAt()).isNotNull();
                softly.assertThat(selectedEvent.getAvailableSeats()).isEqualTo(97);
            });

            verify(userRepository).findById(currentUserId);
            verify(eventRepository).findById(eventId);
            verify(bookingRepository).save(any(BookingEntity.class));
            verify(paymentService).processPayment(any(BookingEntity.class), eq(BigDecimal.valueOf(2997)));
            verify(bookingRepository).flush();
        }
    }

    @Test
    @DisplayName("createBooking -> should return cancelled booking response when payment fails")
    void createBooking_shouldReturnCancelledBooking_WhenPaymentFails() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(selectedUser));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(selectedEvent));
            when(bookingRepository.save(any(BookingEntity.class))).thenReturn(savedBooking);
            when(paymentService.processPayment(any(BookingEntity.class), any(BigDecimal.class)))
                    .thenAnswer(invocation -> {
                        BookingEntity booking = invocation.getArgument(0);
                        booking.setPayment(paymentFail);
                        return paymentResFail;
                    });

            BookingResponse response = bookingService.createBooking(createBookingRequest);

            assertSoftly(softly -> {
                softly.assertThat(response).isNotNull();
                softly.assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CANCELLED);
                softly.assertThat(response.payment().status()).isEqualTo(PaymentStatus.FAILED);
                softly.assertThat(selectedEvent.getAvailableSeats()).isEqualTo(100);
            });

            verify(bookingRepository).flush();
        }
    }

    @Test
    @DisplayName("createBooking -> should throw when user not found")
    void createBooking_shouldThrow_WhenUserNotFound() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(createBookingRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(eventRepository, never()).findById(any());
            verify(bookingRepository, never()).save(any());
            verify(paymentService, never()).processPayment(any(), any());
        }
    }

    @Test
    @DisplayName("createBooking -> should throw when event not found")
    void createBooking_shouldThrow_WhenEventNotFound() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(selectedUser));
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(createBookingRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Event not found");

            verify(bookingRepository, never()).save(any());
            verify(paymentService, never()).processPayment(any(), any());
        }
    }

    @Test
    @DisplayName("createBooking -> should throw when insufficient seats")
    void createBooking_shouldThrow_WhenSeatsUnavailable() {
        selectedEvent.setAvailableSeats(2);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(selectedUser));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(selectedEvent));

            assertThatThrownBy(() -> bookingService.createBooking(createBookingRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Not enough seats available");

            verify(bookingRepository, never()).save(any());
            verify(paymentService, never()).processPayment(any(), any());
            verify(bookingRepository, never()).flush();
        }
    }

    @Test
    @DisplayName("createBooking -> should throw conflict when optimistic locking fails")
    void createBooking_shouldThrowConflict_WhenOptimisticLockFails() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(selectedUser));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(selectedEvent));
            when(bookingRepository.save(any(BookingEntity.class))).thenReturn(savedBooking);
            when(paymentService.processPayment(any(), any())).thenReturn(paymentResSuccess);
            doThrow(new OptimisticLockingFailureException("conflict"))
                    .when(bookingRepository).flush();

            assertThatThrownBy(() -> bookingService.createBooking(createBookingRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Seat availability changed. Please refresh the site.");

            verify(bookingRepository).flush();
        }
    }

    @Test
    @DisplayName("getMyBookings -> should return mapped bookings")
    void getMyBookings_shouldReturnMappedBookings() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(bookingRepository.findByUserId(currentUserId))
                    .thenReturn(List.of(existingConfirmedBooking));

            List<BookingResponse> responses = bookingService.getMyBookings();

            assertThat(responses).hasSize(1);

            BookingResponse response = responses.get(0);

            assertSoftly(softly -> {
                softly.assertThat(response.bookingId()).isEqualTo(existingConfirmedBooking.getId());
                softly.assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
                softly.assertThat(response.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
                softly.assertThat(response.eventTitle()).isEqualTo("Rolling Loud 2026");
            });

            verify(bookingRepository).findByUserId(currentUserId);
        }
    }

    @Test
    @DisplayName("getMyBookings -> should return empty list when no bookings exist")
    void getMyBookings_shouldReturnEmptyList_WhenNoBookingsExist() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(bookingRepository.findByUserId(currentUserId))
                    .thenReturn(List.of());

            List<BookingResponse> responses = bookingService.getMyBookings();

            assertThat(responses).isEmpty();

            verify(bookingRepository).findByUserId(currentUserId);
        }
    }

    @Test
    @DisplayName("cancelBooking -> should cancel booking and restore seats")
    void cancelBooking_shouldCancelBooking_WhenValidBooking() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(bookingRepository.findByIdAndUserId(existingConfirmedBooking.getId(), currentUserId))
                    .thenReturn(Optional.of(existingConfirmedBooking));

            int originalSeats = selectedEvent.getAvailableSeats();

            bookingService.cancelBooking(existingConfirmedBooking.getId());

            assertSoftly(softly -> {
                softly.assertThat(existingConfirmedBooking.getBookingStatus())
                        .isEqualTo(BookingStatus.CANCELLED);
                softly.assertThat(selectedEvent.getAvailableSeats())
                        .isEqualTo(originalSeats + 2);
            });

            verify(bookingRepository)
                    .findByIdAndUserId(existingConfirmedBooking.getId(), currentUserId);
        }
    }

    @Test
    @DisplayName("cancelBooking -> should throw when booking not found")
    void cancelBooking_shouldThrow_WhenBookingNotFound() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            when(bookingRepository.findByIdAndUserId(bookingId, currentUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Booking not found");
        }
    }

    @Test
    @DisplayName("cancelBooking -> should throw when booking already cancelled")
    void cancelBooking_shouldThrow_WhenAlreadyCancelled() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            int originalSeats = selectedEvent.getAvailableSeats();

            when(bookingRepository.findByIdAndUserId(existingCancelledBooking.getId(), currentUserId))
                    .thenReturn(Optional.of(existingCancelledBooking));

            assertThatThrownBy(() -> bookingService.cancelBooking(existingCancelledBooking.getId()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Booking already cancelled");

            assertThat(selectedEvent.getAvailableSeats()).isEqualTo(originalSeats);
        }
    }
}