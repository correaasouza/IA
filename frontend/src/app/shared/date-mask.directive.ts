import { Directive, ElementRef, HostListener, Optional } from '@angular/core';
import { NgControl } from '@angular/forms';

@Directive({
  selector: '[appDateMask]',
  standalone: true
})
export class DateMaskDirective {
  constructor(private el: ElementRef<HTMLInputElement>, @Optional() private ngControl?: NgControl) {}

  @HostListener('input')
  onInput() {
    const input = this.el.nativeElement;
    const digits = (input.value || '').replace(/\D/g, '').slice(0, 8);
    const formatted = this.format(digits);
    this.setValue(formatted);
  }

  @HostListener('blur')
  onBlur() {
    const input = this.el.nativeElement;
    const digits = (input.value || '').replace(/\D/g, '');
    if (digits.length === 2 || digits.length === 4) {
      const now = new Date();
      const day = digits.slice(0, 2);
      const month = digits.length >= 4 ? digits.slice(2, 4) : String(now.getMonth() + 1).padStart(2, '0');
      const year = String(now.getFullYear());
      const formatted = `${day}-${month}-${year}`;
      this.setValue(formatted);
    }
  }

  private format(digits: string): string {
    if (digits.length <= 2) return digits;
    if (digits.length <= 4) return `${digits.slice(0, 2)}-${digits.slice(2)}`;
    return `${digits.slice(0, 2)}-${digits.slice(2, 4)}-${digits.slice(4)}`;
  }

  private setValue(value: string) {
    this.el.nativeElement.value = value;
    if (this.ngControl && this.ngControl.control) {
      this.ngControl.control.setValue(value, { emitEvent: false });
    }
  }
}
