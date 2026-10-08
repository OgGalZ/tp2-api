package fr.efrei.tp2.auth;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Refresh token opaque (aléatoire), stocké en base pour pouvoir être révoqué.
 * Rotation : chaque utilisation le révoque et en délivre un nouveau.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected RefreshToken() {
    }

    public RefreshToken(String token, User user, Instant expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public boolean isUsable() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
}
