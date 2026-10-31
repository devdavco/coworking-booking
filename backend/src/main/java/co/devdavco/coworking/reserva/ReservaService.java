package co.devdavco.coworking.reserva;

import co.devdavco.coworking.auth.UsuarioActual;
import co.devdavco.coworking.common.ConflictoException;
import co.devdavco.coworking.common.RecursoNoEncontradoException;
import co.devdavco.coworking.common.ReglaNegocioException;
import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.EspacioService;
import co.devdavco.coworking.reserva.dto.CrearReservaRequest;
import co.devdavco.coworking.reserva.dto.FranjaOcupada;
import co.devdavco.coworking.reserva.dto.ReservaResponse;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarMias(Long usuarioId) {
        return reservas.findAllByUsuario_IdOrderByHoraInicioDesc(usuarioId).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(Long espacioId, LocalDate fecha) {
        List<Specification<Reserva>> filtros = new ArrayList<>();
        if (espacioId != null) {
            filtros.add(ReservaSpecs.delEspacio(espacioId));
        }
        if (fecha != null) {
            filtros.add(ReservaSpecs.seCruzaCon(inicioDelDia(fecha), inicioDelDia(fecha.plusDays(1))));
        }
        return reservas.findAll(Specification.allOf(filtros), Sort.by(Sort.Direction.DESC, "horaInicio")).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(UsuarioActual actual, Long id) {
        return mapear(buscarVisible(actual, id));
    }

    @Transactional
    public ReservaResponse cancelar(UsuarioActual actual, Long id) {
        Reserva reserva = buscarVisible(actual, id);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("La reserva ya está cancelada");
        }
        if (!actual.esAdmin() && !reserva.getHoraInicio().isAfter(clock.instant())) {
            throw new ReglaNegocioException("No puedes cancelar una reserva que ya empezó");
        }
        reserva.cancelar();
        return mapear(reserva);
    }

    @Transactional(readOnly = true)
    public List<FranjaOcupada> ocupacion(Long espacioId, LocalDate fecha) {
        espacioService.buscarActivo(espacioId);
        return reservas.findSolapadas(espacioId, EstadoReserva.CONFIRMADA,
                        inicioDelDia(fecha), inicioDelDia(fecha.plusDays(1))).stream()
                .map(reserva -> new FranjaOcupada(
                        reserva.getHoraInicio().atZone(propiedades.zonaHoraria()).toOffsetDateTime(),
                        reserva.getHoraFinTotal().atZone(propiedades.zonaHoraria()).toOffsetDateTime()))
                .toList();
    }

    /** Un usuario solo ve sus reservas; si pide la de otro, responde 404 para no revelar que existe. */
    private Reserva buscarVisible(UsuarioActual actual, Long id) {
        return reservas.findById(id)
                .filter(reserva -> actual.esAdmin() || reserva.esDe(actual.id()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + id));
    }

    private Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(propiedades.zonaHoraria()).toInstant();
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
