package co.devdavco.coworking.reserva;

import co.devdavco.coworking.auth.UsuarioActual;
import co.devdavco.coworking.common.ConflictoException;
import co.devdavco.coworking.common.RecursoNoEncontradoException;
import co.devdavco.coworking.common.ReglaNegocioException;
import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.EspacioService;
import co.devdavco.coworking.espacio.TipoEspacio;
import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static co.devdavco.coworking.reserva.Fixtures.RELOJ;
import static co.devdavco.coworking.reserva.Fixtures.bogota;
import static co.devdavco.coworking.reserva.Fixtures.propiedades;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelarReservaServiceTest {

    private static final UsuarioActual ANA = new UsuarioActual(7L, Rol.USUARIO);
    private static final UsuarioActual BETO = new UsuarioActual(8L, Rol.USUARIO);
    private static final UsuarioActual ADMIN = new UsuarioActual(1L, Rol.ADMIN);

    @Mock
    ReservaRepository reservas;

    @Mock
    EspacioService espacioService;

    @Mock
    UsuarioRepository usuarios;

    private ReservaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReservaService(reservas, espacioService, usuarios,
                new ReglasHorario(propiedades(), RELOJ), propiedades(), RELOJ);
    }

    /** Reserva de Ana (id 7) de una hora que empieza en "inicio". */
    private Reserva reservaDeAna(Instant inicio) {
        Usuario ana = new Usuario("Ana", "ana@test.com", "hash", Rol.USUARIO);
        ana.setId(7L);
        Espacio sala = new Espacio("Sala Andes", TipoEspacio.SALA_REUNIONES, 8, 15);
        sala.setId(1L);
        Instant fin = inicio.plusSeconds(3600);
        Reserva reserva = new Reserva(ana, sala, inicio, fin, fin.plusSeconds(900), RELOJ.instant());
        when(reservas.findById(50L)).thenReturn(Optional.of(reserva));
        return reserva;
    }

    @Test
    void elDuenoCancelaAntesDeQueEmpiece() {
        Reserva reserva = reservaDeAna(bogota("2026-11-03T09:00"));

        assertThat(servicio.cancelar(ANA, 50L).estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void elDuenoNoPuedeCancelarUnaReservaQueYaEmpezo() {
        reservaDeAna(bogota("2026-11-02T06:30"));

        assertThatThrownBy(() -> servicio.cancelar(ANA, 50L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya empezó");
    }

    @Test
    void elAdminPuedeCancelarUnaReservaQueYaEmpezo() {
        reservaDeAna(bogota("2026-11-02T06:30"));

        assertThat(servicio.cancelar(ADMIN, 50L).estado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void otroUsuarioRecibe404ParaNoRevelarQueExiste() {
        reservaDeAna(bogota("2026-11-03T09:00"));

        assertThatThrownBy(() -> servicio.cancelar(BETO, 50L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cancelarDosVecesEsUnConflicto() {
        Reserva reserva = reservaDeAna(bogota("2026-11-03T09:00"));
        reserva.cancelar();

        assertThatThrownBy(() -> servicio.cancelar(ANA, 50L))
                .isInstanceOf(ConflictoException.class);
    }
}
