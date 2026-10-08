package fr.efrei.tp2;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Exercice 3 - BMI, JWT et autorisation")
class BmiSecurityTest extends AbstractApiTest {

    @Test
    @DisplayName("GET /api/bmi/history sans jeton -> 401")
    void history_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/bmi/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/bmi/history"));
    }

    @Test
    @DisplayName("Jeton altéré -> 401 « Jeton invalide »")
    void history_withTamperedToken_returns401() throws Exception {
        String token = accessTokenOf(registerAndLogin(uniqueName("tamper")));
        // On remplace la fin de la signature : le jeton n'est plus valide
        String tampered = token.substring(0, token.length() - 5) + "AAAAA";
        mockMvc.perform(get("/api/bmi/history").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Jeton invalide"));
    }

    @Test
    @DisplayName("Calcul, historique et statistiques avec un JWT valide")
    void calculate_history_statistics() throws Exception {
        String token = accessTokenOf(registerAndLogin(uniqueName("alice")));

        mockMvc.perform(post("/api/bmi/calculate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\": 70, \"height\": 1.75}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bmi").value(22.86))
                .andExpect(jsonPath("$.category").value("NORMAL"));

        mockMvc.perform(get("/api/bmi/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/bmi/statistics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.distribution.NORMAL.count", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.myRecords").value(1));
    }

    @Test
    @DisplayName("Supprimer l'enregistrement d'un autre utilisateur -> 403 ; le sien -> 204")
    void deleteOtherUsersRecord_returns403() throws Exception {
        String ownerToken = accessTokenOf(registerAndLogin(uniqueName("owner")));
        String intruderToken = accessTokenOf(registerAndLogin(uniqueName("intrus")));

        String json = mockMvc.perform(post("/api/bmi/calculate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\": 90, \"height\": 1.80}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(json, "$.id");

        mockMvc.perform(delete("/api/bmi/delete/" + id).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(put("/api/bmi/update/" + id)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\": 50, \"height\": 1.80}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/bmi/delete/" + id).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Refresh token : nouveau jeton délivré, ancien refresh token révoqué (rotation)")
    void refreshToken_rotation() throws Exception {
        String login = registerAndLogin(uniqueName("bob"));
        String refresh = refreshTokenOf(login);

        String refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn().getResponse().getContentAsString();

        // Le nouveau jeton d'accès fonctionne
        mockMvc.perform(get("/api/bmi/history").header("Authorization", "Bearer " + accessTokenOf(refreshed)))
                .andExpect(status().isOk());

        // L'ancien refresh token ne peut pas être réutilisé
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mauvais mot de passe -> 401")
    void login_wrongPassword_returns401() throws Exception {
        String username = uniqueName("carol");
        registerAndLogin(username);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"mauvais-mdp\"}"))
                .andExpect(status().isUnauthorized());
    }
}