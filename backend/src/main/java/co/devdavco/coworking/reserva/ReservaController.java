package co.devdavco.coworking.reserva;

import co.devdavco.coworking.auth.UsuarioActual;
import co.devdavco.coworking.reserva.dto.CrearReservaRequest;
import co.devdavco.coworking.reserva.dto.FranjaOcupada;
import co.devdavco.coworking.reserva.dto.ReservaResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponse> crear(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody CrearReservaRequest request) {
        ReservaResponse creada = reservaService.crear(UsuarioActual.desde(jwt).id(), request);
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.id())).body(creada);
    }

    @GetMapping("/reservas/mias")
    public List<ReservaResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return reservaService.listarMias(UsuarioActual.desde(jwt).id());
    }

    /** Solo ADMIN (ver SecurityConfig). */
    @GetMapping("/reservas")
    public List<ReservaResponse> listar(
            @RequestParam(required = false) Long espacioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservaService.listar(espacioId, fecha);
    }

    @GetMapping("/reservas/{id}")
    public ReservaResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reservaService.obtener(UsuarioActual.desde(jwt), id);
    }

    @PostMapping("/reservas/{id}/cancelar")
    public ReservaResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reservaService.cancelar(UsuarioActual.desde(jwt), id);
    }

    @GetMapping("/espacios/{id}/ocupacion")
    public List<FranjaOcupada> ocupacion(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservaService.ocupacion(id, fecha);
    }
}
