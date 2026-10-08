package fr.efrei.tp2.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corps d'erreur JSON unique renvoyé par toute l'API (format imposé par le TP).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ApiError of(HttpStatus status, String message, String path, List<String> details) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}
