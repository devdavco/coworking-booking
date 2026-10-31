package co.devdavco.coworking.auth;

import co.devdavco.coworking.auth.dto.LoginRequest;
import co.devdavco.coworking.common.CredencialesInvalidasException;
import co.devdavco.coworking.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarios;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    /**
     * Si el login con un email inexistente se saltara BCrypt, respondería mucho más rápido
     * y un atacante podría averiguar qué emails están registrados midiendo el tiempo.
     */
    @Test
    void conUnEmailInexistenteTambienSeEjecutaBcrypt() {
        when(usuarios.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());
        AuthService servicio = new AuthService(usuarios, passwordEncoder, jwtService);

        assertThatThrownBy(() -> servicio.login(new LoginRequest("nadie@mail.com", "clave-segura-123")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(passwordEncoder).matches(eq("clave-segura-123"), any());
    }
}
