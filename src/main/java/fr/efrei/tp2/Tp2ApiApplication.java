package fr.efrei.tp2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.retry.annotation.EnableRetry;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

// L'authentification est gérée par JWT (AuthService) : pas d'utilisateur en mémoire généré par défaut.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableCaching
@EnableRetry
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class Tp2ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(Tp2ApiApplication.class, args);
    }
}
