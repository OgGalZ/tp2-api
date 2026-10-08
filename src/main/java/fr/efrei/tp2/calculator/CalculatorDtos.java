package fr.efrei.tp2.calculator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class CalculatorDtos {

    private CalculatorDtos() {
    }

    @Schema(description = "Requête de calcul")
    public record CalculatorRequest(
            @Schema(example = "12.5")
            @NotNull(message = "a est obligatoire")
            @DecimalMin(value = "-1000000", message = "a doit être supérieur ou égal à -1 000 000")
            @DecimalMax(value = "1000000", message = "a doit être inférieur ou égal à 1 000 000")
            Double a,

            @Schema(example = "4")
            @NotNull(message = "b est obligatoire")
            @DecimalMin(value = "-1000000", message = "b doit être supérieur ou égal à -1 000 000")
            @DecimalMax(value = "1000000", message = "b doit être inférieur ou égal à 1 000 000")
            Double b,

            @Schema(example = "division", allowableValues = {"addition", "soustraction", "multiplication", "division"})
            @NotBlank(message = "operation est obligatoire")
            String operation
    ) {
    }

    public record OperationResponse(
            Long id,
            String operateur,
            double a,
            double b,
            double resultat,
            LocalDateTime date
    ) {
        public static OperationResponse from(Operation op) {
            return new OperationResponse(op.getId(), op.getOperateur().name().toLowerCase(),
                    op.getA(), op.getB(), op.getResultat(), op.getDate());
        }
    }
}
