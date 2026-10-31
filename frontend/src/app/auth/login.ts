import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { mensajeDeError } from '../api/errores';
import { SesionService } from './sesion.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login implements OnInit {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  email = '';
  password = '';
  readonly error = signal('');
  readonly aviso = signal('');
  readonly enviando = signal(false);

  ngOnInit(): void {
    if (this.sesion.token()) {
      this.router.navigate(['/reservar']);
      return;
    }
    if (this.ruta.snapshot.queryParamMap.get('expirada')) {
      this.aviso.set('Tu sesión expiró, vuelve a iniciar sesión');
    }
  }

  entrar(): void {
    this.error.set('');
    this.enviando.set(true);
    this.sesion.iniciar(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/reservar']),
      error: (err) => {
        this.error.set(mensajeDeError(err));
        this.enviando.set(false);
      },
    });
  }
}
