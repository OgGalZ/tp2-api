package fr.efrei.tp2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Exercice 1 - Calculatrice")
class CalculatorApiTest extends AbstractApiTest {

    @Test
    @DisplayName("POST /api/calculator avec des données valides -> 201 + résultat")
    void calculate_success() throws Exception {
        mockMvc.perform(post("/api/calculator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"a": 12.5, "b": 4, "operation": "addition"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.operateur").value("addition"))
                .andExpect(jsonPath("$.resultat").value(16.5));
    }

    @Test
    @DisplayName("Division par zéro -> 400 avec message explicite au format ApiError")
    void divisionByZero_returns400() throws Exception {
        mockMvc.perform(post("/api/calculator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"a": 10, "b": 0, "operation": "division"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("La division par zéro n'est pas autorisée"))
                .andExpect(jsonPath("$.path").value("/api/calculator"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Validation @DecimalMax / @NotNull -> 400 avec le détail des champs")
    void validationError_returns400() throws Exception {
        mockMvc.perform(post("/api/calculator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"a": 5000000, "operation": "multiplication"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)))
                .andExpect(jsonPath("$.details[*]", hasItem(containsString("a doit être inférieur"))))
                .andExpect(jsonPath("$.details[*]", hasItem(containsString("b est obligatoire"))));
    }

    @Test
    @DisplayName("GET /history paginé -> contenu + métadonnées de pagination")
    void history_isPaginated() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/calculator")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"a\": %d, \"b\": 2, \"operation\": \"multiplication\"}".formatted(i)));
        }
        mockMvc.perform(get("/api/calculator/history?page=0&size=2&sort=date,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("PUT puis DELETE sur une opération, puis 404")
    void updateThenDelete() throws Exception {
        String json = mockMvc.perform(post("/api/calculator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\": 3, \"b\": 3, \"operation\": \"addition\"}"))
                .andReturn().getResponse().getContentAsString();
        Integer id = com.jayway.jsonpath.JsonPath.read(json, "$.id");

        mockMvc.perform(put("/api/calculator/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\": 3, \"b\": 3, \"operation\": \"multiplication\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultat").value(9.0));

        mockMvc.perform(delete("/api/calculator/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/calculator/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
