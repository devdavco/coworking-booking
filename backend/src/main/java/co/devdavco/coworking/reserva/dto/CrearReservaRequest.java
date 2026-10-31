package co.devdavco.coworking.reserva.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

/** No incluye usuarioId: la reserva siempre queda a nombre del usuario del token. */
public record CrearReservaRequest(
        @NotNull @Positive Long espacioId,
        @NotNull OffsetDateTime horaInicio,
        @NotNull OffsetDateTime horaFin) {
}
