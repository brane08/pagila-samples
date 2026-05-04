import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ToolInfo } from '../chat.models';

@Component({
  selector: 'app-tool-confirm',
  standalone: false,
  template: `
    <div class="d-flex justify-content-start mb-3">
      <div class="p-3 rounded-3 shadow-sm bg-white border border-primary max-w-75 min-w-50">
        <div class="d-flex align-items-center mb-3">
          <i class="fas fa-tools text-primary me-2 fs-5"></i>
          <strong class="fw-bold text-primary fs-5">Tool Confirmation</strong>
        </div>

        <div class="mb-3">
          <h6 class="mb-2 fw-bold">{{ toolInfo.name }}</h6>
          <p class="text-muted mb-3">{{ toolInfo.description }}</p>
          <div class="bg-light p-3 rounded-2 small">
            <strong>Parameters:</strong>
            <pre class="mb-0 mt-2 small overflow-auto" style="max-height: 100px; font-size: 0.8rem;">{{ toolInfo.parameters | json }}</pre>
          </div>
        </div>

        <div class="d-flex gap-2 justify-content-end">
          <button class="btn btn-outline-secondary btn-sm px-3" (click)="reject.emit()">
            <i class="fas fa-times me-1"></i>Reject
          </button>
          <button class="btn btn-primary btn-sm px-3" (click)="approve.emit()">
            <i class="fas fa-check me-1"></i>Approve
          </button>
        </div>
      </div>
    </div>
  `,
})
export class ToolConfirmComponent {
  @Input() toolInfo!: ToolInfo;
  @Output() approve = new EventEmitter<void>();
  @Output() reject = new EventEmitter<void>();
}
