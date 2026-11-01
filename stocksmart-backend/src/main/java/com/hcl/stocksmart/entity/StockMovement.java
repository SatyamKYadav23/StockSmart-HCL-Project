package com.hcl.stocksmart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "movement_type", nullable = false, length = 30)
    private String movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 255)
    private String remarks;

    @Column(name = "direction", length = 10)
    private String direction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Returns structured direction ("IN" or "OUT") with backward-compatible fallback for historical records.
     */
    public String getEffectiveDirection() {
        if (direction != null && !direction.isBlank()) {
            return direction.trim().toUpperCase();
        }
        if ("RECEIPT".equalsIgnoreCase(movementType)) return "IN";
        if ("SALE".equalsIgnoreCase(movementType)) return "OUT";
        if ("TRANSFER".equalsIgnoreCase(movementType)) {
            return (remarks != null && remarks.toLowerCase().contains("out")) ? "OUT" : "IN";
        }
        if ("ADJUSTMENT".equalsIgnoreCase(movementType)) {
            return (remarks != null && remarks.toLowerCase().contains("decreased")) ? "OUT" : "IN";
        }
        return "IN";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}