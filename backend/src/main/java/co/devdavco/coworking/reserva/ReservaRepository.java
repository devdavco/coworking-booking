package co.devdavco.coworking.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas del espacio cuyo rango [horaInicio, horaFinTotal) se cruza con [desde, hasta). */
    @Query("""
            select r from Reserva r
            where r.espacio.id = :espacioId
              and r.estado = :estado
              and r.horaInicio < :hasta
              and r.horaFinTotal > :desde
            order by r.horaInicio
            """)
    List<Reserva> findSolapadas(@Param("espacioId") Long espacioId,
                                @Param("estado") EstadoReserva estado,
                                @Param("desde") Instant desde,
                                @Param("hasta") Instant hasta);
}
