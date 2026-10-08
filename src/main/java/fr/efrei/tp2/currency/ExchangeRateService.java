package fr.efrei.tp2.currency;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Couche de cache devant l'API externe : un taux déjà récupéré est servi depuis le cache
 * (Caffeine, expiration 10 min) sans nouvel appel HTTP.
 * Les exceptions ne sont jamais mises en cache.
 */
@Service
public class ExchangeRateService {

    private final ExchangeRateClient client;

    public ExchangeRateService(ExchangeRateClient client) {
        this.client = client;
    }

    @Cacheable(cacheNames = "exchangeRates", key = "#p0 + '-' + #p1")
    public BigDecimal getExternalRate(String from, String to) {
        return client.fetchRate(from, to);
    }
}
