package co.devdavco.coworking.reserva;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Datos compartidos por los tests unitarios de reservas. */
final class Fixtures {

    static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** "Ahora" en los tests: lunes 2026-11-02 a las 07:00 en Bogotá (12:00 UTC). */
    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);

    private Fixtures() {
    }

    static ReservasProperties propiedades() {
        return new ReservasProperties(BOGOTA, LocalTime.of(7, 0), LocalTime.of(22, 0),
                Duration.ofMinutes(30), Duration.ofHours(4), Duration.ofMinutes(15));
    }

    /** Convierte "2026-11-03T09:00" (hora de Bogotá) a Instant. */
    static Instant bogota(String fechaHoraLocal) {
        return LocalDateTime.parse(fechaHoraLocal).atZone(BOGOTA).toInstant();
    }
}
