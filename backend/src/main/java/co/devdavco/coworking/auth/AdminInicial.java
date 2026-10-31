package co.devdavco.coworking.auth;

import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Al arrancar, crea el ADMIN definido en ADMIN_EMAIL / ADMIN_PASSWORD si todavía no existe. */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);
    private static final int LIMITE_BYTES_BCRYPT = 72;

    private final AdminProperties propiedades;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public AdminInicial(AdminProperties propiedades, UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.propiedades = propiedades;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (vacio(propiedades.email()) || vacio(propiedades.password())) {
            log.info("ADMIN_EMAIL o ADMIN_PASSWORD no definidos: no se crea administrador inicial");
            return;
        }
        if (propiedades.password().getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT) {
            // Mejor no arrancar que arrancar sin el administrador que se configuró.
            throw new IllegalStateException("ADMIN_PASSWORD no puede ocupar más de 72 bytes (límite de BCrypt)");
        }
        String email = AuthService.normalizarEmail(propiedades.email());
        if (usuarios.existsByEmail(email)) {
            return;
        }
        usuarios.save(new Usuario("Administrador", email, passwordEncoder.encode(propiedades.password()), Rol.ADMIN));
        log.info("Administrador inicial creado: {}", email);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
