package com.fintrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.exception.ApiErrorResponse;
import com.fintrack.security.CustomUserDetailsService;
import com.fintrack.security.JwtAuthenticationFilter;
import com.fintrack.security.OAuth2AuthenticationFailureHandler;
import com.fintrack.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final String frontendUrl;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomUserDetailsService userDetailsService,
            OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2FailureHandler,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // JWT API + OAuth2 callback architecture
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                /*
                 * OAuth2 login needs a temporary session.
                 * JWT authentication itself remains stateless.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .authenticationProvider(
                        authenticationProvider()
                )

                .exceptionHandling(exception ->
                        exception

                                // 401
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {

                                            response.setStatus(
                                                    HttpServletResponse.SC_UNAUTHORIZED
                                            );

                                            response.setContentType(
                                                    "application/json"
                                            );

                                            response.setCharacterEncoding(
                                                    "UTF-8"
                                            );

                                            ApiErrorResponse error =
                                                    new ApiErrorResponse(
                                                            401,
                                                            "Unauthorized",
                                                            "Authentication required",
                                                            LocalDateTime.now(),
                                                            request.getRequestURI()
                                                    );

                                            objectMapper.writeValue(
                                                    response.getOutputStream(),
                                                    error
                                            );
                                        }
                                )

                                // 403
                                .accessDeniedHandler(
                                        (request, response, accessDeniedException) -> {

                                            response.setStatus(
                                                    HttpServletResponse.SC_FORBIDDEN
                                            );

                                            response.setContentType(
                                                    "application/json"
                                            );

                                            response.setCharacterEncoding(
                                                    "UTF-8"
                                            );

                                            ApiErrorResponse error =
                                                    new ApiErrorResponse(
                                                            403,
                                                            "Forbidden",
                                                            "Access denied",
                                                            LocalDateTime.now(),
                                                            request.getRequestURI()
                                                    );

                                            objectMapper.writeValue(
                                                    response.getOutputStream(),
                                                    error
                                            );
                                        }
                                )
                )

                .authorizeHttpRequests(auth -> auth

                        // Authentication endpoints
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // OAuth2 authorization/callback
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // OAuth2 error endpoint
                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        // Health check endpoint
                        .requestMatchers(
                                "/api/health"
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 ->
                        oauth2
                                .successHandler(
                                        oauth2SuccessHandler
                                )
                                .failureHandler(
                                        oauth2FailureHandler
                                )
                )

                /*
                 * JWT must execute before Spring's
                 * username/password authentication filter.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(
                passwordEncoder
        );

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(frontendUrl)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}