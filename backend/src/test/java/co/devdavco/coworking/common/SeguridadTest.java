package co.devdavco.coworking.common;

import co.devdavco.coworking.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadTest extends IntegrationTest {

    @Test
    void sinTokenDevuelve401ConProblemDetail() throws Exception {
        mvc.perform(get("/api/espacios"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void unTokenConFirmaFalsaDevuelve401() throws Exception {
        String token = tokenUsuario();
        String falsificado = token.substring(0, token.lastIndexOf('.') + 1) + "firmaFalsa";

        mvc.perform(get("/api/espacios").header(AUTHORIZATION, falsificado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioNoPuedeCrearEspacios() throws Exception {
        mvc.perform(post("/api/espacios")
                        .header(AUTHORIZATION, tokenUsuario())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sala Andes","tipo":"SALA_REUNIONES","capacidad":8,"minutosLimpieza":15}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    @Test
    void headNoSaltaLasReglasDeAdmin() throws Exception {
        String usuario = tokenUsuario();

        mvc.perform(head("/api/usuarios").header(AUTHORIZATION, usuario))
                .andExpect(status().isForbidden());
        mvc.perform(head("/api/reservas").header(AUTHORIZATION, usuario))
                .andExpect(status().isForbidden());
    }

    @Test
    void unUsuarioPuedeListarEspacios() throws Exception {
        mvc.perform(get("/api/espacios").header(AUTHORIZATION, tokenUsuario()))
                .andExpect(status().isOk());
    }

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void corsPermiteElOrigenConfigurado() throws Exception {
        mvc.perform(options("/api/espacios")
                        .header(ORIGIN, "http://localhost:4200")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void corsRechazaOtrosOrigenes() throws Exception {
        mvc.perform(options("/api/espacios")
                        .header(ORIGIN, "https://otro-sitio.example")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
