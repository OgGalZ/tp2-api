package fr.efrei.tp2.bmi;

import fr.efrei.tp2.bmi.BmiDtos.*;
import fr.efrei.tp2.common.ApiError;
import fr.efrei.tp2.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bmi")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "3b. BMI", description = "Calcul et suivi du BMI (JWT obligatoire)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "JWT absent, invalide ou expiré",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class BMICalculatorController {

    private final BmiService service;

    public BMICalculatorController(BmiService service) {
        this.service = service;
    }

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Calculer et enregistrer son BMI", description = "BMI = poids (kg) / taille² (m).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "BMI calculé et enregistré"),
            @ApiResponse(responseCode = "400", description = "Poids ou taille invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public BmiResponse calculate(@AuthenticationPrincipal AuthenticatedUser user,
                                 @Valid @RequestBody BmiRequest request) {
        return service.calculate(user.id(), request);
    }

    @GetMapping("/history")
    @Operation(summary = "Historique BMI de l'utilisateur connecté (paginé)")
    public Page<BmiResponse> history(@AuthenticationPrincipal AuthenticatedUser user,
                                     @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                             direction = Sort.Direction.DESC) Pageable pageable) {
        return service.history(user.id(), pageable);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Mettre à jour poids/taille et recalculer le BMI")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enregistrement mis à jour"),
            @ApiResponse(responseCode = "403", description = "Enregistrement d'un autre utilisateur",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Enregistrement introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public BmiResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                              @PathVariable Long id,
                              @Valid @RequestBody BmiRequest request) {
        return service.update(user.id(), id, request);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un enregistrement (propriétaire uniquement)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Enregistrement supprimé"),
            @ApiResponse(responseCode = "403", description = "Enregistrement d'un autre utilisateur",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Enregistrement introuvable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Statistiques globales",
            description = "BMI moyen/min/max, répartition par catégorie OMS, et statistiques personnelles.")
    public BmiStatistics statistics(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.statistics(user.id());
    }
}
