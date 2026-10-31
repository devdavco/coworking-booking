package co.devdavco.coworking.auth;

import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInicialTest {

    @Mock
    UsuarioRepository usuarios;

    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void creaElAdminConEmailNormalizadoSiNoExiste() throws Exception {
        when(usuarios.existsByEmail("admin@coworking.com")).thenReturn(false);
        when(passwordEncoder.encode("clave-admin-123")).thenReturn("hash");

        new AdminInicial(new AdminProperties(" Admin@Coworking.com ", "clave-admin-123"), usuarios, passwordEncoder)
                .run(null);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().getEmail()).isEqualTo("admin@coworking.com");
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ADMIN);
        assertThat(guardado.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void noHaceNadaSiElAdminYaExiste() throws Exception {
        when(usuarios.existsByEmail("admin@coworking.com")).thenReturn(true);

        new AdminInicial(new AdminProperties("admin@coworking.com", "clave-admin-123"), usuarios, passwordEncoder)
                .run(null);

        verify(usuarios, never()).save(any());
    }

    @Test
    void noHaceNadaSiFaltanLasVariables() throws Exception {
        new AdminInicial(new AdminProperties("", ""), usuarios, passwordEncoder).run(null);
        new AdminInicial(new AdminProperties(null, null), usuarios, passwordEncoder).run(null);

        verifyNoInteractions(usuarios, passwordEncoder);
    }
}
