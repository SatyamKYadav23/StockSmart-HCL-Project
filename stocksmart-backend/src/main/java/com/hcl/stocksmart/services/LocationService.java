package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.LocationRequest;
import com.hcl.stocksmart.dto.LocationResponse;
import com.hcl.stocksmart.entity.Location;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.InventoryRepository;
import com.hcl.stocksmart.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public LocationResponse createLocation(LocationRequest request) {

        if (locationRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(
                    "Location already exists with name: " + request.name()
            );
        }

        Location location = Location.builder()
                .name(request.name())
                .address(request.address())
                .locationType(request.locationType())
                .build();

        return mapToResponse(locationRepository.save(location));
    }

    public List<LocationResponse> getAllLocations() {

        return locationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public LocationResponse getLocationById(Long id) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Location not found with id: " + id
                        )
                );

        return mapToResponse(location);
    }

    @Transactional
    public LocationResponse updateLocation(
            Long id,
            LocationRequest request) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Location not found with id: " + id
                        )
                );

        if (!location.getName().equals(request.name())
                && locationRepository.existsByName(request.name())) {

            throw new DuplicateResourceException(
                    "Location already exists with name: "
                            + request.name()
            );
        }

        location.setName(request.name());
        location.setAddress(request.address());
        location.setLocationType(request.locationType());

        return mapToResponse(locationRepository.save(location));
    }

    @Transactional
    public void deleteLocation(Long id) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Location not found with id: " + id
                        )
                );

        if (inventoryRepository.existsByLocationId(id)) {
            throw new InvalidStockOperationException(
                    "Cannot delete location '" + location.getName() + "' because it has associated inventory records."
            );
        }

        locationRepository.delete(location);
    }

    private LocationResponse mapToResponse(Location location) {

        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getLocationType(),
                location.getCreatedAt()
        );
    }
}