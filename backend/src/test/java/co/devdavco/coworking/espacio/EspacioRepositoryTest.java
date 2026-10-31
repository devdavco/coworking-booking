package co.devdavco.coworking.espacio;

import co.devdavco.coworking.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EspacioRepositoryTest extends IntegrationTest {

    @Autowired
    EspacioRepository espacios;

    @Test
    void listaSoloLosEspaciosActivos() {
        espacios.save(new Espacio("Sala Andes", TipoEspacio.SALA_REUNIONES, 8, 15));
        Espacio cabina = new Espacio("Cabina 1", TipoEspacio.CABINA, 1, 0);
        cabina.setActivo(false);
        espacios.save(cabina);

        assertThat(espacios.findAllByActivoTrueOrderByNombreAsc())
                .extracting(Espacio::getNombre)
                .containsExactly("Sala Andes");
    }

    @Test
    void laBaseDeDatosRechazaCapacidadCero() {
        assertThatThrownBy(() -> espacios.saveAndFlush(new Espacio("Cabina 2", TipoEspacio.CABINA, 0, 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
