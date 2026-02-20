import { Directive, ElementRef, Input, OnChanges, OnDestroy, OnInit, Renderer2, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { AccessControlService } from '../core/access/access-control.service';
import { AccessControlConfigDialogComponent } from '../core/access/access-control-config-dialog.component';

@Directive({
  selector: '[appAccessControl]',
  standalone: true
})
export class AccessControlDirective implements OnInit, OnChanges, OnDestroy {
  @Input('appAccessControl') controlKey = '';
  @Input() appAccessFallbackRoles: string[] = [];
  @Input() appAccessHide = true;
  @Input() appAccessConfigurable = true;

  private sub?: Subscription;
  private configButton?: HTMLButtonElement;
  private removeConfigListener?: () => void;

  constructor(
    private el: ElementRef<HTMLElement>,
    private renderer: Renderer2,
    private access: AccessControlService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.sub = this.access.changes$.subscribe(() => this.apply());
    this.apply();
  }

  ngOnChanges(_: SimpleChanges): void {
    this.apply();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.removeConfigButton();
  }

  private apply(): void {
    const allowed = this.access.can(this.controlKey, this.appAccessFallbackRoles);
    if (this.appAccessHide) {
      this.renderer.setStyle(this.el.nativeElement, 'display', allowed ? '' : 'none');
    } else {
      this.renderer.setProperty(this.el.nativeElement, 'disabled', !allowed);
    }
    this.renderConfigButton();
  }

  private renderConfigButton(): void {
    if (!this.appAccessConfigurable
      || !this.controlKey
      || !this.access.canConfigure()
      || !this.access.securityButtonsVisible()) {
      this.removeConfigButton();
      return;
    }
    if (this.configButton) return;

    const parent = this.el.nativeElement.parentNode;
    if (!parent) return;

    const button = this.renderer.createElement('button') as HTMLButtonElement;
    this.renderer.setAttribute(button, 'type', 'button');
    this.renderer.setAttribute(button, 'title', 'Configurar acesso');
    this.renderer.setAttribute(button, 'aria-label', `Configurar acesso de ${this.controlKey}`);
    this.renderer.addClass(button, 'acl-config-trigger');
    const icon = this.renderer.createElement('span');
    this.renderer.addClass(icon, 'icon-shield');
    this.renderer.appendChild(icon, this.renderer.createText('shield'));
    this.renderer.appendChild(button, icon);

    this.removeConfigListener = this.renderer.listen(button, 'click', (event: Event) => {
      event.preventDefault();
      event.stopPropagation();
      this.openDialog();
    });

    this.renderer.insertBefore(parent, button, this.el.nativeElement.nextSibling);
    this.configButton = button;
  }

  private removeConfigButton(): void {
    if (this.removeConfigListener) {
      this.removeConfigListener();
      this.removeConfigListener = undefined;
    }
    if (this.configButton?.parentNode) {
      this.configButton.parentNode.removeChild(this.configButton);
    }
    this.configButton = undefined;
  }

  private openDialog(): void {
    const current = this.access.getRoles(this.controlKey, this.appAccessFallbackRoles);
    this.dialog.open(AccessControlConfigDialogComponent, {
      width: '460px',
      maxWidth: '92vw',
      data: {
        title: `Configurar acesso de "${this.controlKey}"`,
        controlKey: this.controlKey,
        selectedRoles: current,
        fallbackRoles: this.appAccessFallbackRoles || []
      }
    }).afterClosed().subscribe((roles: string[] | undefined) => {
      if (!roles) return;
      this.access.setRoles(this.controlKey, roles);
    });
  }
}
