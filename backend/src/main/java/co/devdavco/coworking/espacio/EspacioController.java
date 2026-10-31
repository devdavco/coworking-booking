package co.devdavco.coworking.espacio;

import co.devdavco.coworking.espacio.dto.EspacioRequest;
import co.devdavco.coworking.espacio.dto.EspacioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @GetMapping
    public List<EspacioResponse> listar() {
        return espacioService.listarActivos();
    }

    /** Solo ADMIN (ver SecurityConfig). */
    @GetMapping("/todos")
    public List<EspacioResponse> listarTodos() {
        return espacioService.listarTodos();
    }

    @GetMapping("/{id}")
    public EspacioResponse obtener(@PathVariable Long id) {
        return espacioService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EspacioResponse> crear(@Valid @RequestBody EspacioRequest request) {
        EspacioResponse creado = espacioService.crear(request);
        return ResponseEntity.created(URI.create("/api/espacios/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public EspacioResponse actualizar(@PathVariable Long id, @Valid @RequestBody EspacioRequest request) {
        return espacioService.actualizar(id, request);
    }

    @PostMapping("/{id}/activar")
    public EspacioResponse activar(@PathVariable Long id) {
        return espacioService.activar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        espacioService.desactivar(id);
    }
}
