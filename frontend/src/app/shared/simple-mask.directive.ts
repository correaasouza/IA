import { Directive, ElementRef, HostListener, Input } from '@angular/core';

@Directive({
  selector: '[appSimpleMask]',
  standalone: true
})
export class SimpleMaskDirective {
  @Input('appSimpleMask') pattern = /\D/g;

  constructor(private el: ElementRef<HTMLInputElement>) {}

  @HostListener('input')
  onInput() {
    const value = this.el.nativeElement.value || '';
    this.el.nativeElement.value = value.replace(this.pattern as any, '');
  }
}
