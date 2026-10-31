package co.devdavco.coworking.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    /** BCrypt solo admite 72 bytes; "ñ" ocupa 2 bytes, así que contar caracteres no basta. */
    @AssertTrue(message = "la contraseña no puede ocupar más de 72 bytes")
    public boolean isPasswordDentroDeLimite() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
