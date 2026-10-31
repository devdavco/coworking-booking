package co.devdavco.coworking.auth.dto;

import co.devdavco.coworking.common.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BCrypt solo admite 72 bytes; "ñ" ocupa 2 bytes, así que contar caracteres no basta. */
public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8) @MaxBytes(72) String password) {
}
