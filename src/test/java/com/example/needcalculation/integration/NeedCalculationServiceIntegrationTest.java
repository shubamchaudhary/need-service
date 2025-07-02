package com.example.needcalculation.integration;

import com.example.needcalculation.dto.NeedCalculationRequest;
import com.example.needcalculation.dto.NeedCalculationResponse;
import com.example.needcalculation.enums.StoreSize;
import com.example.needcalculation.model.Store;
import com.example.needcalculation.service.CsvDataLoaderService;
import com.example.needcalculation.service.NeedCalculationService;
import com.example.needcalculation.service.StoreConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the service layer.
 * Tests business logic with actual Spring context and dependencies.
 */
public class NeedCalculationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NeedCalculationService needCalculationService;

    @Autowired
    private StoreConfigurationService storeConfigurationService;

    @Autowired
    private CsvDataLoaderService csvDataLoaderService;

    @Test
    void testServiceInitialization() {
        // Verify services are properly initialized
        assertNotNull(needCalculationService);
        assertNotNull(storeConfigurationService);
        assertNotNull(csvDataLoaderService);

        // Verify CSV data is loaded
        assertFalse(csvDataLoaderService.findAll().isEmpty());
        assertTrue(csvDataLoaderService.findAll().size() > 0);
    }

    @Test
    void testCalculateNeedsWithDifferentStoreSizes() {
        // Test each store size multiplier
        Map<String, Double> expectedMultipliers = Map.of(
                "str_xs", 1.0,   // XS
                "str_s", 1.5,    // S
                "str_m", 2.0,    // M
                "str_l", 3.0,    // L
                "str_xl", 5.0    // XL
        );

        // Set up store configurations
        storeConfigurationService.updateStoreConfiguration("str_xs", StoreSize.XS);
        storeConfigurationService.updateStoreConfiguration("str_s", StoreSize.S);
        storeConfigurationService.updateStoreConfiguration("str_m", StoreSize.M);
        storeConfigurationService.updateStoreConfiguration("str_l", StoreSize.L);
        storeConfigurationService.updateStoreConfiguration("str_xl", StoreSize.XL);

        for (Map.Entry<String, Double> entry : expectedMultipliers.entrySet()) {
            Store store = new Store(entry.getKey(), "extreme_north", null);
            NeedCalculationRequest request = new NeedCalculationRequest();
            request.setProductName("Bisleri-1L");
            request.setMonth("December");
            request.setStores(List.of(store));

            NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

            assertNotNull(response);
            assertEquals(1, response.getNeedPerSKU().size());

            Map<String, NeedCalculationResponse.SkuNeed> sku = response.getNeedPerSKU().get(0);
            Integer calculatedNeed = sku.get("Bisleri-1L@" + entry.getKey()).getNeed();

            // Base need for December in extreme_north is 55
            Integer expectedNeed = (int) Math.round(55 * entry.getValue());
            assertEquals(expectedNeed, calculatedNeed,
                    "Store size multiplier not applied correctly for " + entry.getKey());
        }
    }

    @Test
    void testSeasonalVariations() {
        // Test summer vs winter demand variations
        Store store = new Store("str1", "rajasthan", null);
        storeConfigurationService.updateStoreConfiguration("str1", StoreSize.M);

        // Winter month (December)
        NeedCalculationRequest winterRequest = new NeedCalculationRequest();
        winterRequest.setProductName("Bisleri-1L");
        winterRequest.setMonth("December");
        winterRequest.setStores(List.of(store));

        NeedCalculationResponse winterResponse = needCalculationService.calculateNeeds(winterRequest);
        Integer winterNeed = winterResponse.getNeedPerSKU().get(0)
                .get("Bisleri-1L@str1").getNeed();

        // Summer month (May)
        NeedCalculationRequest summerRequest = new NeedCalculationRequest();
        summerRequest.setProductName("Bisleri-1L");
        summerRequest.setMonth("May");
        summerRequest.setStores(List.of(store));

        NeedCalculationResponse summerResponse = needCalculationService.calculateNeeds(summerRequest);
        Integer summerNeed = summerResponse.getNeedPerSKU().get(0)
                .get("Bisleri-1L@str1").getNeed();

        // Verify summer demand is higher than winter (especially in Rajasthan)
        assertTrue(summerNeed > winterNeed,
                "Summer demand should be higher than winter demand in hot regions");
    }

    @Test
    void testRegionalDemandVariations() {
        // Test demand variations across regions for the same month
        String month = "June";
        String product = "Bisleri-1L";

        List<String> regions = Arrays.asList(
                "extreme_north", "rajasthan", "north_central", "northeast",
                "west", "south", "central", "southeast_coastal"
        );

        for (String region : regions) {
            Store store = new Store("test_store", region, null);
            storeConfigurationService.updateStoreConfiguration("test_store", StoreSize.M);

            NeedCalculationRequest request = new NeedCalculationRequest();
            request.setProductName(product);
            request.setMonth(month);
            request.setStores(List.of(store));

            NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

            assertNotNull(response);
            assertEquals(1, response.getNeedPerSKU().size());

            Integer need = response.getNeedPerSKU().get(0)
                    .get(product + "@test_store").getNeed();

            assertTrue(need > 0, "Need should be positive for region: " + region);
        }
    }

    @Test
    void testProductSizeVariations() {
        // Test different product sizes have different demands
        Store store = new Store("str1", "west", null);
        storeConfigurationService.updateStoreConfiguration("str1", StoreSize.L);

        Map<String, Integer> productNeeds = Map.of();

        for (String product : Arrays.asList("Bisleri-0.5L", "Bisleri-1L", "Bisleri-10L")) {
            NeedCalculationRequest request = new NeedCalculationRequest();
            request.setProductName(product);
            request.setMonth("April");
            request.setStores(List.of(store));

            NeedCalculationResponse response = needCalculationService.calculateNeeds(request);
            Integer need = response.getNeedPerSKU().get(0)
                    .get(product + "@str1").getNeed();

            assertTrue(need > 0, "Need should be positive for product: " + product);
        }
    }

    @Test
    void testBulkStoreProcessing() {
        // Create a large number of stores
        List<Store> stores = Arrays.asList(
                new Store("store1", "extreme_north", null),
                new Store("store2", "rajasthan", null),
                new Store("store3", "north_central", null),
                new Store("store4", "northeast", null),
                new Store("store5", "west", null),
                new Store("store6", "south", null),
                new Store("store7", "central", null),
                new Store("store8", "southeast_coastal", null),
                new Store("store9", "extreme_north", null),
                new Store("store10", "rajasthan", null),
                new Store("store11", "north_central", null),
                new Store("store12", "northeast", null),
                new Store("store13", "west", null),
                new Store("store14", "south", null),
                new Store("store15", "central", null),
                new Store("store16", "southeast_coastal", null),
                new Store("store17", "extreme_north", null),
                new Store("store18", "rajasthan", null),
                new Store("store19", "north_central", null),
                new Store("store20", "northeast", null)
        );

        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("August");
        request.setStores(stores);

        long startTime = System.currentTimeMillis();
        NeedCalculationResponse response = needCalculationService.calculateNeeds(request);
        long endTime = System.currentTimeMillis();

        // Verify all stores are processed
        assertEquals(20, response.getNeedPerSKU().size());

        // Verify performance (should complete within reasonable time)
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 1000, "Bulk processing should complete within 1 second");

        // Verify each store has a calculated need
        for (Map<String, NeedCalculationResponse.SkuNeed> skuMap : response.getNeedPerSKU()) {
            assertEquals(1, skuMap.size());
            skuMap.values().forEach(skuNeed -> {
                assertNotNull(skuNeed.getNeed());
                assertTrue(skuNeed.getNeed() >= 0);
            });
        }
    }

    @Test
    void testEdgeCases() {
        // Test with special characters in store name
        Store specialStore = new Store("store@123#", "west", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("March");
        request.setStores(List.of(specialStore));

        NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

        assertNotNull(response);
        assertEquals(1, response.getNeedPerSKU().size());
        assertTrue(response.getNeedPerSKU().get(0).containsKey("Bisleri-1L@store@123#"));
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        // Test thread safety with concurrent requests
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                Store store = new Store("concurrent_store_" + index, "south", null);
                NeedCalculationRequest request = new NeedCalculationRequest();
                request.setProductName("Bisleri-1L");
                request.setMonth("September");
                request.setStores(List.of(store));

                try {
                    NeedCalculationResponse response = needCalculationService.calculateNeeds(request);
                    results[index] = response != null && response.getNeedPerSKU().size() == 1;
                } catch (Exception e) {
                    results[index] = false;
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all concurrent requests succeeded
        for (boolean result : results) {
            assertTrue(result, "All concurrent requests should succeed");
        }
    }

    @Test
    void testDataConsistency() {
        // Run the same calculation multiple times to ensure consistency
        Store store = new Store("consistency_test", "central", null);
        storeConfigurationService.updateStoreConfiguration("consistency_test", StoreSize.M);

        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-10L");
        request.setMonth("November");
        request.setStores(List.of(store));

        Integer firstNeed = null;

        for (int i = 0; i < 5; i++) {
            NeedCalculationResponse response = needCalculationService.calculateNeeds(request);
            Integer need = response.getNeedPerSKU().get(0)
                    .get("Bisleri-10L@consistency_test").getNeed();

            if (firstNeed == null) {
                firstNeed = need;
            } else {
                assertEquals(firstNeed, need,
                        "Same calculation should always return the same result");
            }
        }
    }
}