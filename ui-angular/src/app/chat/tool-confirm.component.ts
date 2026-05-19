import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ToolInfo } from '../chat.models';

@Component({
  selector: 'app-tool-confirm',
  standalone: false,
  template: `
    <div class="d-flex justify-content-start mb-2 w-100">
      <div class="tool-confirm-card p-3">
        <div class="d-flex align-items-center mb-2 gap-2">
          <mat-icon class="text-primary" style="font-size:18px;width:18px;height:18px;">build</mat-icon>
          <strong style="font: var(--mat-sys-title-small)">Tool Confirmation</strong>
        </div>

        <div class="mb-3">
          <div class="fw-medium mb-1" style="font: var(--mat-sys-body-medium)">{{ toolInfo.name }}</div>
          @if (toolInfo.description) {
            <div class="text-muted mb-2" style="font: var(--mat-sys-body-small)">{{ toolInfo.description }}</div>
          }
          <div class="tool-params small">
            <strong>Parameters:</strong>
            <pre class="mb-0 mt-1 overflow-auto" style="max-height:90px;font-size:0.78rem;">{{ toolInfo.parameters | json }}</pre>
          </div>
        </div>

        <div class="d-flex gap-2 justify-content-end">
          <button mat-stroked-button (click)="reject.emit()">
            <mat-icon>close</mat-icon> Reject
          </button>
          <button mat-flat-button color="primary" (click)="approve.emit()">
            <mat-icon>check</mat-icon> Approve
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .tool-confirm-card {
      max-width: 78%;
      border-radius: var(--mat-sys-corner-large);
      border-top-left-radius: var(--mat-sys-corner-extra-small);
      border: 1px solid var(--mat-sys-primary);
      background: var(--mat-sys-surface-container-lowest);
      box-shadow: var(--mat-sys-level1);
    }
    .tool-params {
      background: var(--mat-sys-surface-container);
      border-radius: var(--mat-sys-corner-small);
      padding: 0.5em 0.75em;
    }
  `],
})
export class ToolConfirmComponent {
  @Input() toolInfo!: ToolInfo;
  @Output() approve = new EventEmitter<void>();
  @Output() reject = new EventEmitter<void>();
}
