package com.example.needcalculation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for the need calculation response.
 * This represents the outgoing JSON response structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeedCalculationResponse {

    /**
     * List of SKU needs where each entry is a map with SKU as key and need details as value.
     * Example: {"Bisleri-1L@str1": {"need": 100}}
     */
    private List<Map<String, SkuNeed>> needPerSKU;

    /**
     * Inner class representing the need value for a SKU
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuNeed {
        private Integer need;
    }
}