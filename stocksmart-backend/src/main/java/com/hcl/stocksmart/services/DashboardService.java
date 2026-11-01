package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.DashboardResponse;
import com.hcl.stocksmart.repository.CategoryRepository;
import com.hcl.stocksmart.repository.InventoryRepository;
import com.hcl.stocksmart.repository.LocationRepository;
import com.hcl.stocksmart.repository.OrderRepository;
import com.hcl.stocksmart.repository.ProductRepository;
import com.hcl.stocksmart.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;


    public DashboardResponse getDashboard() {

        return new DashboardResponse(

                productRepository.count(),

                categoryRepository.count(),

                supplierRepository.count(),

                locationRepository.count(),

                inventoryRepository.count(),

                orderRepository.count(),

                productRepository.countLowStockProducts()
        );
    }
}