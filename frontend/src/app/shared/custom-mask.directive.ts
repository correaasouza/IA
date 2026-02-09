import { Directive, ElementRef, HostListener, Input } from '@angular/core';

@Directive({
  selector: '[appCustomMask]',
  standalone: true
})
export class CustomMaskDirective {
  @Input('appCustomMask') mask = '';

  constructor(private el: ElementRef<HTMLInputElement>) {}

  @HostListener('input')
  onInput() {
    if (!this.mask) {
      return;
    }
    const digits = (this.el.nativeElement.value || '').replace(/\D/g, '');
    let out = '';
    let di = 0;
    for (let i = 0; i < this.mask.length; i++) {
      const ch = this.mask[i];
      if (ch === '#') {
        if (di >= digits.length) break;
        out += digits[di++];
      } else {
        if (di < digits.length) {
          out += ch;
        }
      }
    }
    this.el.nativeElement.value = out;
  }
}
