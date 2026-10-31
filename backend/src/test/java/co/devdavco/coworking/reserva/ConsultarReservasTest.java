package co.devdavco.coworking.reserva;

import co.devdavco.coworking.IntegrationTest;
import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.EspacioRepository;
import co.devdavco.coworking.espacio.TipoEspacio;
import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsultarReservasTest extends IntegrationTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    EspacioRepository espacios;

    @Autowired
    ReservaRepository reservas;

    private Usuario ana;
    private String tokenAna;
    private String tokenBeto;
    private String tokenAdmin;
    private Espacio sala;
    private Espacio cabina;

    @BeforeEach
    void datos() {
        ana = crearUsuario("ana@test.com", Rol.USUARIO);
        tokenAna = tokenDe(ana);
        tokenBeto = tokenDe(crearUsuario("beto@test.com", Rol.USUARIO));
        tokenAdmin = tokenAdmin();
        sala = espacios.save(new Espacio("Sala Andes", TipoEspacio.SALA_REUNIONES, 8, 15));
        cabina = espacios.save(new Espacio("Cabina 1", TipoEspacio.CABINA, 1, 0));
    }

    private static LocalDate manana() {
        return LocalDate.now(BOGOTA).plusDays(1);
    }

    private static OffsetDateTime manana(int hora, int minuto) {
        return manana().atTime(hora, minuto).atZone(BOGOTA).toOffsetDateTime();
    }

    private long reservar(String token, Espacio espacio, int horaInicio, int horaFin) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/reservas")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"espacioId":%d,"horaInicio":"%s","horaFin":"%s"}"""
                                .formatted(espacio.getId(), manana(horaInicio, 0), manana(horaFin, 0))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void misReservasSoloIncluyeLasPropias() throws Exception {
        reservar(tokenAna, sala, 9, 10);
        reservar(tokenBeto, sala, 11, 12);

        mvc.perform(get("/api/reservas/mias").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuarioId").value(ana.getId()));
    }

    @Test
    void elAdminListaYFiltraPorEspacioYFecha() throws Exception {
        reservar(tokenAna, sala, 9, 10);
        reservar(tokenBeto, cabina, 9, 10);

        mvc.perform(get("/api/reservas").header(AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/reservas")
                        .param("espacioId", cabina.getId().toString())
                        .param("fecha", manana().toString())
                        .header(AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].espacioNombre").value("Cabina 1"));

        mvc.perform(get("/api/reservas")
                        .param("fecha", manana().plusDays(1).toString())
                        .header(AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unUsuarioNoPuedeListarTodasLasReservas() throws Exception {
        mvc.perform(get("/api/reservas").header(AUTHORIZATION, tokenAna))
                .andExpect(status().isForbidden());
    }

    @Test
    void laReservaDeOtroDa404YElAdminSiLaVe() throws Exception {
        long id = reservar(tokenAna, sala, 9, 10);

        mvc.perform(get("/api/reservas/{id}", id).header(AUTHORIZATION, tokenBeto))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/reservas/{id}", id).header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reservas/{id}", id).header(AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void cancelarLiberaElHorario() throws Exception {
        long id = reservar(tokenAna, sala, 9, 10);

        mvc.perform(post("/api/reservas/{id}/cancelar", id).header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        reservar(tokenBeto, sala, 9, 10);
    }

    @Test
    void cancelarDosVecesDevuelve409() throws Exception {
        long id = reservar(tokenAna, sala, 9, 10);
        mvc.perform(post("/api/reservas/{id}/cancelar", id).header(AUTHORIZATION, tokenAna))
                .andExpect(status().isOk());

        mvc.perform(post("/api/reservas/{id}/cancelar", id).header(AUTHORIZATION, tokenAna))
                .andExpect(status().isConflict());
    }

    @Test
    void elDuenoNoPuedeCancelarUnaReservaYaIniciada() throws Exception {
        Instant haceUnaHora = Instant.now().minusSeconds(3600);
        Reserva enCurso = reservas.save(new Reserva(ana, sala, haceUnaHora, haceUnaHora.plusSeconds(7200),
                haceUnaHora.plusSeconds(8100), haceUnaHora));

        mvc.perform(post("/api/reservas/{id}/cancelar", enCurso.getId()).header(AUTHORIZATION, tokenAna))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/reservas/{id}/cancelar", enCurso.getId()).header(AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void laOcupacionMuestraSoloLasReservasConfirmadasConSuLimpieza() throws Exception {
        reservar(tokenAna, sala, 9, 10);
        long cancelada = reservar(tokenBeto, sala, 14, 15);
        mvc.perform(post("/api/reservas/{id}/cancelar", cancelada).header(AUTHORIZATION, tokenBeto))
                .andExpect(status().isOk());

        MvcResult resultado = mvc.perform(get("/api/espacios/{id}/ocupacion", sala.getId())
                        .param("fecha", manana().toString())
                        .header(AUTHORIZATION, tokenBeto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        JsonNode franja = objectMapper.readTree(resultado.getResponse().getContentAsString()).get(0);
        assertThat(OffsetDateTime.parse(franja.get("hasta").asText()).toInstant())
                .isEqualTo(manana(10, 15).toInstant());
    }

    @Test
    void laOcupacionConUnaFechaFueraDeRangoDevuelve400() throws Exception {
        mvc.perform(get("/api/espacios/{id}/ocupacion", sala.getId())
                        .param("fecha", "+300000-01-01")
                        .header(AUTHORIZATION, tokenAna))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laOcupacionExigeLaFecha() throws Exception {
        mvc.perform(get("/api/espacios/{id}/ocupacion", sala.getId()).header(AUTHORIZATION, tokenAna))
                .andExpect(status().isBadRequest());
    }
}
