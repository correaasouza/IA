import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inline-loader',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './inline-loader.component.html',
  styleUrls: ['./inline-loader.component.css']
})
export class InlineLoaderComponent {
  @Input() label = 'Carregando...';
}
