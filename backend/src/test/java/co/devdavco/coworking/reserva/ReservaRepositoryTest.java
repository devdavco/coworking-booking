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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba la exclusion constraint directamente, sin pasar por el servicio. */
class ReservaRepositoryTest extends IntegrationTest {

    private static final Instant NUEVE = Instant.parse("2026-11-03T14:00:00Z");
    private static final Instant DIEZ = Instant.parse("2026-11-03T15:00:00Z");
    private static final Instant DIEZ_Y_CUARTO = Instant.parse("2026-11-03T15:15:00Z");
    private static final Instant ONCE = Instant.parse("2026-11-03T16:00:00Z");

    @Autowired
    ReservaRepository reservas;

    @Autowired
    EspacioRepository espacios;

    private Usuario ana;
    private Espacio sala;

    @BeforeEach
    void datos() {
        ana = crearUsuario("ana@test.com", Rol.USUARIO);
        sala = espacios.save(new Espacio("Sala Andes", TipoEspacio.SALA_REUNIONES, 8, 15));
    }

    @Test
    void laBaseImpideDosReservasConfirmadasQueSeCruzan() {
        reservas.saveAndFlush(new Reserva(ana, sala, NUEVE, DIEZ, DIEZ_Y_CUARTO, NUEVE));

        assertThatThrownBy(() -> reservas.saveAndFlush(new Reserva(ana, sala, DIEZ, ONCE, ONCE, NUEVE)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reservas_sin_solapamiento");
    }

    @Test
    void laBaseAceptaUnaReservaQueEmpiezaJustoAlTerminarLaLimpieza() {
        reservas.saveAndFlush(new Reserva(ana, sala, NUEVE, DIEZ, DIEZ_Y_CUARTO, NUEVE));

        reservas.saveAndFlush(new Reserva(ana, sala, DIEZ_Y_CUARTO, ONCE, ONCE.plusSeconds(900), NUEVE));

        assertThat(reservas.count()).isEqualTo(2);
    }

    @Test
    void lasReservasCanceladasNoBloquean() {
        Reserva cancelada = new Reserva(ana, sala, NUEVE, DIEZ, DIEZ_Y_CUARTO, NUEVE);
        cancelada.cancelar();
        reservas.saveAndFlush(cancelada);

        reservas.saveAndFlush(new Reserva(ana, sala, NUEVE, DIEZ, DIEZ_Y_CUARTO, NUEVE));

        assertThat(reservas.count()).isEqualTo(2);
    }

    @Test
    void findSolapadasEncuentraSoloLasQueSeCruzan() {
        reservas.saveAndFlush(new Reserva(ana, sala, NUEVE, DIEZ, DIEZ_Y_CUARTO, NUEVE));

        assertThat(reservas.findSolapadas(sala.getId(), EstadoReserva.CONFIRMADA, DIEZ, ONCE)).hasSize(1);
        assertThat(reservas.findSolapadas(sala.getId(), EstadoReserva.CONFIRMADA, DIEZ_Y_CUARTO, ONCE)).isEmpty();
    }
}
