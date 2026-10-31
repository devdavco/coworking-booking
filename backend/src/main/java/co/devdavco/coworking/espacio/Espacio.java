package co.devdavco.coworking.espacio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "espacios")
@Getter
@Setter
@NoArgsConstructor
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(name = "minutos_limpieza", nullable = false)
    private Integer minutosLimpieza;

    @Column(nullable = false)
    private boolean activo = true;

    public Espacio(String nombre, TipoEspacio tipo, int capacidad, int minutosLimpieza) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.minutosLimpieza = minutosLimpieza;
    }
}
