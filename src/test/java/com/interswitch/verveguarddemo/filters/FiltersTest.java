package com.interswitch.verveguarddemo.filters;

import com.interswitch.verveguarddemo.security.JwtUserPrincipal;
import com.interswitch.verveguarddemo.security.ProxyAwareAuthenticationDetailsSource;
import com.interswitch.verveguarddemo.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiltersTest {

    @Nested
    class TraceIdFilterTest {

        private TraceIdFilter filter;

        @Mock
        private HttpServletRequest request;

        @Mock
        private HttpServletResponse response;

        @Mock
        private FilterChain filterChain;

        @BeforeEach
        void setUp() {
            filter = new TraceIdFilter();
            MDC.clear();
        }

        @AfterEach
        void tearDown() {
            MDC.clear();
        }

        @Test
        void doFilterInternal_usesProvidedTraceId() throws Exception {
            when(request.getHeader("X-Trace-Id")).thenReturn("provided-trace-id");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_generatesTraceIdWhenMissing() throws Exception {
            when(request.getHeader("X-Trace-Id")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_generatesTraceIdWhenBlank() throws Exception {
            when(request.getHeader("X-Trace-Id")).thenReturn("   ");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_clearsMdcAfterFilter() throws Exception {
            when(request.getHeader("X-Trace-Id")).thenReturn("test-trace");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(MDC.get("traceId")).isNull();
        }

        @Test
        void doFilterInternal_clearsMdcEvenOnException() throws Exception {
            when(request.getHeader("X-Trace-Id")).thenReturn("test-trace");
            doThrow(new RuntimeException("Test")).when(filterChain).doFilter(request, response);

            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (RuntimeException e) {
                // expected
            }

            assertThat(MDC.get("traceId")).isNull();
        }

        @Test
        void shouldNotFilterAsyncDispatch_returnsFalse() {
            assertThat(filter.shouldNotFilterAsyncDispatch()).isFalse();
        }
    }

    @Nested
    class JwtAuthFilterTest {

        @Mock
        private JwtService jwtService;

        @Mock
        private ProxyAwareAuthenticationDetailsSource detailsSource;

        @Mock
        private HttpServletRequest request;

        @Mock
        private HttpServletResponse response;

        @Mock
        private FilterChain filterChain;

        private JwtAuthFilter filter;

        @BeforeEach
        void setUp() {
            filter = new JwtAuthFilter(jwtService, detailsSource);
            SecurityContextHolder.clearContext();
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void doFilterInternal_continuesWithoutAuthWhenNoHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void doFilterInternal_continuesWithoutAuthWhenInvalidHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic abc123");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void doFilterInternal_setsAuthenticationForValidToken() throws Exception {
            String token = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            JwtUserPrincipal principal = mock(JwtUserPrincipal.class);
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(principal).getAuthorities();
            when(jwtService.extractUserPrincipal(token)).thenReturn(principal);

            WebAuthenticationDetails details = mock(WebAuthenticationDetails.class);
            when(detailsSource.buildDetails(request)).thenReturn(details);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        }

        @Test
        void doFilterInternal_continuesOnTokenExtractionFailure() throws Exception {
            String token = "invalid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUserPrincipal(token)).thenThrow(new RuntimeException("Invalid token"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void doFilterInternal_skipsWhenAlreadyAuthenticated() throws Exception {
            // Pre-set authentication
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user", null, List.of())
            );

            when(request.getHeader("Authorization")).thenReturn("Bearer some.token");

            filter.doFilterInternal(request, response, filterChain);

            verify(jwtService, never()).extractUserPrincipal(any());
            verify(filterChain).doFilter(request, response);
        }
    }
}
