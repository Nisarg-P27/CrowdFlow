package com.nisarg.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.rate-limiting.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RateLimitingFilter extends OncePerRequestFilter {


    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

//      FETCH URI ANd CLIENT-IP FROM REQUEST
        String requestUri = request.getRequestURI();
        String clientIp = extractClientIp(request);

//      CHECK WHETHER BUCKET HAS TOKEN OR NOT
        if (rateLimitingService.tryConsume(requestUri, clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        response.getWriter().write("""
        {
            "status": 429,
            "message": "Too many requests"
        }
        """);
    }

    //    HELPER FUNCTION
    private String extractClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}