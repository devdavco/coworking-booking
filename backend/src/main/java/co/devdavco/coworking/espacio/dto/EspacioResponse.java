package co.devdavco.coworking.espacio.dto;

import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.espacio.TipoEspacio;

public record EspacioResponse(
        Long id, String nombre, TipoEspacio tipo, int capacidad, int minutosLimpieza, boolean activo) {

    public static EspacioResponse desde(Espacio espacio) {
        return new EspacioResponse(
                espacio.getId(),
                espacio.getNombre(),
                espacio.getTipo(),
                espacio.getCapacidad(),
                espacio.getMinutosLimpieza(),
                espacio.isActivo());
    }
}
