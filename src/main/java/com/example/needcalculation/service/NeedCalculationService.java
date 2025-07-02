package com.example.needcalculation.service;

import com.example.needcalculation.dto.NeedCalculationRequest;
import com.example.needcalculation.dto.NeedCalculationResponse;
import com.example.needcalculation.entity.ForecastData;
import com.example.needcalculation.enums.StoreSize;
import com.example.needcalculation.model.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Core service containing the business logic for need calculation.
 * This service:
 * 1. Creates SKUs (Stock Keeping Units) from product and store combinations
 * 2. Calculates needs based on forecast data and store size
 * 3. Returns the calculated needs for each SKU
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NeedCalculationService {

    private final CsvDataLoaderService csvDataLoaderService;
    private final StoreConfigurationService storeConfigurationService;

    /**
     * Main method to calculate needs for given request
     *
     * @param request NeedCalculationRequest containing product and stores
     * @return NeedCalculationResponse with calculated needs per SKU
     */
    public NeedCalculationResponse calculateNeeds(NeedCalculationRequest request) {
        log.info("Calculating needs for product: {} with {} stores",
                request.getProductName(), request.getStores().size());

        // Determine the month for calculation
        String month = determineMonth(request.getMonth());
        log.debug("Using month: {} for calculation", month);

        // Process each store and calculate needs
        List<Map<String, NeedCalculationResponse.SkuNeed>> needPerSKU = new ArrayList<>();

        for (Store store : request.getStores()) {
            try {
                // Get store size
                StoreSize storeSize = storeConfigurationService.getStoreSize(store.getStoreName());
                store.setStoreSize(storeSize.getCode());

                // Calculate need for this SKU
                Integer need = calculateNeedForSKU(
                        request.getProductName(),
                        store,
                        month,
                        storeSize
                );

                // Create SKU identifier (product@store)
                String sku = createSKU(request.getProductName(), store.getStoreName());

                // Create response entry
                Map<String, NeedCalculationResponse.SkuNeed> skuNeedMap = new HashMap<>();
                skuNeedMap.put(sku, new NeedCalculationResponse.SkuNeed(need));
                needPerSKU.add(skuNeedMap);

                log.debug("Calculated need for SKU {}: {}", sku, need);

            } catch (Exception e) {
                log.error("Error calculating need for store {}: {}",
                        store.getStoreName(), e.getMessage());
                // Add zero need for failed calculations
                String sku = createSKU(request.getProductName(), store.getStoreName());
                Map<String, NeedCalculationResponse.SkuNeed> skuNeedMap = new HashMap<>();
                skuNeedMap.put(sku, new NeedCalculationResponse.SkuNeed(0));
                needPerSKU.add(skuNeedMap);
            }
        }

        return new NeedCalculationResponse(needPerSKU);
    }

    /**
     * Calculate need for a specific SKU (product-store combination)
     *
     * @param productName Name of the product
     * @param store Store information
     * @param month Month for calculation
     * @param storeSize Size of the store
     * @return Calculated need value
     */
    private Integer calculateNeedForSKU(String productName, Store store,
                                        String month, StoreSize storeSize) {
        // Find forecast data for the given product, month, and region
        Optional<ForecastData> forecastDataOpt = csvDataLoaderService
                .findByProductMonthAndRegion(productName, month, store.getRegion());

        if (forecastDataOpt.isEmpty()) {
            log.warn("No forecast data found for product: {}, month: {}, region: {}",
                    productName, month, store.getRegion());
            return 0;
        }

        ForecastData forecastData = forecastDataOpt.get();
        Integer baseNeed = forecastData.getBaseNeedXs();

        // Apply multiplication factor based on store size
        Double multipliedNeed = baseNeed * storeSize.getMultiplicationFactor();

        // Round to nearest integer
        return Math.round(multipliedNeed.floatValue());
    }

    /**
     * Create SKU identifier from product name and store name
     * Format: productName@storeName
     *
     * @param productName Name of the product
     * @param storeName Name of the store
     * @return SKU identifier
     */
    private String createSKU(String productName, String storeName) {
        return String.format("%s@%s", productName, storeName);
    }

    /**
     * Determine the month to use for calculation
     * If month is not provided in request, use current month
     *
     * @param requestMonth Month from request (can be null)
     * @return Month name to use for calculation
     */
    private String determineMonth(String requestMonth) {
        if (requestMonth != null && !requestMonth.trim().isEmpty()) {
            // Capitalize first letter
            return requestMonth.substring(0, 1).toUpperCase() +
                    requestMonth.substring(1).toLowerCase();
        }

        // Use current month if not provided
        return LocalDate.now().getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /**
     * Get available months from forecast data
     *
     * @return List of available months
     */
    public List<String> getAvailableMonths() {
        return csvDataLoaderService.findAll().stream()
                .map(ForecastData::getMonth)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Get available regions
     *
     * @return List of available regions
     */
    public List<String> getAvailableRegions() {
        return csvDataLoaderService.getAllRegions();
    }

    /**
     * Get available products
     *
     * @return List of available products
     */
    public List<String> getAvailableProducts() {
        return csvDataLoaderService.getAllProducts();
    }
}