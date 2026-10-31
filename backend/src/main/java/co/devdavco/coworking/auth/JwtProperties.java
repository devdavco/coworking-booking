package co.devdavco.coworking.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** La app no arranca si el secreto falta o tiene menos de 32 caracteres. */
@ConfigurationProperties("app.jwt")
@Validated
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotNull Duration expiracion) {
}
