package co.devdavco.coworking;

import co.devdavco.coworking.auth.JwtService;
import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Base de los tests de integración: la app completa contra un PostgreSQL real en Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final String CLAVE = "clave-segura-123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UsuarioRepository usuarios;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    @BeforeEach
    void limpiarBase() {
        jdbc.execute("TRUNCATE reservas, espacios, usuarios RESTART IDENTITY CASCADE");
    }

    protected Usuario crearUsuario(String email, Rol rol) {
        return usuarios.save(new Usuario("Usuario " + email, email, passwordEncoder.encode(CLAVE), rol));
    }

    /** Valor listo para el header Authorization. */
    protected String tokenDe(Usuario usuario) {
        return "Bearer " + jwtService.generar(usuario).token();
    }

    /** Crea admin@test.com; llamar como máximo una vez por test. */
    protected String tokenAdmin() {
        return tokenDe(crearUsuario("admin@test.com", Rol.ADMIN));
    }

    /** Crea usuario@test.com; llamar como máximo una vez por test. */
    protected String tokenUsuario() {
        return tokenDe(crearUsuario("usuario@test.com", Rol.USUARIO));
    }
}
