package fr.efrei.tp2.security;

/** Utilisateur authentifié extrait du JWT (placé dans le SecurityContext). */
public record AuthenticatedUser(Long id, String username) {
}
