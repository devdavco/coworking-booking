package co.devdavco.coworking.reserva;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/** Filtros combinables para el listado de reservas del ADMIN. */
final class ReservaSpecs {

    private ReservaSpecs() {
    }

    static Specification<Reserva> delEspacio(Long espacioId) {
        return (reserva, consulta, cb) -> cb.equal(reserva.get("espacio").get("id"), espacioId);
    }

    static Specification<Reserva> seCruzaCon(Instant desde, Instant hasta) {
        return (reserva, consulta, cb) -> cb.and(
                cb.lessThan(reserva.<Instant>get("horaInicio"), hasta),
                cb.greaterThan(reserva.<Instant>get("horaFinTotal"), desde));
    }
}
