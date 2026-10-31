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
        verificarNombreLibre(nombre, null);
        Espacio espacio = new Espacio(nombre, request.tipo(), request.capacidad(), request.minutosLimpieza());
        return EspacioResponse.desde(espacios.save(espacio));
    }

    @Transactional
    public EspacioResponse actualizar(Long id, EspacioRequest request) {
        Espacio espacio = buscarActivo(id);
        String nombre = request.nombre().trim();
        verificarNombreLibre(nombre, id);
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

    /** Idempotente: activar un espacio ya activo no es un error. */
    @Transactional
    public EspacioResponse activar(Long id) {
        Espacio espacio = espacios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el espacio " + id));
        espacio.setActivo(true);
        return EspacioResponse.desde(espacio);
    }

    /** Devuelve la entidad; lo usan otros servicios (por ejemplo, reservas). */
    public Espacio buscarActivo(Long id) {
        return espacios.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el espacio " + id));
    }

    /** idActual es null al crear; al actualizar, el propio espacio puede conservar su nombre. */
    private void verificarNombreLibre(String nombre, Long idActual) {
        espacios.findByNombreIgnoreCase(nombre)
                .filter(existente -> !existente.getId().equals(idActual))
                .ifPresent(existente -> {
                    throw nombreDuplicado(existente);
                });
    }

    private static ConflictoException nombreDuplicado(Espacio existente) {
        if (!existente.isActivo()) {
            return new ConflictoException("Ya existe un espacio desactivado llamado '" + existente.getNombre()
                    + "'; puedes reactivarlo con POST /api/espacios/" + existente.getId() + "/activar");
        }
        return new ConflictoException("Ya existe un espacio llamado '" + existente.getNombre() + "'");
    }
}
