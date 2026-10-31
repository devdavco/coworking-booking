package co.devdavco.coworking.reserva;

import co.devdavco.coworking.IntegrationTest;
import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.EspacioRepository;
import co.devdavco.coworking.espacio.TipoEspacio;
import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrearReservaTest extends IntegrationTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    EspacioRepository espacios;

    private Usuario ana;
    private String tokenAna;
    private long salaId;

    @BeforeEach
    void datos() {
        ana = crearUsuario("ana@test.com", Rol.USUARIO);
        tokenAna = tokenDe(ana);
        salaId = espacios.save(new Espacio("Sala Andes", TipoEspacio.SALA_REUNIONES, 8, 15)).getId();
    }

    /** Mañana a la hora indicada en Bogotá: siempre está en el futuro. */
    private static OffsetDateTime manana(int hora, int minuto) {
        return LocalDate.now(BOGOTA).plusDays(1).atTime(hora, minuto).atZone(BOGOTA).toOffsetDateTime();
    }

    private ResultActions reservar(long espacioId, OffsetDateTime inicio, OffsetDateTime fin) throws Exception {
        return mvc.perform(post("/api/reservas")
                .header(AUTHORIZATION, tokenAna)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"espacioId":%d,"horaInicio":"%s","horaFin":"%s"}""".formatted(espacioId, inicio, fin)));
    }

    @Test
    void creaLaReservaYCalculaElFinConLimpieza() throws Exception {
        MvcResult resultado = reservar(salaId, manana(9, 0), manana(10, 0))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/reservas/")))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.finalizada").value(false))
                .andExpect(jsonPath("$.usuarioId").value(ana.getId()))
                .andExpect(jsonPath("$.espacioNombre").value("Sala Andes"))
                .andReturn();

        String finTotal = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("horaFinTotal").asText();
        assertThat(OffsetDateTime.parse(finTotal).toInstant()).isEqualTo(manana(10, 15).toInstant());
    }

    @Test
    void rechazaUnaReservaQueCaeEnLaLimpiezaDeOtra() throws Exception {
        reservar(salaId, manana(9, 0), manana(10, 0)).andExpect(status().isCreated());

        reservar(salaId, manana(10, 0), manana(11, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("09:00 a 10:15")));
    }

    @Test
    void aceptaUnaReservaQueEmpiezaJustoAlTerminarLaLimpieza() throws Exception {
        reservar(salaId, manana(9, 0), manana(10, 0)).andExpect(status().isCreated());

        reservar(salaId, manana(10, 15), manana(11, 0)).andExpect(status().isCreated());
    }

    @Test
    void rechazaUnaReservaFueraDelHorario() throws Exception {
        reservar(salaId, manana(21, 0), manana(22, 30))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regla de negocio incumplida"));
    }

    @Test
    void rechazaReservarUnEspacioInactivo() throws Exception {
        Espacio cabina = new Espacio("Cabina 1", TipoEspacio.CABINA, 1, 0);
        cabina.setActivo(false);
        long cabinaId = espacios.save(cabina).getId();

        reservar(cabinaId, manana(9, 0), manana(10, 0)).andExpect(status().isNotFound());
    }

    @Test
    void rechazaFechasSinZonaHoraria() throws Exception {
        mvc.perform(post("/api/reservas")
                        .header(AUTHORIZATION, tokenAna)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"espacioId":%d,"horaInicio":"2030-01-10T09:00:00","horaFin":"2030-01-10T10:00:00"}"""
                                .formatted(salaId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ignoraElUsuarioIdEnviadoPorElCliente() throws Exception {
        Usuario beto = crearUsuario("beto@test.com", Rol.USUARIO);

        mvc.perform(post("/api/reservas")
                        .header(AUTHORIZATION, tokenAna)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"espacioId":%d,"usuarioId":%d,"horaInicio":"%s","horaFin":"%s"}"""
                                .formatted(salaId, beto.getId(), manana(9, 0), manana(10, 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(ana.getId()));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(post("/api/reservas")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"espacioId":%d,"horaInicio":"%s","horaFin":"%s"}"""
                                .formatted(salaId, manana(9, 0), manana(10, 0))))
                .andExpect(status().isUnauthorized());
    }
}
