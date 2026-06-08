package com.nisarg.services;

import com.nisarg.dtos.CachedEventPageDTO;
import com.nisarg.dtos.requests.CreateEventRequest;
import com.nisarg.dtos.requests.UpdateEventRequest;
import com.nisarg.dtos.responses.EventResponse;
import com.nisarg.entities.EventEntity;
import com.nisarg.entities.UserEntity;
import com.nisarg.exceptions.ResourceNotFoundException;
import com.nisarg.infrastructure.RedisEventCacheStore;
import com.nisarg.repositories.EventRepository;
import com.nisarg.repositories.UserRepository;
import com.nisarg.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RedisEventCacheStore eventCacheStore;


    public Page<EventResponse> getAllEvents(int page, int size) {

            Optional<CachedEventPageDTO> cachedEventPage = eventCacheStore.getCachedEventPage(page, size);

            if (cachedEventPage.isPresent()) {
                log.info("Event Page cache hit, page = {}, size = {}", page, size);
                return new PageImpl<>(
                        cachedEventPage.get().events(),
                        PageRequest.of(page, size),
                        cachedEventPage.get().totalElements()
                );
            }
        log.info("Event Page cache miss, page = {}, size = {}", page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<EventResponse> eventPage= eventRepository.findAll(pageable)
                .map(this::mapToResponse);

        CachedEventPageDTO cachedEventPageDTO = new CachedEventPageDTO(
                eventPage.getContent(),
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages()
        );
        log.info("{}",cachedEventPageDTO);

        eventCacheStore.cacheEventPage(cachedEventPageDTO);
        log.info("Event Page cached, page = {}, size = {}", page, size);
        return eventPage;
    }

    public EventResponse getEventById(UUID eventId) {

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));

        return mapToResponse(event);
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