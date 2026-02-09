import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-blocked',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatButtonModule, RouterLink],
  templateUrl: './blocked.component.html',
  styleUrls: ['./blocked.component.css']
})
export class BlockedComponent {}
