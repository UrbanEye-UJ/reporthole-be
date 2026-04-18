package za.co.urbaneye.reporthole.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.co.urbaneye.reporthole.user.entity.UserRole;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utility component responsible for generating, parsing,
 * and validating JSON Web Tokens (JWT).
 *
 * <p>This class is used to authenticate users and securely
 * transmit identity and authorization information between
 * the client and server.</p>
 *
 * <p>Token contents include:</p>
 * <ul>
 *     <li>User ID as the subject</li>
 *     <li>User role claim</li>
 *     <li>Issued timestamp</li>
 *     <li>Expiration timestamp</li>
 * </ul>
 *
 * <p>Configuration properties:</p>
 * <ul>
 *     <li>{@code web.jwt.key} - Base64 encoded signing key</li>
 *     <li>{@code web.jwt.expiration} - Token lifespan in milliseconds</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
@Component
public class Jwt {

    /**
     * Base64 encoded JWT signing secret.
     */
    @Value("${web.jwt.key}")
    private String secretKey;

    /**
     * JWT expiration time in milliseconds.
     */
    @Value("${web.jwt.expiration}")
    private long expirationMs;

    /**
     * Builds the HMAC signing key used for token signing and verification.
     *
     * @return generated secret signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT token for the authenticated user.
     *
     * <p>The token includes the user ID as subject and the
     * user's role as a custom claim.</p>
     *
     * @param userId unique identifier of the user
     * @param role   user's assigned role
     * @return signed JWT token string
     */
    public String generateToken(UUID userId, UserRole role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts all claims contained in a JWT token.
     *
     * @param token JWT token string
     * @return parsed token claims
     * @throws JwtException if token is invalid or malformed
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user ID stored in the token subject.
     *
     * @param token JWT token string
     * @return user ID as string
     */
    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts user roles from the token claims.
     *
     * <p>Expected claim name is {@code roles}.</p>
     *
     * @param token JWT token string
     * @return list of roles associated with the token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) extractClaims(token).get("roles");
    }

    /**
     * Validates whether a token is properly signed,
     * well-formed, and not expired.
     *
     * @param token JWT token string
     * @return {@code true} if valid, otherwise {@code false}
     */
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}