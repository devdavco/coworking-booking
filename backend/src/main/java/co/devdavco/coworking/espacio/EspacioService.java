package co.devdavco.coworking.espacio;

import co.devdavco.coworking.common.ConflictoException;
import co.devdavco.coworking.common.RecursoNoEncontradoException;
import co.devdavco.coworking.espacio.dto.EspacioRequest;
import co.devdavco.coworking.espacio.dto.EspacioResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EspacioService {

    private final EspacioRepository espacios;

    public EspacioService(EspacioRepository espacios) {
        this.espacios = espacios;
    }

    public List<EspacioResponse> listarActivos() {
        return espacios.findAllByActivoTrueOrderByNombreAsc().stream()
                .map(EspacioResponse::desde)
                .toList();
    }

    public EspacioResponse obtener(Long id) {
        return EspacioResponse.desde(buscarActivo(id));
    }

    @Transactional
    public EspacioResponse crear(EspacioRequest request) {
        String nombre = request.nombre().trim();
        if (espacios.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicado(nombre);
        }
        Espacio espacio = new Espacio(nombre, request.tipo(), request.capacidad(), request.minutosLimpieza());
        return EspacioResponse.desde(espacios.save(espacio));
    }

    @Transactional
    public EspacioResponse actualizar(Long id, EspacioRequest request) {
        Espacio espacio = buscarActivo(id);
        String nombre = request.nombre().trim();
        if (espacios.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicado(nombre);
        }
        espacio.setNombre(nombre);
        espacio.setTipo(request.tipo());
        espacio.setCapacidad(request.capacidad());
        espacio.setMinutosLimpieza(request.minutosLimpieza());
        return EspacioResponse.desde(espacio);
    }

    @Transactional
    public void desactivar(Long id) {
        buscarActivo(id).setActivo(false);
    }

    /** Devuelve la entidad; lo usan otros servicios (por ejemplo, reservas). */
    public Espacio buscarActivo(Long id) {
        return espacios.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el espacio " + id));
    }

    private static ConflictoException nombreDuplicado(String nombre) {
        return new ConflictoException("Ya existe un espacio llamado '" + nombre + "'");
    }
}
