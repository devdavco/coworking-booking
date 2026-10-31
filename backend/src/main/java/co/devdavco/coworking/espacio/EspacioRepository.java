package co.devdavco.coworking.espacio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findAllByActivoTrueOrderByNombreAsc();

    List<Espacio> findAllByOrderByNombreAsc();

    Optional<Espacio> findByIdAndActivoTrue(Long id);

    Optional<Espacio> findByNombreIgnoreCase(String nombre);
}
