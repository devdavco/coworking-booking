import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Espacio } from '../api/modelos';
import { Reservar } from './reservar';

const sala: Espacio = { id: 1, nombre: 'Sala Andes', tipo: 'SALA_REUNIONES', capacidad: 8, minutosLimpieza: 15, activo: true };

describe('Reservar', () => {
  it('si se cambia de fecha antes de que termine la carga anterior, la cuadrícula muestra la fecha nueva', () => {
    TestBed.configureTestingModule({
      imports: [Reservar],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    const reservar = TestBed.createComponent(Reservar).componentInstance as any;
    const ocupacionDe = (fecha: string) =>
      http.expectOne((r) => r.url.endsWith('/api/espacios/1/ocupacion') && r.params.get('fecha') === fecha);

    reservar.cambiarFecha('2030-01-10');
    http.expectOne((r) => r.url.endsWith('/api/espacios')).flush([sala]);
    const lenta = ocupacionDe('2030-01-10');

    reservar.cambiarFecha('2030-01-11');
    http.expectOne((r) => r.url.endsWith('/api/espacios')).flush([sala]);
    ocupacionDe('2030-01-11').flush([{ desde: '2030-01-11T09:00:00-05:00', hasta: '2030-01-11T10:00:00-05:00' }]);

    // La respuesta de la fecha anterior llega tarde: no debe pisar la cuadrícula nueva.
    if (!lenta.cancelled) {
      lenta.flush([]);
    }

    expect(reservar.filas()[0].estados[8]).toBe('ocupado');
  });
});
