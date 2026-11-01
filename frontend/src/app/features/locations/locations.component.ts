import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LocationService } from '../../core/services/location.service';
import { InventoryService } from '../../core/services/inventory.service';
import { Location, LocationRequest, InventoryResponse } from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent
} from '../../shared';

export interface LocationSummaryRow extends Location {
  totalSkus: number;
  totalUnits: number;
}

@Component({
  selector: 'app-locations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    IconComponent,
    BadgeComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent
  ],
  templateUrl: './locations.component.html',
  styleUrl: './locations.component.css'
})
export class LocationsComponent implements OnInit {
  private readonly locationService = inject(LocationService);
  private readonly inventoryService = inject(InventoryService);
  private readonly fb = inject(FormBuilder);

  readonly locations = signal<Location[]>([]);
  readonly inventory = signal<InventoryResponse[]>([]);

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to retrieve facilities from server.');

  readonly searchQuery = signal('');

  // Modal signals
  readonly isAddModalOpen = signal(false);
  readonly isSubmitting = signal(false);
  readonly formErrorMessage = signal<string | null>(null);

  // Toast feedback
  readonly toastMessage = signal<string | null>(null);
  private toastTimer: any = null;

  locationForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    address: ['', [Validators.maxLength(255)]],
    locationType: ['WAREHOUSE', [Validators.required]]
  });

  // Computed enriched list of locations with real inventory totals
  readonly locationRows = computed<LocationSummaryRow[]>(() => {
    const locs = this.locations();
    const inv = this.inventory();

    return locs.map((loc) => {
      const matchingInventory = inv.filter((item) => item.locationId === loc.id);
      const uniqueSkus = new Set(matchingInventory.map((i) => i.productId)).size;
      const totalUnits = matchingInventory.reduce((sum, item) => sum + (item.quantity || 0), 0);

      return {
        ...loc,
        totalSkus: uniqueSkus,
        totalUnits: totalUnits
      };
    });
  });

  // Filtered computed rows
  readonly filteredLocations = computed<LocationSummaryRow[]>(() => {
    const rows = this.locationRows();
    const q = this.searchQuery().trim().toLowerCase();

    return rows.filter((loc) => {
      return (
        !q ||
        loc.name.toLowerCase().includes(q) ||
        loc.locationType.toLowerCase().includes(q) ||
        (loc.address && loc.address.toLowerCase().includes(q))
      );
    });
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.locationService.getLocations().subscribe({
      next: (locs) => {
        this.locations.set(locs);
        // Also fetch inventory to accurately calculate SKU & unit aggregates
        this.inventoryService.getInventory().subscribe({
          next: (inv) => {
            this.inventory.set(inv);
            this.isLoading.set(false);
          },
          error: () => {
            // Inventory load failure is non-fatal for locations list
            this.isLoading.set(false);
          }
        });
      },
      error: (err: Error) => {
        this.errorMessage.set(err.message || 'Failed to connect to location service.');
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  openAddModal(): void {
    this.formErrorMessage.set(null);
    this.locationForm.reset({
      name: '',
      address: '',
      locationType: 'WAREHOUSE'
    });
    this.isAddModalOpen.set(true);
  }

  closeAddModal(): void {
    if (this.isSubmitting()) return;
    this.isAddModalOpen.set(false);
    this.formErrorMessage.set(null);
  }

  onSubmitLocation(): void {
    if (this.locationForm.invalid) {
      this.locationForm.markAllAsTouched();
      return;
    }

    const val = this.locationForm.value;
    const request: LocationRequest = {
      name: val.name.trim(),
      address: val.address ? val.address.trim() : null,
      locationType: val.locationType
    };

    this.isSubmitting.set(true);
    this.formErrorMessage.set(null);

    this.locationService.createLocation(request).subscribe({
      next: (created) => {
        this.isSubmitting.set(false);
        this.closeAddModal();
        this.showToast(`Facility "${created.name}" created successfully.`);
        this.loadData();
      },
      error: (err: Error) => {
        this.isSubmitting.set(false);
        this.formErrorMessage.set(err.message || 'Failed to create facility.');
      }
    });
  }

  showToast(msg: string): void {
    this.toastMessage.set(msg);
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.toastMessage.set(null);
    }, 3500);
  }
}
