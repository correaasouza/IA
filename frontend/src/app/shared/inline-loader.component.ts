import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inline-loader',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './inline-loader.component.html'
})
export class InlineLoaderComponent {
  @Input() label = 'Carregando...';
}
