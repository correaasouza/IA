import { Directive, ElementRef, HostListener } from '@angular/core';
import { formatTelefone } from '../shared/telefone.validator';

@Directive({
  selector: '[appTelefoneMask]',
  standalone: true
})
export class TelefoneMaskDirective {
  constructor(private el: ElementRef<HTMLInputElement>) {}

  @HostListener('input')
  onInput() {
    const value = this.el.nativeElement.value || '';
    this.el.nativeElement.value = formatTelefone(value);
  }
}
