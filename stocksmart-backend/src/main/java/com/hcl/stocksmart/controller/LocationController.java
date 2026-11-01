package com.hcl.stocksmart.controller;

import com.hcl.stocksmart.dto.LocationRequest;
import com.hcl.stocksmart.dto.LocationResponse;
import com.hcl.stocksmart.services.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(
            @Valid @RequestBody LocationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(locationService.createLocation(request));
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAllLocations() {

        return ResponseEntity.ok(
                locationService.getAllLocations()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocationById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                locationService.getLocationById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request) {

        return ResponseEntity.ok(
                locationService.updateLocation(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable Long id) {

        locationService.deleteLocation(id);

        return ResponseEntity.noContent().build();
    }
}