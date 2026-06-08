package com.nisarg.crowdflow.unittests;

import com.nisarg.security.RateLimitingFilter;
import com.nisarg.security.RateLimitingService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private RateLimitingService rateLimitingService;
    private jakarta.servlet.FilterChain filterChain;

    @BeforeEach
    void setup() {
        rateLimitingFilter = new RateLimitingFilter(rateLimitingService);
        filterChain = mock(jakarta.servlet.FilterChain.class);
    }

    @Test
    @DisplayName("should allow unprotected endpoint")
    void unprotectedEndpoint_shouldAllowRequest_whenEndpointIsNotRateLimited() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/events");
        request.setRemoteAddr("192.168.1.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("should allow login request within rate limit")
    void login_shouldAllowRequest_whenWithinRateLimit() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("should block login request when rate limit exceeded")
    void login_shouldBlockRequest_whenRateLimitExceeded() throws ServletException, IOException {

        MockHttpServletResponse blockedResponse = null;

        for (int i = 0; i < 6; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.10");

            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);

            if (i == 5) {
                blockedResponse = response;
            }
        }

        verify(filterChain, times(5)).doFilter(any(), any());

        assertThat(blockedResponse).isNotNull();
        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentAsString())
                .contains("Too many requests");
    }

    @Test
    @DisplayName("should allow register request within rate limit")
    void register_shouldAllowRequest_whenWithinRateLimit() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");
        request.setRemoteAddr("192.168.1.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("should block register request when rate limit exceeded")
    void register_shouldBlockRequest_whenRateLimitExceeded() throws ServletException, IOException {

        MockHttpServletResponse blockedResponse = null;

        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/register");
            request.setRemoteAddr("192.168.1.10");

            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);

            if (i == 3) {
                blockedResponse = response;
            }
        }

        verify(filterChain, times(3)).doFilter(any(), any());

        assertThat(blockedResponse).isNotNull();
        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("should allow login request within rate limit")
    void refresh_shouldAllowRequest_whenWithinRateLimit() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/refresh");
        request.setRemoteAddr("192.168.1.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("should block refresh request when rate limit exceeded")
    void refresh_shouldBlockRequest_whenRateLimitExceeded() throws ServletException, IOException {

        MockHttpServletResponse blockedResponse = null;

        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/refresh");
            request.setRemoteAddr("192.168.1.10");

            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);

            if (i == 10) {
                blockedResponse = response;
            }
        }

        verify(filterChain, times(10)).doFilter(any(), any());

        assertThat(blockedResponse).isNotNull();
        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("should maintain separate buckets per endpoint")
    void rateLimiting_shouldMaintainSeparateBuckets_whenEndpointsDiffer() throws ServletException, IOException {

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest loginRequest = new MockHttpServletRequest();
            loginRequest.setRequestURI("/api/auth/login");
            loginRequest.setRemoteAddr("192.168.1.10");

            rateLimitingFilter.doFilter(
                    loginRequest,
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        MockHttpServletRequest refreshRequest = new MockHttpServletRequest();
        refreshRequest.setRequestURI("/api/auth/refresh");
        refreshRequest.setRemoteAddr("192.168.1.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(refreshRequest, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("should maintain separate buckets per ip")
    void rateLimiting_shouldMaintainSeparateBuckets_whenIpsDiffer() throws ServletException, IOException {

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.10");

            rateLimitingFilter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        MockHttpServletRequest secondIpRequest = new MockHttpServletRequest();
        secondIpRequest.setRequestURI("/api/auth/login");
        secondIpRequest.setRemoteAddr("192.168.1.20");

        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(secondIpRequest, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(429);
    }
}