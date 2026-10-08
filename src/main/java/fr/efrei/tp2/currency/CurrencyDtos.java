package fr.efrei.tp2.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CurrencyDtos {

    private CurrencyDtos() {
    }

    private static final String CURRENCY_REGEX = "^[A-Za-z]{3}$";
    private static final String CURRENCY_MSG = "doit être un code devise ISO 4217 de 3 lettres (ex. EUR)";

    @Schema(description = "Conversion avec un taux manuel (devises non couvertes par l'API externe)")
    public record ManualConversionRequest(
            @Schema(example = "100")
            @NotNull(message = "amount est obligatoire")
            @DecimalMin(value = "0.01", message = "amount doit être supérieur à 0")
            @DecimalMax(value = "1000000000", message = "amount est trop élevé")
            BigDecimal amount,

            @Schema(example = "EUR")
            @NotBlank(message = "from est obligatoire")
            @Pattern(regexp = CURRENCY_REGEX, message = "from " + CURRENCY_MSG)
            String from,

            @Schema(example = "XOF")
            @NotBlank(message = "to est obligatoire")
            @Pattern(regexp = CURRENCY_REGEX, message = "to " + CURRENCY_MSG)
            String to,

            @Schema(example = "655.957")
            @NotNull(message = "rate est obligatoire")
            @DecimalMin(value = "0.00000001", message = "rate doit être strictement positif")
            BigDecimal rate
    ) {
    }

    @Schema(description = "Mise à jour d'un taux stocké localement")
    public record UpdateRateRequest(
            @Schema(example = "EUR")
            @NotBlank(message = "from est obligatoire")
            @Pattern(regexp = CURRENCY_REGEX, message = "from " + CURRENCY_MSG)
            String from,

            @Schema(example = "XOF")
            @NotBlank(message = "to est obligatoire")
            @Pattern(regexp = CURRENCY_REGEX, message = "to " + CURRENCY_MSG)
            String to,

            @Schema(example = "656.5")
            @NotNull(message = "rate est obligatoire")
            @DecimalMin(value = "0.00000001", message = "rate doit être strictement positif")
            BigDecimal rate
    ) {
    }

    public record ConversionResponse(
            Long transactionId,
            BigDecimal amount,
            String from,
            String to,
            BigDecimal rate,
            BigDecimal convertedAmount,
            String source,
            LocalDateTime date
    ) {
        public static ConversionResponse from(CurrencyTransaction t) {
            return new ConversionResponse(t.getId(), t.getAmount(), t.getFromCurrency(), t.getToCurrency(),
                    t.getRate(), t.getConvertedAmount(), t.getSource().name(), t.getDate());
        }
    }

    public record RateResponse(Long id, String from, String to, BigDecimal rate, LocalDateTime updatedAt) {
        public static RateResponse from(ExchangeRate r) {
            return new RateResponse(r.getId(), r.getBaseCurrency(), r.getTargetCurrency(), r.getRate(), r.getUpdatedAt());
        }
    }
}
