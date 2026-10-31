package co.devdavco.coworking.auth;

import co.devdavco.coworking.auth.dto.LoginRequest;
import co.devdavco.coworking.auth.dto.RegistroRequest;
import co.devdavco.coworking.auth.dto.TokenResponse;
import co.devdavco.coworking.common.ConflictoException;
import co.devdavco.coworking.common.CredencialesInvalidasException;
import co.devdavco.coworking.usuario.Rol;
import co.devdavco.coworking.usuario.Usuario;
import co.devdavco.coworking.usuario.UsuarioRepository;
import co.devdavco.coworking.usuario.dto.UsuarioResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private static final int LIMITE_BYTES_BCRYPT = 72;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarios.existsByEmail(email)) {
            throw new ConflictoException("Ya existe una cuenta con el email " + email);
        }
        Usuario usuario = new Usuario(request.nombre().trim(), email,
                passwordEncoder.encode(request.password()), Rol.USUARIO);
        return UsuarioResponse.desde(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT) {
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = usuarios.findByEmail(normalizarEmail(request.email()))
                .filter(encontrado -> passwordEncoder.matches(request.password(), encontrado.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        return jwtService.generar(usuario);
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
