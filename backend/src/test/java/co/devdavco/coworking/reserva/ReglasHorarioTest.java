package co.devdavco.coworking.reserva;

import co.devdavco.coworking.common.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static co.devdavco.coworking.reserva.Fixtures.RELOJ;
import static co.devdavco.coworking.reserva.Fixtures.bogota;
import static co.devdavco.coworking.reserva.Fixtures.propiedades;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasHorarioTest {

    private final ReglasHorario reglas = new ReglasHorario(propiedades(), RELOJ);

    private void assertRechaza(String inicio, String fin, String mensaje) {
        assertThatThrownBy(() -> reglas.validar(bogota(inicio), bogota(fin)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining(mensaje);
    }

    private void assertAcepta(String inicio, String fin) {
        assertThatCode(() -> reglas.validar(bogota(inicio), bogota(fin))).doesNotThrowAnyException();
    }

    @Test
    void aceptaUnaReservaValida() {
        assertAcepta("2026-11-03T09:00", "2026-11-03T10:30");
    }

    @Test
    void rechazaUnaReservaQueEmpiezaEnElPasado() {
        assertRechaza("2026-11-02T06:45", "2026-11-02T08:00", "futuro");
    }

    @Test
    void rechazaUnaReservaQueEmpiezaJustoAhora() {
        assertRechaza("2026-11-02T07:00", "2026-11-02T08:00", "futuro");
    }

    @Test
    void rechazaFinAntesDelInicio() {
        assertRechaza("2026-11-03T10:00", "2026-11-03T09:00", "posterior");
    }

    @Test
    void respetaLaDuracionMinimaYMaxima() {
        assertRechaza("2026-11-03T09:00", "2026-11-03T09:15", "al menos 30 minutos");
        assertRechaza("2026-11-03T09:00", "2026-11-03T13:15", "más de 240 minutos");
        assertAcepta("2026-11-03T09:00", "2026-11-03T09:30");
        assertAcepta("2026-11-03T09:00", "2026-11-03T13:00");
    }

    @Test
    void exigeBloquesDe15Minutos() {
        assertRechaza("2026-11-03T09:07", "2026-11-03T10:07", "bloques de 15 minutos");
        assertRechaza("2026-11-03T09:00", "2026-11-03T09:50", "bloques de 15 minutos");
    }

    @Test
    void exigeQueEmpieceYTermineElMismoDia() {
        assertRechaza("2026-11-03T23:00", "2026-11-04T00:30", "mismo día");
    }

    @Test
    void respetaElHorarioDelCoworking() {
        assertRechaza("2026-11-03T06:30", "2026-11-03T07:30", "entre 07:00 y 22:00");
        assertRechaza("2026-11-03T21:00", "2026-11-03T22:15", "entre 07:00 y 22:00");
        assertAcepta("2026-11-03T07:00", "2026-11-03T08:00");
        assertAcepta("2026-11-03T21:00", "2026-11-03T22:00");
    }

    @Test
    void elFinTotalSumaLaLimpieza() {
        assertThat(reglas.calcularFinTotal(bogota("2026-11-03T10:00"), 15))
                .isEqualTo(bogota("2026-11-03T10:15"));
        assertThat(reglas.calcularFinTotal(bogota("2026-11-03T10:00"), 0))
                .isEqualTo(bogota("2026-11-03T10:00"));
    }
}
