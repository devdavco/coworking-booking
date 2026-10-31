package co.devdavco.coworking.reserva;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

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

    /** Carga el espacio en la misma consulta para evitar N+1 al mapear el nombre. */
    @EntityGraph(attributePaths = "espacio")
    List<Reserva> findAllByUsuario_IdOrderByHoraInicioDesc(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = "espacio")
    List<Reserva> findAll(Specification<Reserva> spec, Sort sort);
}
