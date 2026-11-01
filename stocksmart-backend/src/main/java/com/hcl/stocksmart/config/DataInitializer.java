package com.hcl.stocksmart.config;

import com.hcl.stocksmart.entity.Category;
import com.hcl.stocksmart.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        try {
            if (categoryRepository.count() == 0) {
                log.info("No categories detected. Seeding initial retail categories...");
                categoryRepository.saveAll(List.of(
                        Category.builder().name("Electronics & Appliances").description("Consumer electronics, peripherals, and office appliances").build(),
                        Category.builder().name("Footwear & Apparel").description("Branded retail apparel, footwear, and casual clothing").build(),
                        Category.builder().name("Home & Kitchen").description("Modern home accessories, kitchenware, and storage solutions").build(),
                        Category.builder().name("Health & Personal Care").description("Personal care goods, hygiene essentials, and grooming kits").build(),
                        Category.builder().name("Sports & Fitness").description("Sporting equipment, gym gear, and outdoor athletic merchandise").build(),
                        Category.builder().name("General Merchandise").description("General retail catalog merchandise").build()
                ));
                log.info("Default categories seeded successfully.");
            }
        } catch (Exception e) {
            log.warn("Could not seed default categories: {}", e.getMessage());
        }
    }
}

