package co.devdavco.coworking.reserva.dto;

import java.time.OffsetDateTime;

/** Intervalo [desde, hasta) ocupado por una reserva, incluida la limpieza. */
public record FranjaOcupada(OffsetDateTime desde, OffsetDateTime hasta) {
}
