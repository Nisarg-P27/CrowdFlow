//package com.nisarg.crowdflow.unittests;
//
//import com.nisarg.dtos.requests.CreateEventRequest;
//import com.nisarg.dtos.requests.UpdateEventRequest;
//import com.nisarg.dtos.responses.EventResponse;
//import com.nisarg.entities.EventEntity;
//import com.nisarg.exceptions.ResourceNotFoundException;
//import com.nisarg.repositories.EventRepository;
//import com.nisarg.services.EventService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.assertj.core.api.SoftAssertions.assertSoftly;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class EventServiceTest {
//
//    //MOCKS
//    @Mock
//    private EventRepository eventRepository;
//
//    @InjectMocks
//    private EventService eventService;
//
//    //TEST data
//    private CreateEventRequest createEventRequest;
//    private UpdateEventRequest updateEventRequest;
//    private EventEntity savedEvent;
//    private EventEntity updatedEvent;
//    private List<EventEntity> savedEventsList;
//
//    private final UUID currentEventId =  UUID.randomUUID();
//    @BeforeEach
//    void setup(){
//
//        createEventRequest = CreateEventRequest.builder()
//                .title("Rolling Loud, 2026")
//                .description("Rolling Loud, 2026")
//                .venue("Mahalakshmi Racecourse, Mumbai")
//                .eventDate(LocalDateTime.parse("2026-06-15T10:30:00"))
//                .totalSeats(1800)
//                .ticketPrice(BigDecimal.valueOf(999.00))
//                .build();
//
//        updateEventRequest = UpdateEventRequest.builder()
//                .title("Rolling Loud, 2027")
//                .description("Rolling Loud, 2027")
//                .venue("Mahalakshmi Racecourse, Mumbai")
//                .eventDate(LocalDateTime.parse("2027-06-19T10:00:00"))
//                .ticketPrice(BigDecimal.valueOf(1299.00))
//                .build();
//
//
//        savedEvent = new EventEntity();
//        savedEvent.setId(currentEventId);
//        savedEvent.setTitle("Rolling Loud, 2026");
//        savedEvent.setDescription("Rolling Loud, 2026");
//        savedEvent.setVenue("Mahalakshmi Racecourse, Mumbai");
//        savedEvent.setEventDate(LocalDateTime.parse("2026-06-15T10:30:00"));
//        savedEvent.setTicketPrice(BigDecimal.valueOf(999.00));
//        savedEvent.setTotalSeats(1800);
//        savedEvent.setAvailableSeats(1800);
//        savedEvent.setCreatedAt(LocalDateTime.parse("2026-05-16T11:34:11"));
//
//        updatedEvent = new EventEntity();
//        updatedEvent.setId(savedEvent.getId());
//        updatedEvent.setTitle("Rolling Loud, 2027");
//        updatedEvent.setDescription(savedEvent.getDescription());
//        updatedEvent.setVenue(savedEvent.getVenue());
//        updatedEvent.setEventDate(LocalDateTime.parse("2027-06-19T10:00:00"));
//        updatedEvent.setTotalSeats(savedEvent.getTotalSeats());
//        updatedEvent.setAvailableSeats(savedEvent.getAvailableSeats());
//        updatedEvent.setTicketPrice(BigDecimal.valueOf(1299.00));
//        updatedEvent.setCreatedAt(savedEvent.getCreatedAt());
//
//        savedEventsList = List.of(savedEvent,updatedEvent,savedEvent);
//    }
//    //STUBS
//    @Test
//    @DisplayName("createEvent -> should return Event Response on success")
//    void createEvent_shouldReturnEventResponse_whenRequestIsValid(){
//        //ARRANGE
//        when(eventRepository.save(any(EventEntity.class))).thenReturn(savedEvent);
//        //ACT
//        EventResponse eventResponse = eventService.createEvent(createEventRequest);
//        //ASSERT
//        assertSoftly(softly ->{
//                softly.assertThat(eventResponse).isNotNull();
//                softly.assertThat(eventResponse.id()).isEqualTo(currentEventId);
//                softly.assertThat(eventResponse.title()).isEqualTo("Rolling Loud, 2026");
//                softly.assertThat(eventResponse.description()).isEqualTo("Rolling Loud, 2026");
//                softly.assertThat(eventResponse.venue()).isEqualTo("Mahalakshmi Racecourse, Mumbai");
//                softly.assertThat(eventResponse.eventDate()).isEqualTo(LocalDateTime.parse("2026-06-15T10:30:00"));
//                softly.assertThat(eventResponse.totalSeats()).isEqualTo(1800);
//                softly.assertThat(eventResponse.availableSeats()).isEqualTo(1800);
//                softly.assertThat(eventResponse.ticketPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.00));
//                softly.assertThat(eventResponse.createdAt()).isEqualTo(LocalDateTime.parse("2026-05-16T11:34:11"));
//        });
//    }
//
//    @Test
//    @DisplayName("getAllEvents -> should return list of events on success")
//    void getAllEvents_shouldReturnEventsList_whenRequestIsValid(){
//        //ARRANGE
//        when(eventRepository.findAll()).thenReturn(savedEventsList);
//        //ACT
//        List<EventResponse> eventResponse = eventService.getAllEvents();
//        //ASSERT
//        assertSoftly(softly -> {
//            softly.assertThat(eventResponse).isNotNull();
//            softly.assertThat(eventResponse).hasSize(3);
//
//            softly.assertThat(eventResponse.get(0).id()).isEqualTo(currentEventId);
//            softly.assertThat(eventResponse.get(0).title()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.get(0).description()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.get(0).venue()).isEqualTo("Mahalakshmi Racecourse, Mumbai");
//            softly.assertThat(eventResponse.get(0).eventDate()).isEqualTo(LocalDateTime.parse("2026-06-15T10:30:00"));
//            softly.assertThat(eventResponse.get(0).totalSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.get(0).availableSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.get(0).ticketPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.00));
//            softly.assertThat(eventResponse.get(0).createdAt()).isEqualTo(LocalDateTime.parse("2026-05-16T11:34:11"));
//
//            softly.assertThat(eventResponse.get(1)).isNotNull();
//            softly.assertThat(eventResponse.get(1).id()).isEqualTo(currentEventId);
//            softly.assertThat(eventResponse.get(1).title()).isEqualTo("Rolling Loud, 2027");
//            softly.assertThat(eventResponse.get(1).description()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.get(1).venue()).isEqualTo("Mahalakshmi Racecourse, Mumbai");
//            softly.assertThat(eventResponse.get(1).eventDate()).isEqualTo(LocalDateTime.parse("2027-06-19T10:00:00"));
//            softly.assertThat(eventResponse.get(1).totalSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.get(1).availableSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.get(1).ticketPrice()).isEqualByComparingTo(BigDecimal.valueOf(1299.00));
//            softly.assertThat(eventResponse.get(1).createdAt()).isEqualTo(LocalDateTime.parse("2026-05-16T11:34:11"));
//
//            softly.assertThat(eventResponse.get(2)).isNotNull();
//            softly.assertThat(eventResponse.get(2).id()).isEqualTo(currentEventId);
//            softly.assertThat(eventResponse.get(2).title()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.get(2).description()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.get(2).venue()).isEqualTo("Mahalakshmi Racecourse, Mumbai");
//            softly.assertThat(eventResponse.get(2).eventDate()).isEqualTo(LocalDateTime.parse("2026-06-15T10:30:00"));
//            softly.assertThat(eventResponse.get(2).totalSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.get(2).availableSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.get(2).ticketPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.00));
//            softly.assertThat(eventResponse.get(2).createdAt()).isEqualTo(LocalDateTime.parse("2026-05-16T11:34:11"));
//        });
//    }
//    @Test
//    @DisplayName("getEventById -> should return a event with eventId on success")
//    void getEventById_shouldReturnEventByEventId_whenRequestIsValid() {
//        //ARRANGE
//        when(eventRepository.findById(currentEventId)).thenReturn(Optional.of(savedEvent));
//        //ACT
//        EventResponse eventResponse = eventService.getEventById(currentEventId);
//        //ASSERT
//        assertSoftly(softly -> {
//            softly.assertThat(eventResponse).isNotNull();
//            softly.assertThat(eventResponse.id()).isEqualTo(currentEventId);
//            softly.assertThat(eventResponse.title()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.description()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.venue()).isEqualTo("Mahalakshmi Racecourse, Mumbai");
//            softly.assertThat(eventResponse.eventDate()).isEqualTo(LocalDateTime.parse("2026-06-15T10:30:00"));
//            softly.assertThat(eventResponse.totalSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.availableSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.ticketPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.00));
//            softly.assertThat(eventResponse.createdAt()).isEqualTo(LocalDateTime.parse("2026-05-16T11:34:11"));
//        });
//    }
//
//        @Test
//        @DisplayName("getEventById -> should throw ResourceNotFoundException when Event not found")
//        void getEventById_shouldThrowResourceNotFoundException_whenEventNotFound(){
//            //ARRANGE
//            when(eventRepository.findById(currentEventId)).thenReturn(Optional.empty());
//            //ACT + ASSERT
//            assertThatThrownBy(() -> eventService.getEventById(currentEventId))
//                    .isInstanceOf(ResourceNotFoundException.class)
//                    .hasMessage("Event not found");
//    }
//    @Test
//    @DisplayName("updateEvent -> should return event response on success")
//    void updateEvent_shouldReturnEventResponse_whenRequestIsValid(){
//        //ARRANGE
//        when(eventRepository.findById(currentEventId)).thenReturn(Optional.of(savedEvent));
//        when(eventRepository.save(any(EventEntity.class))).thenReturn(updatedEvent);
//        //ACT
//        EventResponse eventResponse = eventService.updateEvent(currentEventId, updateEventRequest);
//        //ASSERT
//        assertSoftly(softly -> {
//            softly.assertThat(eventResponse).isNotNull();
//            softly.assertThat(eventResponse.id()).isEqualTo(currentEventId);
//            softly.assertThat(eventResponse.title()).isEqualTo("Rolling Loud, 2027");
//            softly.assertThat(eventResponse.description()).isEqualTo("Rolling Loud, 2026");
//            softly.assertThat(eventResponse.venue()).isEqualTo("Mahalakshmi Racecourse, Mumbai");
//            softly.assertThat(eventResponse.eventDate()).isEqualTo(LocalDateTime.parse("2027-06-19T10:00:00"));
//            softly.assertThat(eventResponse.totalSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.availableSeats()).isEqualTo(1800);
//            softly.assertThat(eventResponse.ticketPrice()).isEqualByComparingTo(BigDecimal.valueOf(1299.00));
//            softly.assertThat(eventResponse.createdAt()).isEqualTo(LocalDateTime.parse("2026-05-16T11:34:11"));
//        });
//    }
//    @Test
//    @DisplayName("updateEvent -> should throw ResourceNotFoundException when event not found")
//    void updateEvent_shouldThrowResourceNotFoundException_whenEventNotFound(){
//        //ARRANGE
//        when(eventRepository.findById(currentEventId)).thenReturn(Optional.empty());
//        //ACT + ASSERT
//        assertThatThrownBy(() -> eventService.updateEvent(currentEventId, updateEventRequest))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("Event not found");
//
//        verify(eventRepository, never()).save(any(EventEntity.class));
//    }
//    @Test
//    @DisplayName("deleteEvent -> should return void on success")
//    void deleteEvent_shouldReturnVoid_whenRequestIsValid(){
//        //ARRANGE
//        when(eventRepository.findById(currentEventId)).thenReturn(Optional.of(savedEvent));
//
//        //ACT
//        eventService.deleteEvent(currentEventId);
//        //ASSERT
//        verify(eventRepository , times(1)).delete(any(EventEntity.class));
//    }
//    @Test
//    @DisplayName("deleteEvent -> should throw ResourceNotFoundException when event not found")
//    void deleteEvent_shouldThrowResourceNotFoundException_whenEventNotFound(){
//        //ARRANGE
//        when(eventRepository.findById(currentEventId)).thenReturn(Optional.empty());
//        //ACT + ASSERT
//        assertThatThrownBy(() -> eventService.deleteEvent(currentEventId))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessage("Event not found");
//    }
//}
