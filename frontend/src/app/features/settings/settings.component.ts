import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IconComponent, BadgeComponent } from '../../shared';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, BadgeComponent],
  template: `
    <div class="feature-page-container">
      <div class="page-header">
        <div class="header-titles">
          <h2 class="page-title">System Settings</h2>
          <p class="page-subtitle">Inventory optimization parameters, API endpoints, and system configurations</p>
        </div>
        <div class="header-actions">
          <button type="button" class="ss-btn ss-btn-primary">
            <app-icon name="check-circle" [size]="14"></app-icon>
            <span>Save Configuration</span>
          </button>
        </div>
      </div>

      <div class="settings-grid">
        <!-- Backend API Configuration -->
        <div class="ss-card">
          <div class="ss-card-header">
            <div>
              <h3 class="ss-card-title">Backend API Architecture</h3>
              <p class="ss-card-subtitle">Spring Boot REST API endpoint integration status</p>
            </div>
            <app-badge variant="info" size="sm">Phase 1 Foundation</app-badge>
          </div>
          <div class="ss-card-body">
            <div class="setting-row">
              <div class="setting-label">
                <span class="setting-name">Spring Boot REST API Base URL</span>
                <span class="setting-desc">Configured via environment variables for core services</span>
              </div>
              <div class="setting-control font-mono">
                <input type="text" class="ss-input" [value]="apiUrl" readonly />
              </div>
            </div>

            <div class="setting-row">
              <div class="setting-label">
                <span class="setting-name">Backend Runtime</span>
                <span class="setting-desc">Target server environment</span>
              </div>
              <div class="setting-control font-mono text-sm text-secondary">
                Java 21 / Spring Boot 3.x / MySQL
              </div>
            </div>

            <div class="setting-row">
              <div class="setting-label">
                <span class="setting-name">HttpClient Interceptors</span>
                <span class="setting-desc">Global enterprise error handling & logging</span>
              </div>
              <div class="setting-control">
                <app-badge variant="success" size="sm">Active (errorInterceptor)</app-badge>
              </div>
            </div>
          </div>
        </div>

        <!-- Inventory Optimization Rules -->
        <div class="ss-card">
          <div class="ss-card-header">
            <div>
              <h3 class="ss-card-title">Inventory Replenishment Thresholds</h3>
              <p class="ss-card-subtitle">Automated alert generation and safety stock criteria</p>
            </div>
          </div>
          <div class="ss-card-body">
            <div class="setting-row">
              <div class="setting-label">
                <span class="setting-name">Low Stock Warning Threshold</span>
                <span class="setting-desc">Trigger alert when on-hand stock falls below reorder point</span>
              </div>
              <div class="setting-control">
                <select class="ss-select">
                  <option selected>100% of Reorder Level</option>
                  <option>120% of Reorder Level</option>
                  <option>80% of Reorder Level</option>
                </select>
              </div>
            </div>

            <div class="setting-row">
              <div class="setting-label">
                <span class="setting-name">Safety Stock Buffer Days</span>
                <span class="setting-desc">Default buffer added to lead time reorder quantities</span>
              </div>
              <div class="setting-control">
                <input type="number" class="ss-input" value="7" style="max-width: 100px;" />
              </div>
            </div>

            <div class="setting-row">
              <div class="setting-label">
                <span class="setting-name">Primary Operational Currency</span>
                <span class="setting-desc">Valuation display standard</span>
              </div>
              <div class="setting-control">
                <select class="ss-select">
                  <option selected>USD ($) - US Dollar</option>
                  <option>EUR (€) - Euro</option>
                  <option>GBP (£) - British Pound</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .feature-page-container {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      max-width: 1600px;
      margin: 0 auto;
    }
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
    }
    .page-subtitle {
      font-size: 0.8125rem;
      color: var(--text-muted);
      margin-top: 2px;
    }
    .settings-grid {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .setting-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 0;
      border-bottom: 1px solid var(--border-subtle);
      gap: 16px;
    }
    .setting-row:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }
    .setting-label {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .setting-name {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-primary);
    }
    .setting-desc {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .setting-control {
      min-width: 220px;
    }
  `]
})
export class SettingsComponent {
  readonly apiUrl = environment.apiUrl;
}
