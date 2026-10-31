package co.devdavco.coworking.reserva;

import co.devdavco.coworking.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservasPropertiesTest extends IntegrationTest {

    @Autowired
    ReservasProperties propiedades;

    @Test
    void seLeenDesdeApplicationYml() {
        assertThat(propiedades.zonaHoraria()).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(propiedades.apertura()).isEqualTo(LocalTime.of(7, 0));
        assertThat(propiedades.cierre()).isEqualTo(LocalTime.of(22, 0));
        assertThat(propiedades.duracionMinima()).isEqualTo(Duration.ofMinutes(30));
        assertThat(propiedades.duracionMaxima()).isEqualTo(Duration.ofHours(4));
        assertThat(propiedades.bloque()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void rechazaUnCierreAntesDeLaApertura() {
        assertThatThrownBy(() -> new ReservasProperties(ZoneId.of("America/Bogota"),
                LocalTime.of(22, 0), LocalTime.of(7, 0),
                Duration.ofMinutes(30), Duration.ofHours(4), Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnaDuracionMaximaMenorQueLaMinima() {
        assertThatThrownBy(() -> new ReservasProperties(ZoneId.of("America/Bogota"),
                LocalTime.of(7, 0), LocalTime.of(22, 0),
                Duration.ofHours(2), Duration.ofHours(1), Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
