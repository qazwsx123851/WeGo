package com.wego.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import jakarta.servlet.Filter;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RateLimitConfig.
 *
 * Tests rate limiting behavior for API endpoints.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
    }

    @Test
    @DisplayName("Should create filter registration bean")
    void rateLimitFilter_shouldCreateFilterRegistrationBean() {
        FilterRegistrationBean<Filter> registrationBean = rateLimitConfig.rateLimitFilter();

        assertThat(registrationBean).isNotNull();
        assertThat(registrationBean.getFilter()).isNotNull();
        assertThat(registrationBean.getUrlPatterns()).contains("/api/*");
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void rateLimitFilter_withinLimit_shouldAllowRequest() throws Exception {
        // Arrange
        FilterRegistrationBean<Filter> registrationBean = rateLimitConfig.rateLimitFilter();
        Filter filter = registrationBean.getFilter();

        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    @DisplayName("Should block requests exceeding rate limit")
    void rateLimitFilter_exceedingLimit_shouldBlockRequest() throws Exception {
        // Arrange
        FilterRegistrationBean<Filter> registrationBean = rateLimitConfig.rateLimitFilter();
        Filter filter = registrationBean.getFilter();

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        when(response.getWriter()).thenReturn(printWriter);

        // Act - Make 101 requests (rate limit is 100/minute)
        for (int i = 0; i < 100; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // 101st request should be blocked
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, times(100)).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        assertThat(stringWriter.toString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Should use X-Forwarded-For header when present")
    void rateLimitFilter_withXForwardedFor_shouldUseProxyIp() throws Exception {
        // Arrange
        FilterRegistrationBean<Filter> registrationBean = rateLimitConfig.rateLimitFilter();
        Filter filter = registrationBean.getFilter();

        // When X-Forwarded-For is present, getRemoteAddr should not be called
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        // Verify X-Forwarded-For was checked
        verify(request).getHeader("X-Forwarded-For");
    }

    @Test
    @DisplayName("Should isolate rate limits per IP")
    void rateLimitFilter_differentIps_shouldHaveSeparateLimits() throws Exception {
        // Arrange
        FilterRegistrationBean<Filter> registrationBean = rateLimitConfig.rateLimitFilter();
        Filter filter = registrationBean.getFilter();

        // Act - Each IP gets its own bucket
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        filter.doFilter(request, response, filterChain);

        when(request.getRemoteAddr()).thenReturn("192.168.1.11");
        filter.doFilter(request, response, filterChain);

        // Assert - Both requests should pass
        verify(filterChain, times(2)).doFilter(request, response);
    }
}
