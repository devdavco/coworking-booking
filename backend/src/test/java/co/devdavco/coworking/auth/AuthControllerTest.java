package co.devdavco.coworking.auth;

import co.devdavco.coworking.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegrationTest {

    private void registrar(String nombre, String email, String password, int statusEsperado) throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nombre", nombre, "email", email, "password", password))))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void registraUnUsuarioSinExponerLaContrasena() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","email":"Ana@Mail.com","password":"clave-segura-123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(usuarios.findByEmail("ana@mail.com").orElseThrow().getPasswordHash())
                .startsWith("$2a$")
                .doesNotContain("clave-segura-123");
    }

    @Test
    void rechazaEmailDuplicadoSinImportarMayusculas() throws Exception {
        registrar("Ana", "ana@mail.com", CLAVE, 201);
        registrar("Otra Ana", "ANA@MAIL.COM", CLAVE, 409);
    }

    @Test
    void rechazaContrasenaCorta() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","email":"ana@mail.com","password":"corta"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void rechazaContrasenaDeMasDe72Bytes() throws Exception {
        // 40 caracteres pero 80 bytes: pasa @Size y debe fallar por el límite de BCrypt, con 400 y no con 500.
        registrar("Ana", "ana@mail.com", "ñ".repeat(40), 400);
    }

    @Test
    void elTokenDelLoginDaAccesoALaApi() throws Exception {
        registrar("Ana", "ana@mail.com", CLAVE, 201);

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"ANA@mail.com","password":"clave-segura-123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEn").isNotEmpty())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/api/espacios").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        registrar("Ana", "ana@mail.com", CLAVE, 201);

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"ana@mail.com","password":"otra-clave-123"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciales inválidas"));
    }

    @Test
    void loginConEmailInexistenteDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"nadie@mail.com","password":"clave-segura-123"}"""))
                .andExpect(status().isUnauthorized());
    }
}
