package fr.efrei.tp2.bmi;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "bmi_records", indexes = @Index(name = "idx_bmi_user", columnList = "user_id"))
public class BMIRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Poids en kg */
    @Column(nullable = false)
    private double weight;

    /** Taille en mètres */
    @Column(nullable = false)
    private double height;

    @Column(nullable = false)
    private double bmi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BmiCategory category;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected BMIRecord() {
    }

    public BMIRecord(Long userId, double weight, double height) {
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        apply(weight, height);
    }

    public void update(double weight, double height) {
        apply(weight, height);
        this.updatedAt = LocalDateTime.now();
    }

    /** BMI = poids (kg) / taille² (m), arrondi à 2 décimales. */
    public static double computeBmi(double weight, double height) {
        return BigDecimal.valueOf(weight / (height * height)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void apply(double weight, double height) {
        this.weight = weight;
        this.height = height;
        this.bmi = computeBmi(weight, height);
        this.category = BmiCategory.of(this.bmi);
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public double getWeight() { return weight; }
    public double getHeight() { return height; }
    public double getBmi() { return bmi; }
    public BmiCategory getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
