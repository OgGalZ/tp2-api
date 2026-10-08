package fr.efrei.tp2.currency;

import fr.efrei.tp2.common.ApiException;
import fr.efrei.tp2.currency.CurrencyDtos.*;
import fr.efrei.tp2.currency.CurrencyTransaction.RateSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CurrencyService {

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateRepository rateRepository;
    private final CurrencyTransactionRepository transactionRepository;

    public CurrencyService(ExchangeRateService exchangeRateService,
                           ExchangeRateRepository rateRepository,
                           CurrencyTransactionRepository transactionRepository) {
        this.exchangeRateService = exchangeRateService;
        this.rateRepository = rateRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Conversion « dynamique ».
     * Un taux saisi localement est prioritaire ; sinon on interroge l'API externe (avec cache + retry).
     * {@code @Transactional} : la transaction n'est enregistrée que si la récupération du taux
     * ET le calcul ont réussi. Toute exception (503, 400...) provoque un rollback : rien n'est persisté.
     */
    @Transactional
    public ConversionResponse convert(BigDecimal amount, String fromRaw, String toRaw) {
        String from = normalize(fromRaw);
        String to = normalize(toRaw);

        BigDecimal rate;
        RateSource source;
        if (from.equals(to)) {
            rate = BigDecimal.ONE;
            source = RateSource.LOCAL;
        } else {
            Optional<ExchangeRate> local = rateRepository.findByBaseCurrencyAndTargetCurrency(from, to);
            if (local.isPresent()) {
                rate = local.get().getRate();
                source = RateSource.LOCAL;
            } else {
                rate = exchangeRateService.getExternalRate(from, to);
                source = RateSource.API;
            }
        }

        BigDecimal converted = amount.multiply(rate).setScale(4, RoundingMode.HALF_EVEN);
        CurrencyTransaction saved = transactionRepository.save(
                new CurrencyTransaction(amount, from, to, rate, converted, source));
        return ConversionResponse.from(saved);
    }

    /** Conversion avec un taux manuel : le taux est mémorisé (créé ou remplacé) puis utilisé. */
    @Transactional
    public ConversionResponse convertWithManualRate(ManualConversionRequest req) {
        String from = normalize(req.from());
        String to = normalize(req.to());
        if (from.equals(to)) {
            throw new ApiException.BadRequest("Les devises source et cible doivent être différentes");
        }

        ExchangeRate rate = rateRepository.findByBaseCurrencyAndTargetCurrency(from, to)
                .map(existing -> {
                    existing.updateRate(req.rate());
                    return existing;
                })
                .orElseGet(() -> rateRepository.save(new ExchangeRate(from, to, req.rate())));

        BigDecimal converted = req.amount().multiply(rate.getRate()).setScale(4, RoundingMode.HALF_EVEN);
        CurrencyTransaction saved = transactionRepository.save(
                new CurrencyTransaction(req.amount(), from, to, rate.getRate(), converted, RateSource.MANUAL));
        return ConversionResponse.from(saved);
    }

    @Transactional
    public RateResponse updateRate(UpdateRateRequest req) {
        String from = normalize(req.from());
        String to = normalize(req.to());
        ExchangeRate rate = rateRepository.findByBaseCurrencyAndTargetCurrency(from, to)
                .orElseThrow(() -> new ApiException.NotFound(
                        "Aucun taux local pour " + from + " -> " + to + ". Créez-le d'abord via POST /api/currency/convert."));
        rate.updateRate(req.rate());
        return RateResponse.from(rate);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        CurrencyTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ApiException.NotFound("Transaction introuvable avec l'id " + id));
        transactionRepository.delete(tx);
    }

    @Transactional(readOnly = true)
    public Page<ConversionResponse> transactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(ConversionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<RateResponse> localRates() {
        return rateRepository.findAll().stream().map(RateResponse::from).toList();
    }

    private String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
