package com.nisarg.services;

import com.nisarg.dtos.requests.CreateEventRequest;
import com.nisarg.dtos.requests.UpdateEventRequest;
import com.nisarg.dtos.responses.BookingResponse;
import com.nisarg.dtos.responses.EventResponse;
import com.nisarg.dtos.responses.EventStatsResponse;
import com.nisarg.dtos.responses.OrganizerStatsResponse;
import com.nisarg.entities.EventEntity;
import com.nisarg.entities.UserEntity;
import com.nisarg.exceptions.ResourceNotFoundException;
import com.nisarg.infrastructure.RedisEventCacheStore;
import com.nisarg.repositories.BookingRepository;
import com.nisarg.repositories.EventRepository;
import com.nisarg.repositories.PaymentRepository;
import com.nisarg.repositories.UserRepository;
import com.nisarg.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final RedisEventCacheStore eventCacheStore;

    public EventResponse createEvent(CreateEventRequest request) {

        UserEntity organizerId = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EventEntity event = new EventEntity();

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats());
        event.setTicketPrice(request.ticketPrice());
        event.setOrganizer(organizerId);

        EventEntity savedEvent = eventRepository.save(event);

        log.info("Event created. eventId={}, title={}, venue={}",
                savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getVenue());
        eventCacheStore.deleteAllEventPage();

        return mapToResponse(savedEvent);
    }

    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request) {

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));

        UUID organizerId = SecurityUtils.getCurrentUserId();

        validateOwnership(event, organizerId);

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTicketPrice(request.ticketPrice());

        EventEntity updatedEvent = eventRepository.save(event);

        log.info("Event updated. eventId={}", event.getId());
        eventCacheStore.deleteAllEventPage();
        return mapToResponse(updatedEvent);
    }

    public void deleteEvent(UUID eventId) {

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        UUID organizerId = SecurityUtils.getCurrentUserId();

        validateOwnership(event, organizerId);
        eventRepository.delete(event);
        log.info("Event deleted. eventId={}", eventId);
        eventCacheStore.deleteAllEventPage();
    }

    public Page<EventResponse> getOrganizerEvents(int page, int size) {


        UUID organizerId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        log.info("{}", pageable.getPageNumber());
        Page<EventResponse> eventPage = eventRepository.findByOrganizerId(organizerId, pageable)
                .map(this::mapToResponse);
        log.info("{}",eventPage.getContent());
        return eventPage;
    }

    public EventResponse getOrganizerEventById(UUID eventId) {

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));
        return mapToResponse(event);
    }

    public Page<BookingResponse> getEventBookings(UUID eventId, int page, int size) {

        UUID organizerId = SecurityUtils.getCurrentUserId();

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        validateOwnership(event, organizerId);
        Pageable pageable = PageRequest.of(page, size);

        Page<BookingResponse> bookingResponsePage = bookingRepository.findByEventId(eventId, pageable)
                .map(bookingService::mapToResponse);
        return bookingResponsePage;
    }

    public EventStatsResponse getEventStats(UUID eventId) {

        UUID organiserId = SecurityUtils.getCurrentUserId();
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        validateOwnership(event, organiserId);

        long bookingCount = bookingRepository.countByEventId(eventId);
        long ticketsSold = bookingRepository.getTicketsSoldByEventId(eventId);
        BigDecimal revenue = paymentRepository.getRevenue(eventId);

        EventStatsResponse eventStatsResponse =
                EventStatsResponse.builder()
                        .eventId(eventId)
                        .bookingCount(bookingCount)
                        .ticketsSold(ticketsSold)
                        .ticketsRemaining(event.getAvailableSeats())
                        .revenue(revenue)
                        .build();
        log.info("{}", eventStatsResponse);
        return eventStatsResponse;
    }

    public OrganizerStatsResponse getOrganizerStats(){

        UUID organizerId = SecurityUtils.getCurrentUserId();

        long totalEvents = eventRepository.countByOrganizerId(organizerId);
        long totalBookings = bookingRepository.getTotalBookings(organizerId);
        long totalTicketsSold = bookingRepository.getTotalTicketsSold(organizerId);
        BigDecimal totalRevenue = paymentRepository.getTotalRevenue(organizerId);

        OrganizerStatsResponse organizerStatsResponse =
                OrganizerStatsResponse.builder()
                        .totalEvents(totalEvents)
                        .totalBookings(totalBookings)
                        .totalTicketsSold(totalTicketsSold)
                        .totalRevenue(totalRevenue)
                        .build();

        return organizerStatsResponse;
    }

    private void validateOwnership(EventEntity event, UUID organizerId) {

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new AccessDeniedException(
                    "You do not own this event"
            );
        }
    }

    private EventResponse mapToResponse(EventEntity event) {

        return new EventResponse(
                event.getId(),
                event.getOrganizer().getName(),
                event.getTitle(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getTicketPrice(),
                event.getCreatedAt()
        );
    }
}