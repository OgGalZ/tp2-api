package fr.efrei.tp2.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @Schema(example = "enzo")
            @NotBlank(message = "username est obligatoire")
            @Size(min = 3, max = 50, message = "username doit contenir entre 3 et 50 caractères")
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "username ne peut contenir que lettres, chiffres, . _ -")
            String username,

            @Schema(example = "enzo@efrei.net")
            @NotBlank(message = "email est obligatoire")
            @Email(message = "email doit être une adresse valide")
            String email,

            @Schema(example = "MotDePasse123!")
            @NotBlank(message = "password est obligatoire")
            @Size(min = 8, max = 72, message = "password doit contenir entre 8 et 72 caractères")
            String password
    ) {
    }

    public record LoginRequest(
            @Schema(example = "enzo") @NotBlank(message = "username est obligatoire") String username,
            @Schema(example = "MotDePasse123!") @NotBlank(message = "password est obligatoire") String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank(message = "refreshToken est obligatoire") String refreshToken
    ) {
    }

    public record UserResponse(Long id, String username, String email, LocalDateTime createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getCreatedAt());
        }
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {
    }
}
