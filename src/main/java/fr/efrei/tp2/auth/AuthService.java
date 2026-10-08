package fr.efrei.tp2.auth;

import fr.efrei.tp2.auth.AuthDtos.*;
import fr.efrei.tp2.common.ApiException;
import fr.efrei.tp2.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenExpirationMs;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ApiException.Conflict("Ce nom d'utilisateur est déjà utilisé");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException.Conflict("Cet email est déjà utilisé");
        }
        User user = userRepository.save(new User(req.username(), passwordEncoder.encode(req.password()), req.email()));
        return UserResponse.from(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                // Même message dans les deux cas : on ne révèle pas si le username existe
                .orElseThrow(() -> new ApiException.Unauthorized("Nom d'utilisateur ou mot de passe incorrect"));
        return issueTokens(user);
    }

    /**
     * Renouvelle le jeton d'accès sans redemander le mot de passe.
     * Rotation : l'ancien refresh token est révoqué, un nouveau est délivré.
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .orElseThrow(() -> new ApiException.Unauthorized("Refresh token invalide"));
        if (!stored.isUsable()) {
            throw new ApiException.Unauthorized("Refresh token expiré ou révoqué : reconnectez-vous");
        }
        stored.revoke();
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(RefreshRequest req) {
        refreshTokenRepository.findByToken(req.refreshToken()).ifPresent(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException.NotFound("Utilisateur introuvable"));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshValue = generateOpaqueToken();
        refreshTokenRepository.save(new RefreshToken(refreshValue, user,
                Instant.now().plusMillis(refreshTokenExpirationMs)));
        return new TokenResponse(accessToken, refreshValue, "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000);
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
