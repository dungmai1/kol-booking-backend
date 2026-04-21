package kolbooking.datn.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import kolbooking.datn.auth.domain.AppUser;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    @Getter
    private final long accessTokenTtlSeconds;

    @Getter
    private final long refreshTokenTtlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-seconds}") long accessTtl,
            @Value("${app.jwt.refresh-token-ttl-seconds}") long refreshTtl
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenTtlSeconds = accessTtl;
        this.refreshTokenTtlSeconds = refreshTtl;
    }

    public String generateAccessToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenTtlSeconds)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
