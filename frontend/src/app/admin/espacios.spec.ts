import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AdminEspacios } from './espacios';

describe('AdminEspacios', () => {
  it('si la carga falla no dice que no hay espacios', async () => {
    TestBed.configureTestingModule({
      imports: [AdminEspacios],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AdminEspacios);
    fixture.detectChanges();

    http.expectOne((r) => r.url.endsWith('/api/espacios/todos')).error(new ProgressEvent('error'));
    await fixture.whenStable();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('No se pudo conectar con el servidor');
    expect(texto).not.toContain('Todavía no hay espacios');
  });
});
