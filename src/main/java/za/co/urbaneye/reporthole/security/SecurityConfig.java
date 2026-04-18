package za.co.urbaneye.reporthole.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main Spring Security configuration class for the application.
 *
 * <p>This configuration defines authentication and authorization rules,
 * password encoding, and JWT filter integration.</p>
 *
 * <p>Security features enabled:</p>
 * <ul>
 *     <li>Web security configuration</li>
 *     <li>Method-level security using annotations</li>
 *     <li>JWT-based stateless authentication</li>
 *     <li>BCrypt password hashing</li>
 * </ul>
 *
 * <p>Publicly accessible endpoints:</p>
 * <ul>
 *     <li>{@code /auth/**} - Authentication endpoints</li>
 *     <li>{@code /v3/api-docs/**} - OpenAPI documentation</li>
 *     <li>{@code /swagger-ui/**} - Swagger UI</li>
 * </ul>
 *
 * <p>All other endpoints require authentication.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Custom JWT authentication filter.
     */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>This disables CSRF for stateless APIs, defines public and protected
     * routes, and registers the JWT authentication filter before the default
     * username/password authentication filter.</p>
     *
     * @param http Spring Security HTTP configuration
     * @return configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Exposes the Spring AuthenticationManager bean.
     *
     * <p>This is commonly used for manual authentication flows
     * such as login endpoints.</p>
     *
     * @param config authentication configuration
     * @return authentication manager instance
     * @throws Exception if manager creation fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides the password encoder used for hashing user passwords.
     *
     * <p>Uses BCrypt with strength factor 12.</p>
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}