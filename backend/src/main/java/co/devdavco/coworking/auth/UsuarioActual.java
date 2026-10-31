package co.devdavco.coworking.auth;

import co.devdavco.coworking.usuario.Rol;
import org.springframework.security.oauth2.jwt.Jwt;

/** El usuario autenticado, leído de los claims del JWT. */
public record UsuarioActual(Long id, Rol rol) {

    public static UsuarioActual desde(Jwt jwt) {
        return new UsuarioActual(Long.valueOf(jwt.getSubject()), Rol.valueOf(jwt.getClaimAsString("rol")));
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }
}
