package co.devdavco.coworking.reserva;

import co.devdavco.coworking.espacio.Espacio;
import co.devdavco.coworking.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "reservas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @Column(name = "hora_inicio", nullable = false)
    private Instant horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private Instant horaFin;

    @Column(name = "hora_fin_total", nullable = false)
    private Instant horaFinTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Version
    private Integer version;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Reserva(Usuario usuario, Espacio espacio, Instant horaInicio, Instant horaFin,
                   Instant horaFinTotal, Instant creadoEn) {
        this.usuario = usuario;
        this.espacio = espacio;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.horaFinTotal = horaFinTotal;
        this.creadoEn = creadoEn;
        this.estado = EstadoReserva.CONFIRMADA;
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public boolean esDe(Long usuarioId) {
        return usuario.getId().equals(usuarioId);
    }
}
