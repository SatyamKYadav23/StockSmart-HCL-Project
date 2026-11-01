package com.hcl.stocksmart;

import com.hcl.stocksmart.dto.StockAdjustmentRequest;
import com.hcl.stocksmart.entity.Inventory;
import com.hcl.stocksmart.entity.Location;
import com.hcl.stocksmart.entity.Product;
import com.hcl.stocksmart.entity.StockMovement;
import com.hcl.stocksmart.repository.InventoryRepository;
import com.hcl.stocksmart.repository.LocationRepository;
import com.hcl.stocksmart.repository.ProductRepository;
import com.hcl.stocksmart.repository.StockMovementRepository;
import com.hcl.stocksmart.services.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private Long testProductId;
    private Long testLocationId;

    @BeforeEach
    void setUp() {
        List<Product> products = productRepository.findAll();
        assertFalse(products.isEmpty(), "At least one product must exist in database");
        testProductId = products.get(0).getId();

        List<Location> locations = locationRepository.findAll();
        assertFalse(locations.isEmpty(), "At least one location must exist in database");
        testLocationId = locations.get(0).getId();
    }

    @Nested
    @DisplayName("1. Security Headers & CORS Tests")
    class SecurityHeadersAndCorsTests {

        @Test
        @DisplayName("Security headers must be present on API responses")
        void testSecurityHeaders() throws Exception {
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("CORS preflight request must allow configured origin")
        void testCorsPreflight() throws Exception {
            mockMvc.perform(options("/api/products")
                            .header("Origin", "http://localhost:4200")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        }
    }

    @Nested
    @DisplayName("2. API Input Validation & Exception Handling")
    class ValidationAndExceptionHandlingTests {

        @Test
        @DisplayName("Validation failure returns 400 Bad Request with field error details")
        void testCategoryValidationFailure() throws Exception {
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"\", \"description\": \"Test\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Category name is required")));
        }

        @Test
        @DisplayName("Product validation failure for required fields returns 400 Bad Request")
        void testProductValidationFailure() throws Exception {
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sku\": \"\", \"name\": \"\", \"price\": -5.00}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Malformed JSON returns 400 Bad Request with shielded safe message")
        void testMalformedJsonHandling() throws Exception {
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": broken_json}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid field format."));
        }

        @Test
        @DisplayName("Type mismatch on path variable returns clean 400 Bad Request")
        void testTypeMismatchHandling() throws Exception {
            mockMvc.perform(get("/api/products/not-a-valid-id"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid parameter 'id'")));
        }

        @Test
        @DisplayName("Resource not found returns clean 404 Not Found")
        void testResourceNotFoundHandling() throws Exception {
            mockMvc.perform(get("/api/products/99999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")));
        }

        @Test
        @DisplayName("Cannot delete location with existing inventory returns 409 Conflict")
        void testCannotDeleteLocationWithInventory() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/locations/" + testLocationId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("associated inventory records")));
        }

        @Test
        @DisplayName("Cannot delete product with existing inventory returns 409 Conflict")
        void testCannotDeleteProductWithInventory() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/products/" + testProductId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("associated inventory records")));
        }

        @Test
        @DisplayName("Cannot delete category with associated products returns 409 Conflict")
        void testCannotDeleteCategoryWithProducts() throws Exception {
            Product product = productRepository.findById(testProductId).orElseThrow();
            Long categoryId = product.getCategory().getId();
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/categories/" + categoryId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("associated with existing products")));
        }
    }

    @Nested
    @DisplayName("3. Structured Movement Direction & Fallback Tests")
    class StructuredMovementDirectionTests {

        @Test
        @DisplayName("getEffectiveDirection() returns explicit direction when present")
        void testExplicitDirection() {
            StockMovement inMovement = StockMovement.builder()
                    .movementType("TRANSFER")
                    .direction("IN")
                    .build();
            assertEquals("IN", inMovement.getEffectiveDirection());

            StockMovement outMovement = StockMovement.builder()
                    .movementType("TRANSFER")
                    .direction("OUT")
                    .build();
            assertEquals("OUT", outMovement.getEffectiveDirection());

            StockMovement lowerMovement = StockMovement.builder()
                    .movementType("ADJUSTMENT")
                    .direction("out")
                    .build();
            assertEquals("OUT", lowerMovement.getEffectiveDirection());
        }

        @Test
        @DisplayName("getEffectiveDirection() falls back correctly when direction is null (legacy compatibility)")
        void testLegacyFallback() {
            // RECEIPT -> IN
            StockMovement receipt = StockMovement.builder()
                    .movementType("RECEIPT")
                    .direction(null)
                    .build();
            assertEquals("IN", receipt.getEffectiveDirection());

            // SALE -> OUT
            StockMovement sale = StockMovement.builder()
                    .movementType("SALE")
                    .direction(null)
                    .build();
            assertEquals("OUT", sale.getEffectiveDirection());

            // TRANSFER Out
            StockMovement transferOut = StockMovement.builder()
                    .movementType("TRANSFER")
                    .direction(null)
                    .remarks("Transfer Out to Regional Hub")
                    .build();
            assertEquals("OUT", transferOut.getEffectiveDirection());

            // TRANSFER In
            StockMovement transferIn = StockMovement.builder()
                    .movementType("TRANSFER")
                    .direction(null)
                    .remarks("Transfer In from Central Warehouse")
                    .build();
            assertEquals("IN", transferIn.getEffectiveDirection());

            // ADJUSTMENT Decreased
            StockMovement adjDown = StockMovement.builder()
                    .movementType("ADJUSTMENT")
                    .direction(null)
                    .remarks("Stock decreased by 5 units - Damaged items")
                    .build();
            assertEquals("OUT", adjDown.getEffectiveDirection());

            // ADJUSTMENT Increased
            StockMovement adjUp = StockMovement.builder()
                    .movementType("ADJUSTMENT")
                    .direction(null)
                    .remarks("Stock increased by 10 units - Found inventory")
                    .build();
            assertEquals("IN", adjUp.getEffectiveDirection());
        }

        @Test
        @DisplayName("adjustStock sets explicit direction IN for positive adjustment and OUT for negative adjustment")
        void testAdjustmentDirectionIntegration() {
            // Positive adjustment
            StockAdjustmentRequest inRequest = new StockAdjustmentRequest(
                    testProductId,
                    testLocationId,
                    15,
                    "TEST-SEC-IN",
                    "Security positive adjustment"
            );
            inventoryService.adjustStock(inRequest);

            List<StockMovement> inMovements = stockMovementRepository.findAll().stream()
                    .filter(m -> "TEST-SEC-IN".equals(m.getReferenceType()))
                    .toList();
            assertFalse(inMovements.isEmpty(), "Audit movement for positive adjustment must exist");
            assertEquals("IN", inMovements.get(0).getDirection());
            assertEquals("IN", inMovements.get(0).getEffectiveDirection());

            // Negative adjustment
            StockAdjustmentRequest outRequest = new StockAdjustmentRequest(
                    testProductId,
                    testLocationId,
                    -5,
                    "TEST-SEC-OUT",
                    "Security negative adjustment"
            );
            inventoryService.adjustStock(outRequest);

            List<StockMovement> outMovements = stockMovementRepository.findAll().stream()
                    .filter(m -> "TEST-SEC-OUT".equals(m.getReferenceType()))
                    .toList();
            assertFalse(outMovements.isEmpty(), "Audit movement for negative adjustment must exist");
            assertEquals("OUT", outMovements.get(0).getDirection());
            assertEquals("OUT", outMovements.get(0).getEffectiveDirection());
        }
    }
}
