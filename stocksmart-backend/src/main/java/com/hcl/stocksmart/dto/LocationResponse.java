package com.hcl.stocksmart.dto;

import java.time.LocalDateTime;

public record LocationResponse(

        Long id,
        String name,
        String address,
        String locationType,
        LocalDateTime createdAt
) {
}