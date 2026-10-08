package fr.efrei.tp2.bmi;

/** Catégories OMS de l'indice de masse corporelle (adulte). */
public enum BmiCategory {
    INSUFFISANCE_PONDERALE("Insuffisance pondérale", "BMI < 18,5"),
    NORMAL("Corpulence normale", "18,5 ≤ BMI < 25"),
    SURPOIDS("Surpoids", "25 ≤ BMI < 30"),
    OBESITE("Obésité", "BMI ≥ 30");

    private final String label;
    private final String range;

    BmiCategory(String label, String range) {
        this.label = label;
        this.range = range;
    }

    public static BmiCategory of(double bmi) {
        if (bmi < 18.5) return INSUFFISANCE_PONDERALE;
        if (bmi < 25) return NORMAL;
        if (bmi < 30) return SURPOIDS;
        return OBESITE;
    }

    public String getLabel() { return label; }
    public String getRange() { return range; }
}
