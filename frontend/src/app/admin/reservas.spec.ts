import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Reserva } from '../api/modelos';
import { AdminReservas } from './reservas';

const reserva: Reserva = {
  id: 5,
  usuarioId: 7,
  espacioId: 1,
  espacioNombre: 'Sala Andes',
  horaInicio: '2030-01-11T09:00:00-05:00',
  horaFin: '2030-01-11T10:00:00-05:00',
  horaFinTotal: '2030-01-11T10:15:00-05:00',
  estado: 'CONFIRMADA',
  finalizada: false,
};

describe('AdminReservas', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminReservas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  const reservasCon = (fecha: string) =>
    http.expectOne((r) => r.url.endsWith('/api/reservas') && r.params.get('fecha') === fecha);

  it('un error de carga no dice que no hay reservas y se borra cuando la siguiente carga funciona', async () => {
    const fixture = TestBed.createComponent(AdminReservas);
    const admin = fixture.componentInstance as any;
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/api/espacios/todos')).flush([]);
    http.expectOne((r) => r.url.endsWith('/api/usuarios')).flush([]);
    http.expectOne((r) => r.url.endsWith('/api/reservas')).error(new ProgressEvent('error'));
    await fixture.whenStable();

    let texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('No se pudo conectar con el servidor');
    expect(texto).not.toContain('No hay reservas para estos filtros');

    admin.cambiarFecha('2030-01-11');
    reservasCon('2030-01-11').flush([]);
    await fixture.whenStable();

    texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).not.toContain('No se pudo conectar con el servidor');
    expect(texto).toContain('No hay reservas para estos filtros');
  });

  it('si se cambia el filtro antes de que termine la carga anterior, se muestra el resultado del filtro nuevo', () => {
    const admin = TestBed.createComponent(AdminReservas).componentInstance as any;

    admin.cambiarFecha('2030-01-10');
    const lenta = reservasCon('2030-01-10');
    admin.cambiarFecha('2030-01-11');
    reservasCon('2030-01-11').flush([reserva]);
    if (!lenta.cancelled) {
      lenta.flush([]);
    }

    expect(admin.reservas()).toEqual([reserva]);
  });
});
