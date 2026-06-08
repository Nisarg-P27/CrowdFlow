package com.nisarg.crowdflow.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nisarg.dtos.requests.CreateBookingRequest;
import com.nisarg.dtos.requests.LoginRequest;
import com.nisarg.entities.EventEntity;
import com.nisarg.entities.UserEntity;
import com.nisarg.enums.BookingStatus;
import com.nisarg.enums.UserRole;
import com.nisarg.repositories.BookingRepository;
import com.nisarg.repositories.EventRepository;
import com.nisarg.repositories.PaymentRepository;
import com.nisarg.repositories.UserRepository;
import org.junit.jupiter.api.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("crowdflow_test")
                    .withUsername("postgres")
                    .withPassword("password");

    private static String authToken;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @AfterEach
    void cleanup() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void shouldCreateBookingSuccessfully() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Coldplay");
        event.setDescription("Concert");
        event.setVenue("Mumbai");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setTotalSeats(100);
        event.setAvailableSeats(100);
        event.setTicketPrice(BigDecimal.valueOf(1000));

        EventEntity savedEvent = eventRepository.save(event);

        CreateBookingRequest request = new CreateBookingRequest(
                savedEvent.getId(),
                2
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var booking = bookingRepository.findAll().getFirst();

        assertThat(booking.getBookingStatus())
                .isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getSeatCount()).isEqualTo(2);

        EventEntity updatedEvent = eventRepository.findById(savedEvent.getId()).orElseThrow();

        assertThat(updatedEvent.getAvailableSeats()).isEqualTo(98);
    }

    @Test
    void shouldFailBookingWhenSeatsAreInsufficient() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Small Event");
        event.setDescription("Limited");
        event.setVenue("Pune");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setTotalSeats(5);
        event.setAvailableSeats(5);
        event.setTicketPrice(BigDecimal.valueOf(1000));

        EventEntity savedEvent = eventRepository.save(event);

        CreateBookingRequest request = new CreateBookingRequest(
                savedEvent.getId(),
                10
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAll()).isEmpty();
    }

    @Test
    void shouldCreateCancelledBookingWhenPaymentFails() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Expensive Event");
        event.setDescription("Premium");
        event.setVenue("Delhi");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setTotalSeats(50);
        event.setAvailableSeats(50);
        event.setTicketPrice(BigDecimal.valueOf(3000));

        EventEntity savedEvent = eventRepository.save(event);

        CreateBookingRequest request = new CreateBookingRequest(
                savedEvent.getId(),
                2
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var booking = bookingRepository.findAll().getFirst();

        assertThat(booking.getBookingStatus())
                .isEqualTo(BookingStatus.CANCELLED);

        EventEntity updatedEvent = eventRepository.findById(savedEvent.getId()).orElseThrow();

        assertThat(updatedEvent.getAvailableSeats()).isEqualTo(50);
    }

    @Test
    void shouldGetMyBookings() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("My Event");
        event.setDescription("Test");
        event.setVenue("Ahmedabad");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setTotalSeats(100);
        event.setAvailableSeats(100);
        event.setTicketPrice(BigDecimal.valueOf(1000));

        EventEntity savedEvent = eventRepository.save(event);

        CreateBookingRequest request = new CreateBookingRequest(
                savedEvent.getId(),
                1
        );

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldCancelBookingSuccessfully() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Cancelable");
        event.setDescription("Test");
        event.setVenue("Surat");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setTotalSeats(100);
        event.setAvailableSeats(100);
        event.setTicketPrice(BigDecimal.valueOf(1000));

        EventEntity savedEvent = eventRepository.save(event);

        CreateBookingRequest request = new CreateBookingRequest(
                savedEvent.getId(),
                2
        );

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        UUID bookingId = bookingRepository.findAll().getFirst().getId();

        mockMvc.perform(delete("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        var booking = bookingRepository.findById(bookingId).orElseThrow();

        assertThat(booking.getBookingStatus())
                .isEqualTo(BookingStatus.CANCELLED);

        EventEntity updatedEvent = eventRepository.findById(savedEvent.getId()).orElseThrow();

        assertThat(updatedEvent.getAvailableSeats()).isEqualTo(100);
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledBooking() throws Exception {
        EventEntity event = new EventEntity();
        event.setTitle("Duplicate Cancel");
        event.setDescription("Test");
        event.setVenue("Goa");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setTotalSeats(100);
        event.setAvailableSeats(100);
        event.setTicketPrice(BigDecimal.valueOf(3000));

        EventEntity savedEvent = eventRepository.save(event);

        CreateBookingRequest request = new CreateBookingRequest(
                savedEvent.getId(),
                2
        );

        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        UUID bookingId = bookingRepository.findAll().getFirst().getId();

        mockMvc.perform(delete("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectBookingWithInvalidPayload() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(),
                0
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}