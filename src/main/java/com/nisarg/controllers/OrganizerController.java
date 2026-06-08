package com.nisarg.controllers;

import com.nisarg.dtos.requests.CreateEventRequest;
import com.nisarg.dtos.requests.UpdateEventRequest;
import com.nisarg.dtos.responses.BookingResponse;
import com.nisarg.dtos.responses.EventResponse;
import com.nisarg.dtos.responses.EventStatsResponse;
import com.nisarg.dtos.responses.OrganizerStatsResponse;
import com.nisarg.services.EventService;
import com.nisarg.services.OrganizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizer/")
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerController {
    private final OrganizerService organizerService;

    @PostMapping("events")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request
    ) {

        return ResponseEntity.ok(
                organizerService.createEvent(request)
        );
    }

    @PutMapping("events/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request
    ) {

        return ResponseEntity.ok(
                organizerService.updateEvent(eventId, request)
        );
    }

    @DeleteMapping("events/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID eventId
    ) {

        organizerService.deleteEvent(eventId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("events")
    public ResponseEntity<Page<EventResponse>> getOrganizerEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        System.out.println("in org controller ");
        return ResponseEntity.ok(
                organizerService.getOrganizerEvents(page, size)
        );
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getOrganizerEventById( @PathVariable("id") UUID id ){
        return ResponseEntity.ok(
                organizerService.getOrganizerEventById(id)
        );
    }

    @GetMapping("events/{id}/bookings")
    public ResponseEntity<Page<BookingResponse>> getEventBookings(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return ResponseEntity.ok(organizerService.getEventBookings(id, page, size));
    }

    @GetMapping("events/{id}/stats")
    public ResponseEntity<EventStatsResponse> getEventStats(@PathVariable UUID id){
        return ResponseEntity.ok(organizerService.getEventStats(id));
    }

    @GetMapping("stats")
    public ResponseEntity<OrganizerStatsResponse> getOrganizerStats(){
        return ResponseEntity.ok(organizerService.getOrganizerStats());
    }


}
