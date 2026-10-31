package co.devdavco.coworking.reserva;

import co.devdavco.coworking.common.ConflictoException;
import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.EspacioService;
import co.devdavco.coworking.reserva.dto.CrearReservaRequest;
import co.devdavco.coworking.reserva.dto.ReservaResponse;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReservaService {

    private static final String CONSTRAINT_SOLAPAMIENTO = "reservas_sin_solapamiento";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservaRepository reservas;
    private final EspacioService espacioService;
    private final UsuarioRepository usuarios;
    private final ReglasHorario reglasHorario;
    private final ReservasProperties propiedades;
    private final Clock clock;

    public ReservaService(ReservaRepository reservas, EspacioService espacioService, UsuarioRepository usuarios,
                          ReglasHorario reglasHorario, ReservasProperties propiedades, Clock clock) {
        this.reservas = reservas;
        this.espacioService = espacioService;
        this.usuarios = usuarios;
        this.reglasHorario = reglasHorario;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    @Transactional
    public ReservaResponse crear(Long usuarioId, CrearReservaRequest request) {
        Espacio espacio = espacioService.buscarActivo(request.espacioId());
        Instant inicio = request.horaInicio().toInstant();
        Instant fin = request.horaFin().toInstant();
        reglasHorario.validar(inicio, fin);
        Instant finTotal = reglasHorario.calcularFinTotal(fin, espacio.getMinutosLimpieza());

        List<Reserva> cruces = reservas.findSolapadas(espacio.getId(), EstadoReserva.CONFIRMADA, inicio, finTotal);
        if (!cruces.isEmpty()) {
            throw franjaOcupada(cruces.get(0));
        }

        Usuario usuario = usuarios.getReferenceById(usuarioId);
        Reserva reserva = new Reserva(usuario, espacio, inicio, fin, finTotal, clock.instant());
        try {
            reserva = reservas.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException ex) {
            // Dos peticiones pasaron la consulta a la vez: la base de datos rechazó la segunda.
            if (esSolapamiento(ex)) {
                throw new ConflictoException("El espacio ya está ocupado en ese horario");
            }
            throw ex;
        }
        return mapear(reserva);
    }

    private ReservaResponse mapear(Reserva reserva) {
        return ReservaResponse.desde(reserva, propiedades.zonaHoraria(), clock.instant());
    }

    private ConflictoException franjaOcupada(Reserva existente) {
        String desde = HORA.format(existente.getHoraInicio().atZone(propiedades.zonaHoraria()));
        String hasta = HORA.format(existente.getHoraFinTotal().atZone(propiedades.zonaHoraria()));
        return new ConflictoException(
                "El espacio ya está ocupado de " + desde + " a " + hasta + " (incluye limpieza)");
    }

    private static boolean esSolapamiento(DataIntegrityViolationException ex) {
        String mensaje = ex.getMostSpecificCause().getMessage();
        return mensaje != null && mensaje.contains(CONSTRAINT_SOLAPAMIENTO);
    }
}
