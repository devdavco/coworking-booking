package co.devdavco.coworking.reserva;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/** Reglas de horario del coworking. Si son inválidas, la app no arranca. */
@ConfigurationProperties("app.reservas")
public record ReservasProperties(
        ZoneId zonaHoraria,
        @DateTimeFormat(pattern = "HH:mm") LocalTime apertura,
        @DateTimeFormat(pattern = "HH:mm") LocalTime cierre,
        Duration duracionMinima,
        Duration duracionMaxima,
        Duration bloque,
        Duration anticipacionMaxima) {

    public ReservasProperties {
        Objects.requireNonNull(zonaHoraria, "app.reservas.zona-horaria es obligatorio");
        Objects.requireNonNull(apertura, "app.reservas.apertura es obligatorio");
        Objects.requireNonNull(cierre, "app.reservas.cierre es obligatorio");
        Objects.requireNonNull(duracionMinima, "app.reservas.duracion-minima es obligatorio");
        Objects.requireNonNull(duracionMaxima, "app.reservas.duracion-maxima es obligatorio");
        Objects.requireNonNull(bloque, "app.reservas.bloque es obligatorio");
        Objects.requireNonNull(anticipacionMaxima, "app.reservas.anticipacion-maxima es obligatorio");
        if (!cierre.isAfter(apertura)) {
            throw new IllegalArgumentException("app.reservas.cierre debe ser posterior a la apertura");
        }
        if (duracionMinima.isNegative() || duracionMinima.isZero()) {
            throw new IllegalArgumentException("app.reservas.duracion-minima debe ser positiva");
        }
        if (duracionMaxima.compareTo(duracionMinima) < 0) {
            throw new IllegalArgumentException("app.reservas.duracion-maxima no puede ser menor que la mínima");
        }
        if (bloque.isNegative() || bloque.isZero()) {
            throw new IllegalArgumentException("app.reservas.bloque debe ser positivo");
        }
        if (anticipacionMaxima.isNegative() || anticipacionMaxima.isZero()) {
            throw new IllegalArgumentException("app.reservas.anticipacion-maxima debe ser positiva");
        }
    }
}
