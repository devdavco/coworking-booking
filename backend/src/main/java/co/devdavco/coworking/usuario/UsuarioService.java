package co.devdavco.coworking.usuario;

import co.devdavco.coworking.common.RecursoNoEncontradoException;
import co.devdavco.coworking.usuario.dto.UsuarioResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public UsuarioResponse obtener(Long id) {
        return usuarios.findById(id)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }

    public List<UsuarioResponse> listar() {
        return usuarios.findAllByOrderByNombreAsc().stream()
                .map(UsuarioResponse::desde)
                .toList();
    }
}
