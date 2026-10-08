package fr.efrei.tp2.bmi;

import fr.efrei.tp2.bmi.BmiDtos.*;
import fr.efrei.tp2.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BmiService {

    private final BMIRecordRepository repository;

    public BmiService(BMIRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BmiResponse calculate(Long userId, BmiRequest req) {
        BMIRecord saved = repository.save(new BMIRecord(userId, req.weight(), req.height()));
        return BmiResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<BmiResponse> history(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(BmiResponse::from);
    }

    @Transactional
    public BmiResponse update(Long userId, Long recordId, BmiRequest req) {
        BMIRecord record = getOwnedRecord(userId, recordId);
        record.update(req.weight(), req.height());
        return BmiResponse.from(record);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        repository.delete(getOwnedRecord(userId, recordId));
    }

    @Transactional(readOnly = true)
    public BmiStatistics statistics(Long userId) {
        long total = repository.count();

        Map<BmiCategory, Long> counts = new EnumMap<>(BmiCategory.class);
        for (BmiCategory c : BmiCategory.values()) {
            counts.put(c, 0L);
        }
        for (Object[] row : repository.countByCategory()) {
            counts.put((BmiCategory) row[0], ((Number) row[1]).longValue());
        }

        Map<String, CategoryStat> distribution = new LinkedHashMap<>();
        counts.forEach((category, count) -> distribution.put(category.name(),
                new CategoryStat(category.getLabel(), category.getRange(), count,
                        total == 0 ? 0 : round(100.0 * count / total))));

        return new BmiStatistics(
                total,
                repository.countDistinctUsers(),
                round(repository.globalAverageBmi()),
                repository.globalMinBmi(),
                repository.globalMaxBmi(),
                distribution,
                repository.countByUserId(userId),
                round(repository.userAverageBmi(userId)));
    }

    /**
     * Autorisation (et pas seulement authentification) :
     * 404 si l'enregistrement n'existe pas, 403 s'il appartient à un autre utilisateur.
     */
    private BMIRecord getOwnedRecord(Long userId, Long recordId) {
        BMIRecord record = repository.findById(recordId)
                .orElseThrow(() -> new ApiException.NotFound("Enregistrement BMI introuvable avec l'id " + recordId));
        if (!record.isOwnedBy(userId)) {
            throw new ApiException.Forbidden("Vous n'êtes pas le propriétaire de cet enregistrement BMI");
        }
        return record;
    }

    private static Double round(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
