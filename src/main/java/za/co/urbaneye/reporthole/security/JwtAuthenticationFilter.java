package za.co.urbaneye.reporthole.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security filter responsible for processing JWT authentication
 * on every incoming HTTP request.
 *
 * <p>This filter executes once per request and performs the following:</p>
 * <ul>
 *     <li>Reads the Authorization header</li>
 *     <li>Validates Bearer JWT tokens</li>
 *     <li>Extracts the authenticated user ID and roles</li>
 *     <li>Creates a Spring Security authentication object</li>
 *     <li>Stores authentication in the security context</li>
 * </ul>
 *
 * <p>If the token is missing, the request continues without authentication.
 * If the token is invalid or expired, a 401 Unauthorized response is returned.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final Jwt jwt;

    /**
     * Intercepts each HTTP request and applies JWT authentication logic.
     *
     * @param request     incoming HTTP request
     * @param response    outgoing HTTP response
     * @param filterChain filter chain for continuing request processing
     * @throws ServletException if servlet processing fails
     * @throws IOException      if input/output errors occur
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Continue request if no Bearer token is present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Reject invalid or expired token
        if (!jwt.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }

        String userId = jwt.extractUserId(token);
        List<String> roles = jwt.extractRoles(token);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}