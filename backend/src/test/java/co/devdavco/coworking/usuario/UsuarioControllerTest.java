package co.devdavco.coworking.usuario;

import co.devdavco.coworking.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest extends IntegrationTest {

    @Test
    void meDevuelveLosDatosDelUsuarioDelToken() throws Exception {
        Usuario ana = crearUsuario("ana@test.com", Rol.USUARIO);

        mvc.perform(get("/api/usuarios/me").header(AUTHORIZATION, tokenDe(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ana.getId()))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void elAdminVeTodosLosUsuarios() throws Exception {
        String admin = tokenAdmin();
        crearUsuario("ana@test.com", Rol.USUARIO);

        mvc.perform(get("/api/usuarios").header(AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void unUsuarioNoPuedeVerTodosLosUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").header(AUTHORIZATION, tokenUsuario()))
                .andExpect(status().isForbidden());
    }
}
