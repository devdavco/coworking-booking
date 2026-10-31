package co.devdavco.coworking.auth;

import co.devdavco.coworking.auth.dto.TokenResponse;
import co.devdavco.coworking.usuario.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties propiedades;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, JwtProperties propiedades, Clock clock) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public TokenResponse generar(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expira = ahora.plus(propiedades.expiracion());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("coworking-booking")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, expira);
    }
}
