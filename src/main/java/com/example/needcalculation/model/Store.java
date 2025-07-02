package com.example.needcalculation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * Model class representing a store in the request.
 * Contains store name and region information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @NotBlank(message = "Store name is required")
    private String storeName;

    @NotBlank(message = "Region is required")
    private String region;

    /**
     * Store size (xs, s, m, l, xl)
     * This will be fetched from a store configuration service or database
     * For now, we'll simulate this in the service layer
     */
    private String storeSize;
}