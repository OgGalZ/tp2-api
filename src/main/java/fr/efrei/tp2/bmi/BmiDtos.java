package fr.efrei.tp2.bmi;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public final class BmiDtos {

    private BmiDtos() {
    }

    @Schema(description = "Poids en kg et taille en mètres")
    public record BmiRequest(
            @Schema(example = "72.5")
            @NotNull(message = "weight est obligatoire")
            @DecimalMin(value = "2", message = "weight doit être au moins 2 kg")
            @DecimalMax(value = "500", message = "weight doit être au plus 500 kg")
            Double weight,

            @Schema(example = "1.78")
            @NotNull(message = "height est obligatoire")
            @DecimalMin(value = "0.4", message = "height doit être au moins 0,4 m")
            @DecimalMax(value = "2.8", message = "height doit être au plus 2,8 m (en mètres, pas en cm)")
            Double height
    ) {
    }

    public record BmiResponse(
            Long id,
            Long userId,
            double weight,
            double height,
            double bmi,
            String category,
            String categoryLabel,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static BmiResponse from(BMIRecord r) {
            return new BmiResponse(r.getId(), r.getUserId(), r.getWeight(), r.getHeight(), r.getBmi(),
                    r.getCategory().name(), r.getCategory().getLabel(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }

    public record CategoryStat(String label, String range, long count, double percentage) {
    }

    public record BmiStatistics(
            long totalRecords,
            long totalUsers,
            Double averageBmi,
            Double minBmi,
            Double maxBmi,
            Map<String, CategoryStat> distribution,
            long myRecords,
            Double myAverageBmi
    ) {
    }
}
