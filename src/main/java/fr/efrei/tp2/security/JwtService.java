package fr.efrei.tp2.security;

import fr.efrei.tp2.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Génération / validation des JWT d'accès (format Header.Payload.Signature, signature HMAC-SHA).
 * Payload : sub = username, uid = id utilisateur, type = access, iat, exp.
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Vérifie la signature et l'expiration puis renvoie l'utilisateur.
     *
     * @throws JwtException si le jeton est invalide, altéré ou expiré
     */
    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Type de jeton invalide");
        }
        Number uid = claims.get(CLAIM_USER_ID, Number.class);
        if (uid == null) {
            throw new JwtException("Jeton sans identifiant utilisateur");
        }
        return new AuthenticatedUser(uid.longValue(), claims.getSubject());
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
