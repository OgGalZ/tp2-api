package fr.efrei.tp2.calculator;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "operations")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperatorType operateur;

    @Column(nullable = false)
    private double a;

    @Column(nullable = false)
    private double b;

    @Column(nullable = false)
    private double resultat;

    @Column(nullable = false)
    private LocalDateTime date;

    protected Operation() {
    }

    public Operation(OperatorType operateur, double a, double b, double resultat) {
        this.operateur = operateur;
        this.a = a;
        this.b = b;
        this.resultat = resultat;
        this.date = LocalDateTime.now();
    }

    public void update(OperatorType operateur, double a, double b, double resultat) {
        this.operateur = operateur;
        this.a = a;
        this.b = b;
        this.resultat = resultat;
        this.date = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public OperatorType getOperateur() { return operateur; }
    public double getA() { return a; }
    public double getB() { return b; }
    public double getResultat() { return resultat; }
    public LocalDateTime getDate() { return date; }
}
