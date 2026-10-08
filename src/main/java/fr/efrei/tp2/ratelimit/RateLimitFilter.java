package fr.efrei.tp2.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.efrei.tp2.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre Spring personnalisé de limitation de débit (algorithme du seau à jetons / token bucket).
 * Appliqué uniquement à GET /api/currency/convert : par défaut 20 requêtes par minute et par adresse IP.
 * Au-delà : 429 Too Many Requests + en-tête Retry-After.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final String LIMITED_PATH = "/api/currency/convert";

    private final int capacity;
    private final long periodNanos;
    private final ObjectMapper objectMapper;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.capacity}") int capacity,
                           @Value("${app.rate-limit.period-seconds}") long periodSeconds,
                           ObjectMapper objectMapper) {
        this.capacity = capacity;
        this.periodNanos = periodSeconds * 1_000_000_000L;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("GET".equalsIgnoreCase(request.getMethod()) && LIMITED_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, ip -> new TokenBucket(capacity, periodNanos));

        long waitNanos = bucket.tryConsume();
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(capacity));
        if (waitNanos == 0) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.remaining()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, (long) Math.ceil(waitNanos / 1_000_000_000.0));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("X-Rate-Limit-Remaining", "0");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS,
                "Limite de " + capacity + " requêtes par minute dépassée. Réessayez dans " + retryAfterSeconds + " s.",
                request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }

    /** Seau à jetons thread-safe : recharge continue de {@code capacity} jetons par période. */
    static final class TokenBucket {
        private final int capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastRefill;

        TokenBucket(int capacity, long periodNanos) {
            this.capacity = capacity;
            this.refillPerNano = (double) capacity / periodNanos;
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }

        /** @return 0 si un jeton a été consommé, sinon le temps d'attente (ns) avant le prochain jeton. */
        synchronized long tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            return (long) Math.ceil((1 - tokens) / refillPerNano);
        }

        synchronized int remaining() {
            return (int) Math.floor(tokens);
        }

        private void refill() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
            lastRefill = now;
        }
    }
}
