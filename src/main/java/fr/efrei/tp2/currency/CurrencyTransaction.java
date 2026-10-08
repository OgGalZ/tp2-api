package fr.efrei.tp2.currency;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency_transactions")
public class CurrencyTransaction {

    public enum RateSource { API, LOCAL, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "converted_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal convertedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RateSource source;

    @Column(nullable = false)
    private LocalDateTime date;

    protected CurrencyTransaction() {
    }

    public CurrencyTransaction(BigDecimal amount, String fromCurrency, String toCurrency,
                               BigDecimal rate, BigDecimal convertedAmount, RateSource source) {
        this.amount = amount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.convertedAmount = convertedAmount;
        this.source = source;
        this.date = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getFromCurrency() { return fromCurrency; }
    public String getToCurrency() { return toCurrency; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getConvertedAmount() { return convertedAmount; }
    public RateSource getSource() { return source; }
    public LocalDateTime getDate() { return date; }
}
