package fr.efrei.tp2;

import fr.efrei.tp2.common.ApiException;
import fr.efrei.tp2.currency.ExchangeRateClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie le vrai comportement @Retryable : l'API externe pointe vers un port fermé,
 * le client fait 3 tentatives (backoff 500 ms puis 1 s) avant de renvoyer une erreur 503.
 */
@SpringBootTest(properties = {
        "app.exchange.base-url=http://localhost:1",
        "app.exchange.timeout-ms=500",
        "spring.datasource.url=jdbc:h2:mem:retrytest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
@DisplayName("Exercice 2 - Retry avec backoff puis 503")
class ExchangeRateRetryTest {

    @Autowired
    private ExchangeRateClient client;

    @Test
    @DisplayName("API injoignable -> 3 tentatives puis ServiceUnavailable (503)")
    void unreachableApi_retriesThenFails() {
        long start = System.currentTimeMillis();

        assertThatThrownBy(() -> client.fetchRate("EUR", "USD"))
                .isInstanceOf(ApiException.ServiceUnavailable.class)
                .hasMessageContaining("indisponible");

        // 2 attentes de backoff : 500 ms + 1000 ms
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(1400);
    }
}
