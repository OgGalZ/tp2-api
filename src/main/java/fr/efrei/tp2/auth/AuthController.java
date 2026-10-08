package fr.efrei.tp2.auth;

import fr.efrei.tp2.auth.AuthDtos.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "3a. Authentification", description = "Inscription, connexion JWT et refresh tokens")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte", description = "Le mot de passe est haché avec BCrypt avant stockage.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Username ou email déjà utilisé",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter", description = "Renvoie un JWT d'accès (15 min) et un refresh token (7 jours).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie"),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouveler le jeton d'accès",
            description = "Échange un refresh token valide contre une nouvelle paire de jetons (rotation).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveaux jetons délivrés"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide, expiré ou révoqué",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Se déconnecter (révoque le refresh token)")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.me(user.id());
    }
}
