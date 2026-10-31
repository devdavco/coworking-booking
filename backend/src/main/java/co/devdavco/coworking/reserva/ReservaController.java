package co.devdavco.coworking.reserva;

import co.devdavco.coworking.auth.UsuarioActual;
import co.devdavco.coworking.reserva.dto.CrearReservaRequest;
import co.devdavco.coworking.reserva.dto.ReservaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
}
