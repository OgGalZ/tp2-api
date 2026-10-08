package fr.efrei.tp2;

import com.jayway.jsonpath.JsonPath;
import fr.efrei.tp2.currency.ExchangeRateClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base commune : contexte Spring complet + MockMvc + base H2 en mémoire (profil « test »).
 * L'API externe de taux de change est simulée pour que les tests soient rapides et déterministes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected ExchangeRateClient exchangeRateClient;

    protected static String uniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Inscrit puis connecte un utilisateur ; renvoie la réponse JSON de /login. */
    protected String registerAndLogin(String username) throws Exception {
        String password = "MotDePasse123!";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@test.fr","password":"%s"}
                                """.formatted(username, username, password)))
                .andExpect(status().isCreated());

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected String accessTokenOf(String loginJson) {
        return JsonPath.read(loginJson, "$.accessToken");
    }

    protected String refreshTokenOf(String loginJson) {
        return JsonPath.read(loginJson, "$.refreshToken");
    }
}
