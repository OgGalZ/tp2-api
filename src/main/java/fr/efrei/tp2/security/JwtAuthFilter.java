package fr.efrei.tp2.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit l'en-tête « Authorization: Bearer &lt;jwt&gt; », valide le jeton et authentifie la requête.
 * Volontairement non déclaré comme @Component : il n'est inséré que dans la chaîne Spring Security.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            try {
                AuthenticatedUser user = jwtService.parseAccessToken(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "Jeton expiré : utilisez /api/auth/refresh pour en obtenir un nouveau");
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "Jeton invalide");
            }
        }
        chain.doFilter(request, response);
    }
}
