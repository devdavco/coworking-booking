import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SesionService } from '../auth/sesion.service';

/** Barra superior; solo se ve cuando hay un usuario cargado. */
@Component({
  selector: 'app-barra',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './barra.html',
  styleUrl: './barra.css',
})
export class Barra implements OnInit {
  protected readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (this.sesion.token()) {
      // Si falla (token rechazado), el interceptor ya cierra la sesión.
      this.sesion.cargarUsuario().subscribe({ error: () => undefined });
    }
  }

  salir(): void {
    this.sesion.cerrar();
    this.router.navigate(['/login']);
  }
}
