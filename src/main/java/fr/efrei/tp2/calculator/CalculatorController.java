package fr.efrei.tp2.calculator;

import fr.efrei.tp2.calculator.CalculatorDtos.CalculatorRequest;
import fr.efrei.tp2.calculator.CalculatorDtos.OperationResponse;
import fr.efrei.tp2.common.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calculator")
@Tag(name = "1. Calculatrice", description = "Opérations mathématiques avec historique persistant")
public class CalculatorController {

    private final CalculatorService service;

    public CalculatorController(CalculatorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Effectuer un calcul",
            description = "Valide a, b et l'opération, calcule le résultat puis enregistre l'opération dans l'historique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Calcul effectué et enregistré"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou division par zéro",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public OperationResponse calculate(@Valid @RequestBody CalculatorRequest request) {
        return service.calculate(request);
    }

    @GetMapping("/history")
    @Operation(summary = "Historique paginé des opérations",
            description = "Exemple : ?page=0&size=10&sort=date,desc. La réponse contient le contenu et les métadonnées de pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'historique"),
            @ApiResponse(responseCode = "400", description = "Paramètre de tri invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public Page<OperationResponse> history(
            @ParameterObject @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.history(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une opération par son id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opération trouvée"),
            @ApiResponse(responseCode = "404", description = "Opération introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public OperationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une opération", description = "Recalcule le résultat avec les nouvelles valeurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opération mise à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Opération introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public OperationResponse update(@PathVariable Long id, @Valid @RequestBody CalculatorRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une opération de l'historique")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Opération supprimée"),
            @ApiResponse(responseCode = "404", description = "Opération introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
