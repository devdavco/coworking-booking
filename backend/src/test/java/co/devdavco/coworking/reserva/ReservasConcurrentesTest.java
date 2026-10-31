package co.devdavco.coworking.reserva;

import co.devdavco.coworking.IntegrationTest;
import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.EspacioRepository;
import co.devdavco.coworking.espacio.TipoEspacio;
import co.devdavco.coworking.usuario.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ReservasConcurrentesTest extends IntegrationTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final int PETICIONES = 8;

    @Autowired
    EspacioRepository espacios;

    @Autowired
    ReservaRepository reservas;

    @Test
    void deVariasReservasSimultaneasParaLaMismaFranjaSoloUnaSeConfirma() throws Exception {
        String token = tokenUsuario();
        long salaId = espacios.save(new Espacio("Sala Andes", TipoEspacio.SALA_REUNIONES, 8, 15)).getId();
        OffsetDateTime inicio = LocalDate.now(BOGOTA).plusDays(1).atTime(9, 0).atZone(BOGOTA).toOffsetDateTime();
        String cuerpo = """
                {"espacioId":%d,"horaInicio":"%s","horaFin":"%s"}""".formatted(salaId, inicio, inicio.plusHours(1));

        ExecutorService hilos = Executors.newFixedThreadPool(PETICIONES);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> respuestas = new ArrayList<>();
        for (int i = 0; i < PETICIONES; i++) {
            respuestas.add(hilos.submit(() -> {
                salida.await();
                return mvc.perform(post("/api/reservas")
                                .header(AUTHORIZATION, token)
                                .contentType(APPLICATION_JSON)
                                .content(cuerpo))
                        .andReturn().getResponse().getStatus();
            }));
        }
        salida.countDown();

        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> respuesta : respuestas) {
            estados.add(respuesta.get());
        }
        hilos.shutdown();

        assertThat(estados).filteredOn(estado -> estado == 201).hasSize(1);
        assertThat(estados).filteredOn(estado -> estado == 409).hasSize(PETICIONES - 1);
        assertThat(reservas.count()).isEqualTo(1);
    }
}
