package com.nisarg.crowdflow.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nisarg.dtos.requests.CreateEventRequest;
import com.nisarg.dtos.requests.LoginRequest;
import com.nisarg.dtos.requests.UpdateEventRequest;
import com.nisarg.entities.EventEntity;
import com.nisarg.entities.UserEntity;
import com.nisarg.enums.UserRole;
import com.nisarg.repositories.EventRepository;
import com.nisarg.repositories.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("crowdflow_test")
                    .withUsername("postgres")
                    .withPassword("password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String authToken;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void setupAuth(
            @Autowired UserRepository userRepository,
            @Autowired PasswordEncoder passwordEncoder,
            @Autowired MockMvc mockMvc,
            @Autowired ObjectMapper objectMapper
    ) throws Exception {

        UserEntity user = new UserEntity();
        user.setName("Test User");
        user.setEmail("test@gmail.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);

        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(
                "test@gmail.com",
                "password123"
        );

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = objectMapper.readTree(response)
                .get("token")
                .asText();
    }

    @AfterAll
    static void cleanupAuth(
            @Autowired UserRepository userRepository
    ) {
        userRepository.deleteAll();
    }

    @BeforeEach
    void cleanup() {
        eventRepository.deleteAll();
    }

    @Test
    void shouldCreateEventSuccessfully() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Coldplay Live",
                "Music concert",
                "Mumbai Arena",
                LocalDateTime.now().plusDays(10),
                500,
                BigDecimal.valueOf(2999)
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        EventEntity savedEvent = eventRepository.findAll().getFirst();

        assertThat(savedEvent.getTitle()).isEqualTo("Coldplay Live");
        assertThat(savedEvent.getVenue()).isEqualTo("Mumbai Arena");
        assertThat(savedEvent.getAvailableSeats()).isEqualTo(500);
        assertThat(savedEvent.getTotalSeats()).isEqualTo(500);
    }

    @Test
    void shouldRejectCreateEventWithInvalidPayload() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "",
                "Music concert",
                "",
                LocalDateTime.now().minusDays(1),
                0,
                BigDecimal.valueOf(-100)
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(eventRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoEventsExist() throws Exception {
        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldGetAllEvents() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Tech Conference");
        event.setDescription("Developer meetup");
        event.setVenue("BKC");
        event.setEventDate(LocalDateTime.now().plusDays(15));
        event.setTotalSeats(200);
        event.setAvailableSeats(200);
        event.setTicketPrice(BigDecimal.valueOf(1500));

        eventRepository.save(event);

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Tech Conference"));
    }

    @Test
    void shouldGetEventById() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Startup Summit");
        event.setDescription("Networking event");
        event.setVenue("Pune");
        event.setEventDate(LocalDateTime.now().plusDays(20));
        event.setTotalSeats(300);
        event.setAvailableSeats(300);
        event.setTicketPrice(BigDecimal.valueOf(999));

        EventEntity savedEvent = eventRepository.save(event);

        mockMvc.perform(get("/api/events/{eventId}", savedEvent.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Startup Summit"));
    }

    @Test
    void shouldReturnNotFoundForUnknownEventId() throws Exception {
        mockMvc.perform(get("/api/events/{eventId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateEventSuccessfully() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Old Event");
        event.setDescription("Old description");
        event.setVenue("Old Venue");
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setTotalSeats(100);
        event.setAvailableSeats(100);
        event.setTicketPrice(BigDecimal.valueOf(500));

        EventEntity savedEvent = eventRepository.save(event);

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Event",
                "Updated description",
                "Updated Venue",
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(1200)
        );

        mockMvc.perform(put("/api/events/{eventId}", savedEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        EventEntity updatedEvent = eventRepository.findById(savedEvent.getId()).orElseThrow();

        assertThat(updatedEvent.getTitle()).isEqualTo("Updated Event");
        assertThat(updatedEvent.getVenue()).isEqualTo("Updated Venue");
        assertThat(updatedEvent.getTotalSeats()).isEqualTo(100);
        assertThat(updatedEvent.getAvailableSeats()).isEqualTo(100);
    }

    @Test
    void shouldRejectUpdateForUnknownEvent() throws Exception {
        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Event",
                "Updated description",
                "Updated Venue",
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(1200)
        );

        mockMvc.perform(put("/api/events/{eventId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteEventSuccessfully() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Delete Me");
        event.setDescription("To be deleted");
        event.setVenue("Delhi");
        event.setEventDate(LocalDateTime.now().plusDays(12));
        event.setTotalSeats(50);
        event.setAvailableSeats(50);
        event.setTicketPrice(BigDecimal.valueOf(250));

        EventEntity savedEvent = eventRepository.save(event);

        mockMvc.perform(delete("/api/events/{eventId}", savedEvent.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        assertThat(eventRepository.findById(savedEvent.getId())).isEmpty();
    }

    @Test
    void shouldRejectDeleteForUnknownEvent() throws Exception {
        mockMvc.perform(delete("/api/events/{eventId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}