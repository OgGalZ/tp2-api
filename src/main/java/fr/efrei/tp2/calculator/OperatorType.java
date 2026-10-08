package fr.efrei.tp2.calculator;

import fr.efrei.tp2.common.ApiException;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum OperatorType {
    ADDITION,
    SOUSTRACTION,
    MULTIPLICATION,
    DIVISION;

    public static OperatorType from(String value) {
        return Arrays.stream(values())
                .filter(op -> op.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new ApiException.BadRequest(
                        "Opération inconnue : '" + value + "'. Valeurs acceptées : " + accepted()));
    }

    private static String accepted() {
        return Arrays.stream(values()).map(v -> v.name().toLowerCase()).collect(Collectors.joining(", "));
    }
}
