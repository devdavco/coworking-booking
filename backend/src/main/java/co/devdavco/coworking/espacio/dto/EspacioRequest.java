package co.devdavco.coworking.espacio.dto;

import co.devdavco.coworking.espacio.TipoEspacio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EspacioRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotNull TipoEspacio tipo,
        @NotNull @Positive Integer capacidad,
        @NotNull @PositiveOrZero Integer minutosLimpieza) {
}
