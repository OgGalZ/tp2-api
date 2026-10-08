package fr.efrei.tp2;

import fr.efrei.tp2.common.ApiException;
import fr.efrei.tp2.currency.CurrencyTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Exercice 2 - Devises")
class CurrencyApiTest extends AbstractApiTest {

    @Autowired
    private CurrencyTransactionRepository transactionRepository;

    /** Chaque test utilise sa propre IP pour ne pas partager le seau du rate limiter. */
    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    @DisplayName("GET /convert -> taux de l'API externe, puis servi depuis le cache")
    void convert_usesExternalRate_thenCache() throws Exception {
        when(exchangeRateClient.fetchRate("GBP", "JPY")).thenReturn(new BigDecimal("190.5"));

        mockMvc.perform(get("/api/currency/convert?amount=10&from=gbp&to=JPY").with(fromIp("10.0.0.1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("GBP"))
                .andExpect(jsonPath("$.rate").value(190.5))
                .andExpect(jsonPath("$.convertedAmount").value(1905.0))
                .andExpect(jsonPath("$.source").value("API"));

        mockMvc.perform(get("/api/currency/convert?amount=2&from=GBP&to=JPY").with(fromIp("10.0.0.1")))
                .andExpect(status().isOk());

        // @Cacheable : un seul appel réel malgré deux conversions
        verify(exchangeRateClient, times(1)).fetchRate("GBP", "JPY");
    }

    @Test
    @DisplayName("API externe indisponible -> 503 et rollback (aucune transaction enregistrée)")
    void externalApiDown_returns503_andNothingSaved() throws Exception {
        when(exchangeRateClient.fetchRate(anyString(), eq("CHF")))
                .thenThrow(new ApiException.ServiceUnavailable("Le service de taux de change est momentanément indisponible. Réessayez plus tard."));
        long before = transactionRepository.count();

        mockMvc.perform(get("/api/currency/convert?amount=10&from=USD&to=CHF").with(fromIp("10.0.0.2")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        assertThat(transactionRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("Rate limiting -> 429 au-delà du seuil (5/min en profil test)")
    void rateLimit_returns429() throws Exception {
        when(exchangeRateClient.fetchRate("EUR", "CAD")).thenReturn(new BigDecimal("1.5"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/currency/convert?amount=1&from=EUR&to=CAD").with(fromIp("10.0.0.3")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/currency/convert?amount=1&from=EUR&to=CAD").with(fromIp("10.0.0.3")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("Taux manuel (POST), mise à jour (PUT), suppression de transaction (DELETE)")
    void manualRate_update_delete() throws Exception {
        String json = mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100, "from": "EUR", "to": "XOF", "rate": 655.957}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.convertedAmount").value(65595.7))
                .andReturn().getResponse().getContentAsString();
        Integer txId = com.jayway.jsonpath.JsonPath.read(json, "$.transactionId");

        mockMvc.perform(put("/api/currency/update-rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from": "EUR", "to": "XOF", "rate": 656}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(656));

        // Le taux local est désormais prioritaire : pas d'appel à l'API externe
        mockMvc.perform(get("/api/currency/convert?amount=1&from=EUR&to=XOF").with(fromIp("10.0.0.4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("LOCAL"));
        verify(exchangeRateClient, never()).fetchRate("EUR", "XOF");

        mockMvc.perform(delete("/api/currency/delete-transaction/" + txId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/currency/delete-transaction/" + txId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Paramètres invalides -> 400")
    void invalidParams_return400() throws Exception {
        mockMvc.perform(get("/api/currency/convert?amount=-5&from=EURO&to=USD").with(fromIp("10.0.0.5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }
}
