package com.hcl.stocksmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(

        @NotBlank(message = "Location name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String address,

        @NotBlank(message = "Location type is required")
        @Size(max = 50)
        String locationType
) {
}