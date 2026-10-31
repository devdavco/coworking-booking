import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Barra } from './layout/barra';

@Component({
  imports: [RouterOutlet, Barra],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
