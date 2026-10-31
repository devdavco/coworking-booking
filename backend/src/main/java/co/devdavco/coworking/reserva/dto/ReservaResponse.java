package co.devdavco.coworking.reserva.dto;

import co.devdavco.coworking.reserva.EstadoReserva;
import co.devdavco.coworking.reserva.Reserva;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ReservaResponse(
        Long id,
        Long usuarioId,
        Long espacioId,
        String espacioNombre,
        OffsetDateTime horaInicio,
        OffsetDateTime horaFin,
        OffsetDateTime horaFinTotal,
        EstadoReserva estado,
        boolean finalizada) {

    public static ReservaResponse desde(Reserva reserva, ZoneId zona, Instant ahora) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getEspacio().getId(),
                reserva.getEspacio().getNombre(),
                reserva.getHoraInicio().atZone(zona).toOffsetDateTime(),
                reserva.getHoraFin().atZone(zona).toOffsetDateTime(),
                reserva.getHoraFinTotal().atZone(zona).toOffsetDateTime(),
                reserva.getEstado(),
                reserva.getHoraFin().isBefore(ahora));
    }
}
