package co.devdavco.coworking.reserva;

import co.devdavco.coworking.common.ConflictoException;
import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.EspacioService;
import co.devdavco.coworking.espacio.TipoEspacio;
import co.devdavco.coworking.reserva.dto.CrearReservaRequest;
import co.devdavco.coworking.reserva.dto.ReservaResponse;
import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static co.devdavco.coworking.reserva.Fixtures.BOGOTA;
import static co.devdavco.coworking.reserva.Fixtures.RELOJ;
import static co.devdavco.coworking.reserva.Fixtures.bogota;
import static co.devdavco.coworking.reserva.Fixtures.propiedades;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    ReservaRepository reservas;

    @Mock
    EspacioService espacioService;

    @Mock
    UsuarioRepository usuarios;

    private ReservaService servicio;
    private Espacio sala;
    private Usuario ana;

    private final CrearReservaRequest nueveADiez = new CrearReservaRequest(1L,
            bogota("2026-11-03T09:00").atZone(BOGOTA).toOffsetDateTime(),
            bogota("2026-11-03T10:00").atZone(BOGOTA).toOffsetDateTime());

    @BeforeEach
    void preparar() {
        servicio = new ReservaService(reservas, espacioService, usuarios,
                new ReglasHorario(propiedades(), RELOJ), propiedades(), RELOJ);
        sala = new Espacio("Sala Andes", TipoEspacio.SALA_REUNIONES, 8, 15);
        sala.setId(1L);
        ana = new Usuario("Ana", "ana@test.com", "hash", Rol.USUARIO);
        ana.setId(7L);
    }

    @Test
    void guardaLaReservaConfirmadaSumandoLaLimpieza() {
        when(espacioService.buscarActivo(1L)).thenReturn(sala);
        when(reservas.findSolapadas(eq(1L), eq(EstadoReserva.CONFIRMADA), any(), any())).thenReturn(List.of());
        when(usuarios.getReferenceById(7L)).thenReturn(ana);
        when(reservas.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaResponse respuesta = servicio.crear(7L, nueveADiez);

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservas).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getHoraFinTotal()).isEqualTo(bogota("2026-11-03T10:15"));
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(respuesta.usuarioId()).isEqualTo(7L);
        assertThat(respuesta.finalizada()).isFalse();
    }

    @Test
    void rechazaUnaReservaQueSeCruzaIndicandoLaFranjaOcupada() {
        Reserva existente = new Reserva(ana, sala, bogota("2026-11-03T08:30"), bogota("2026-11-03T09:15"),
                bogota("2026-11-03T09:30"), RELOJ.instant());
        when(espacioService.buscarActivo(1L)).thenReturn(sala);
        when(reservas.findSolapadas(eq(1L), eq(EstadoReserva.CONFIRMADA), any(), any()))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> servicio.crear(7L, nueveADiez))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("08:30 a 09:30");
        verify(reservas, never()).saveAndFlush(any());
    }

    @Test
    void traduceLaViolacionDeLaConstraintA409() {
        when(espacioService.buscarActivo(1L)).thenReturn(sala);
        when(reservas.findSolapadas(eq(1L), eq(EstadoReserva.CONFIRMADA), any(), any())).thenReturn(List.of());
        when(usuarios.getReferenceById(7L)).thenReturn(ana);
        when(reservas.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("error",
                new RuntimeException("violates exclusion constraint \"reservas_sin_solapamiento\"")));

        assertThatThrownBy(() -> servicio.crear(7L, nueveADiez))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ocupado");
    }

    @Test
    void noOcultaOtrosErroresDeIntegridad() {
        when(espacioService.buscarActivo(1L)).thenReturn(sala);
        when(reservas.findSolapadas(eq(1L), eq(EstadoReserva.CONFIRMADA), any(), any())).thenReturn(List.of());
        when(usuarios.getReferenceById(7L)).thenReturn(ana);
        when(reservas.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("error",
                new RuntimeException("violates foreign key constraint \"reservas_usuario_id_fkey\"")));

        assertThatThrownBy(() -> servicio.crear(7L, nueveADiez))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
