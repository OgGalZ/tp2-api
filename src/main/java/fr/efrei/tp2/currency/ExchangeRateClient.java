package fr.efrei.tp2.currency;

import fr.efrei.tp2.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Client de l'API publique frankfurter (taux BCE), via le client HTTP moderne WebClient.
 * <ul>
 *   <li>Timeout sur chaque appel ;</li>
 *   <li>3 tentatives avec backoff exponentiel (500 ms, 1 s) si l'API est indisponible ou lente ;</li>
 *   <li>après épuisement : 503 Service Unavailable explicite ;</li>
 *   <li>devise inconnue (4xx) : pas de retry, 400 immédiat.</li>
 * </ul>
 */
@Component
public class ExchangeRateClient {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public ExchangeRateClient(WebClient.Builder builder,
                              @Value("${app.exchange.base-url}") String baseUrl,
                              @Value("${app.exchange.timeout-ms}") long timeoutMs) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Retryable(
            retryFor = ExternalApiUnavailableException.class,
            noRetryFor = ApiException.class,
            notRecoverable = ApiException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public BigDecimal fetchRate(String from, String to) {
        log.info("Appel à l'API externe de taux de change : {} -> {}", from, to);
        FrankfurterResponse response;
        try {
            response = webClient.get()
                    .uri(uri -> uri.path("/latest")
                            .queryParam("base", from)
                            .queryParam("symbols", to)
                            .build())
                    .retrieve()
                    .bodyToMono(FrankfurterResponse.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw unsupported(from, to);
            }
            throw new ExternalApiUnavailableException("L'API externe a répondu " + e.getStatusCode(), e);
        } catch (RuntimeException e) {
            // Timeout, connexion refusée, DNS...
            throw new ExternalApiUnavailableException("API externe injoignable : " + e.getMessage(), e);
        }

        if (response == null || response.rates() == null || !response.rates().containsKey(to)) {
            throw unsupported(from, to);
        }
        return response.rates().get(to);
    }

    /** Appelé quand les 3 tentatives ont échoué. */
    @Recover
    public BigDecimal recover(ExternalApiUnavailableException e, String from, String to) {
        log.warn("API de taux indisponible après 3 tentatives ({} -> {}) : {}", from, to, e.getMessage());
        throw new ApiException.ServiceUnavailable(
                "Le service de taux de change est momentanément indisponible. Réessayez plus tard.");
    }

    private ApiException unsupported(String from, String to) {
        return new ApiException.BadRequest("Paire de devises non prise en charge par l'API externe : "
                + from + " -> " + to + ". Définissez un taux manuel via POST /api/currency/convert.");
    }

    public record FrankfurterResponse(BigDecimal amount, String base, String date, Map<String, BigDecimal> rates) {
    }

    /** Exception interne déclenchant le retry (jamais exposée telle quelle au client). */
    public static class ExternalApiUnavailableException extends RuntimeException {
        public ExternalApiUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
