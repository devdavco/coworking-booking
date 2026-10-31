package co.devdavco.coworking.espacio;

import co.devdavco.coworking.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EspacioControllerTest extends IntegrationTest {

    private static final String SALA_ANDES = """
            {"nombre":"Sala Andes","tipo":"SALA_REUNIONES","capacidad":8,"minutosLimpieza":15}""";
    private static final String CABINA = """
            {"nombre":"Cabina 1","tipo":"CABINA","capacidad":1,"minutosLimpieza":0}""";

    private String admin;

    @BeforeEach
    void autenticar() {
        // Todavía no hay seguridad: el header se ignora. La Tarea 4 cambia esto por un token real.
        admin = "Bearer sin-seguridad";
    }

    private long crearEspacio(String json) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/espacios")
                        .header(AUTHORIZATION, admin)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void creaUnEspacioYDevuelveSuUbicacion() throws Exception {
        mvc.perform(post("/api/espacios")
                        .header(AUTHORIZATION, admin)
                        .contentType(APPLICATION_JSON)
                        .content(SALA_ANDES))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/espacios/")))
                .andExpect(jsonPath("$.nombre").value("Sala Andes"))
                .andExpect(jsonPath("$.tipo").value("SALA_REUNIONES"))
                .andExpect(jsonPath("$.minutosLimpieza").value(15));
    }

    @Test
    void rechazaDatosInvalidosIndicandoCadaCampo() throws Exception {
        mvc.perform(post("/api/espacios")
                        .header(AUTHORIZATION, admin)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nombre":"","tipo":"CABINA","capacidad":0,"minutosLimpieza":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.capacidad").exists())
                .andExpect(jsonPath("$.errores.minutosLimpieza").exists());
    }

    @Test
    void rechazaUnTipoDesconocido() throws Exception {
        mvc.perform(post("/api/espacios")
                        .header(AUTHORIZATION, admin)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nombre":"Piscina","tipo":"PISCINA","capacidad":5,"minutosLimpieza":0}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaNombreDuplicadoSinImportarMayusculas() throws Exception {
        crearEspacio(SALA_ANDES);

        mvc.perform(post("/api/espacios")
                        .header(AUTHORIZATION, admin)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nombre":"sala andes","tipo":"CABINA","capacidad":2,"minutosLimpieza":0}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto"));
    }

    @Test
    void listaSoloLosActivosOrdenadosPorNombre() throws Exception {
        crearEspacio(SALA_ANDES);
        long cabina = crearEspacio(CABINA);
        crearEspacio("""
                {"nombre":"Escritorio 7","tipo":"ESCRITORIO","capacidad":1,"minutosLimpieza":5}""");

        mvc.perform(delete("/api/espacios/{id}", cabina).header(AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/espacios").header(AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Escritorio 7"))
                .andExpect(jsonPath("$[1].nombre").value("Sala Andes"));
    }

    @Test
    void obtenerUnEspacioInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/espacios/{id}", 999).header(AUTHORIZATION, admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void actualizaUnEspacio() throws Exception {
        long id = crearEspacio(SALA_ANDES);

        mvc.perform(put("/api/espacios/{id}", id)
                        .header(AUTHORIZATION, admin)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sala Andes","tipo":"SALA_REUNIONES","capacidad":10,"minutosLimpieza":20}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidad").value(10))
                .andExpect(jsonPath("$.minutosLimpieza").value(20));
    }

    @Test
    void unEspacioDesactivadoYaNoSePuedeConsultar() throws Exception {
        long id = crearEspacio(SALA_ANDES);

        mvc.perform(delete("/api/espacios/{id}", id).header(AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/espacios/{id}", id).header(AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }
}
