package co.devdavco.coworking.usuario.dto;

import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;

/** Nunca incluye la contraseña ni su hash. */
public record UsuarioResponse(Long id, String nombre, String email, Rol rol) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol());
    }
}
