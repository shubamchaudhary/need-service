package com.example.needcalculation.dto;

import com.example.needcalculation.model.Store;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Data Transfer Object for the need calculation request.
 * This represents the incoming JSON request structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeedCalculationRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotEmpty(message = "At least one store is required")
    @Valid
    private List<Store> stores;

    /**
     * Optional: Month for calculation. If not provided, current month will be used.
     */
    private String month;
}