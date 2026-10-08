package fr.efrei.tp2.currency;

import fr.efrei.tp2.common.ApiError;
import fr.efrei.tp2.currency.CurrencyDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/currency")
@Tag(name = "2. Devises", description = "Conversion de devises avec taux dynamiques, cache, retry et rate limiting")
public class CurrencyConverterController {

    private static final String CURRENCY_REGEX = "^[A-Za-z]{3}$";

    private final CurrencyService service;

    public CurrencyConverterController(CurrencyService service) {
        this.service = service;
    }

    @GetMapping("/convert")
    @Operation(summary = "Convertir un montant (taux dynamique)",
            description = "Utilise un taux local s'il existe, sinon l'API externe frankfurter (mise en cache 10 min). "
                    + "Limité à 20 requêtes par minute et par adresse IP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversion effectuée et enregistrée"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides ou devise non prise en charge",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Trop de requêtes (limite de débit atteinte)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "API externe indisponible après 3 tentatives",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ConversionResponse convert(
            @Parameter(example = "100")
            @RequestParam
            @NotNull @DecimalMin(value = "0.01", message = "amount doit être supérieur à 0")
            @DecimalMax(value = "1000000000", message = "amount est trop élevé") BigDecimal amount,
            @Parameter(example = "EUR")
            @RequestParam @Pattern(regexp = CURRENCY_REGEX, message = "from doit être un code devise de 3 lettres") String from,
            @Parameter(example = "USD")
            @RequestParam @Pattern(regexp = CURRENCY_REGEX, message = "to doit être un code devise de 3 lettres") String to) {
        return service.convert(amount, from, to);
    }

    @PostMapping("/convert")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convertir avec un taux manuel",
            description = "Pour les devises non couvertes par l'API externe. Le taux est mémorisé localement.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Taux enregistré et conversion effectuée"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ConversionResponse convertWithManualRate(@Valid @RequestBody ManualConversionRequest request) {
        return service.convertWithManualRate(request);
    }

    @PutMapping("/update-rate")
    @Operation(summary = "Mettre à jour un taux stocké localement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Taux mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Aucun taux local pour cette paire",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public RateResponse updateRate(@Valid @RequestBody UpdateRateRequest request) {
        return service.updateRate(request);
    }

    @DeleteMapping("/delete-transaction/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction supprimée"),
            @ApiResponse(responseCode = "404", description = "Transaction introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void deleteTransaction(@PathVariable Long id) {
        service.deleteTransaction(id);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Lister les transactions (paginé)")
    public Page<ConversionResponse> transactions(
            @ParameterObject @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.transactions(pageable);
    }

    @GetMapping("/rates")
    @Operation(summary = "Lister les taux stockés localement")
    public List<RateResponse> rates() {
        return service.localRates();
    }
}
