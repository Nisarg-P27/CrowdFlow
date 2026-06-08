package com.nisarg.controllers;

import com.nisarg.dtos.requests.CreateEventRequest;
import com.nisarg.dtos.requests.UpdateEventRequest;
import com.nisarg.dtos.responses.EventResponse;
import com.nisarg.services.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;



    @GetMapping
    public ResponseEntity<Page<EventResponse>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(
                eventService.getAllEvents(page, size)
        );
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable UUID eventId
    ) {

        return ResponseEntity.ok(
                eventService.getEventById(eventId)
        );
    }

}