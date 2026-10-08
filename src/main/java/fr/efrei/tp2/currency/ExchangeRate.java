package fr.efrei.tp2.currency;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Taux stocké localement (saisi manuellement), prioritaire sur l'API externe. */
@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"base_currency", "target_currency"}))
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    private String targetCurrency;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ExchangeRate() {
    }

    public ExchangeRate(String baseCurrency, String targetCurrency, BigDecimal rate) {
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRate(BigDecimal newRate) {
        this.rate = newRate;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getBaseCurrency() { return baseCurrency; }
    public String getTargetCurrency() { return targetCurrency; }
    public BigDecimal getRate() { return rate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
