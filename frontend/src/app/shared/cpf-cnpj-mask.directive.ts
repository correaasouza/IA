import { Directive, ElementRef, HostListener } from '@angular/core';
import { formatCpfCnpj } from '../shared/cpf-cnpj.validator';

@Directive({
  selector: '[appCpfCnpjMask]',
  standalone: true
})
export class CpfCnpjMaskDirective {
  constructor(private el: ElementRef<HTMLInputElement>) {}

  @HostListener('input', ['$event'])
  onInput() {
    const value = this.el.nativeElement.value || '';
    this.el.nativeElement.value = formatCpfCnpj(value);
  }
}
