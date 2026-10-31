package co.devdavco.coworking.reserva;

import co.devdavco.coworking.common.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/** Reglas de horario de una reserva. No toca la base de datos: se prueba con un reloj fijo. */
@Component
public class ReglasHorario {

    private final ReservasProperties propiedades;
    private final Clock clock;

    public ReglasHorario(ReservasProperties propiedades, Clock clock) {
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public void validar(Instant inicio, Instant fin) {
        if (!inicio.isAfter(clock.instant())) {
            throw new ReglaNegocioException("La reserva debe empezar en el futuro");
        }
        if (inicio.isAfter(clock.instant().plus(propiedades.anticipacionMaxima()))) {
            throw new ReglaNegocioException("No se puede reservar con más de "
                    + propiedades.anticipacionMaxima().toDays() + " días de anticipación");
        }
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio");
        }
        Duration duracion = Duration.between(inicio, fin);
        if (duracion.compareTo(propiedades.duracionMinima()) < 0) {
            throw new ReglaNegocioException(
                    "La reserva debe durar al menos " + propiedades.duracionMinima().toMinutes() + " minutos");
        }
        if (duracion.compareTo(propiedades.duracionMaxima()) > 0) {
            throw new ReglaNegocioException(
                    "La reserva no puede durar más de " + propiedades.duracionMaxima().toMinutes() + " minutos");
        }
        ZonedDateTime inicioLocal = inicio.atZone(propiedades.zonaHoraria());
        ZonedDateTime finLocal = fin.atZone(propiedades.zonaHoraria());
        if (!alineado(inicioLocal) || !alineado(finLocal)) {
            throw new ReglaNegocioException(
                    "Las horas deben ir en bloques de " + propiedades.bloque().toMinutes() + " minutos");
        }
        if (!inicioLocal.toLocalDate().equals(finLocal.toLocalDate())) {
            throw new ReglaNegocioException("La reserva debe empezar y terminar el mismo día");
        }
        if (inicioLocal.toLocalTime().isBefore(propiedades.apertura())
                || finLocal.toLocalTime().isAfter(propiedades.cierre())) {
            throw new ReglaNegocioException(
                    "La reserva debe estar entre " + propiedades.apertura() + " y " + propiedades.cierre());
        }
    }

    public Instant calcularFinTotal(Instant fin, int minutosLimpieza) {
        return fin.plus(Duration.ofMinutes(minutosLimpieza));
    }

    private boolean alineado(ZonedDateTime momento) {
        return momento.getNano() == 0
                && momento.toLocalTime().toSecondOfDay() % propiedades.bloque().toSeconds() == 0;
    }
}
