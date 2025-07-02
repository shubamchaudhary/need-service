package com.example.needcalculation.integration;

import com.example.needcalculation.dto.NeedCalculationRequest;
import com.example.needcalculation.dto.NeedCalculationResponse;
import com.example.needcalculation.model.Store;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for NeedCalculationController.
 * Tests all REST endpoints with real Spring context.
 */
public class NeedCalculationControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/need-calculation";

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get(BASE_URL + "/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Need Calculation Service"));
    }

    @Test
    void testInfoEndpoint() throws Exception {
        mockMvc.perform(get(BASE_URL + "/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("Need Calculation Service"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.endpoints").exists());
    }

    @Test
    void testConfigEndpoint() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableMonths").isArray())
                .andExpect(jsonPath("$.availableMonths", hasSize(12)))
                .andExpect(jsonPath("$.availableRegions").isArray())
                .andExpect(jsonPath("$.availableRegions", hasItem("extreme_north")))
                .andExpect(jsonPath("$.availableProducts").isArray())
                .andExpect(jsonPath("$.availableProducts", hasItem("Bisleri-1L")))
                .andExpect(jsonPath("$.storeSizes").isArray())
                .andExpect(jsonPath("$.storeSizes", hasSize(5)));
    }

    @Test
    void testCalculateNeeds_SingleStore_Success() throws Exception {
        // Arrange
        Store store = new Store("str1", "extreme_north", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("December");
        request.setStores(List.of(store));

        // Act & Assert
        MvcResult result = mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needPerSKU").isArray())
                .andExpect(jsonPath("$.needPerSKU", hasSize(1)))
                .andReturn();

        // Verify response structure
        String responseJson = result.getResponse().getContentAsString();
        NeedCalculationResponse response = fromJson(responseJson, NeedCalculationResponse.class);

        assertNotNull(response);
        assertEquals(1, response.getNeedPerSKU().size());

        Map<String, NeedCalculationResponse.SkuNeed> firstSku = response.getNeedPerSKU().get(0);
        assertTrue(firstSku.containsKey("Bisleri-1L@str1"));
        assertNotNull(firstSku.get("Bisleri-1L@str1").getNeed());
        assertTrue(firstSku.get("Bisleri-1L@str1").getNeed() > 0);
    }

    @Test
    void testCalculateNeeds_MultipleStores_Success() throws Exception {
        // Arrange
        Store store1 = new Store("str1", "extreme_north", null);
        Store store2 = new Store("str2", "rajasthan", null);
        Store store3 = new Store("str3", "west", null);
        Store store4 = new Store("str4", "south", null);

        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("May");
        request.setStores(Arrays.asList(store1, store2, store3, store4));

        // Act & Assert
        MvcResult result = mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needPerSKU").isArray())
                .andExpect(jsonPath("$.needPerSKU", hasSize(4)))
                .andReturn();

        // Verify all SKUs are present
        String responseJson = result.getResponse().getContentAsString();
        NeedCalculationResponse response = fromJson(responseJson, NeedCalculationResponse.class);

        assertEquals(4, response.getNeedPerSKU().size());

        // Verify each SKU has a calculated need
        for (int i = 0; i < 4; i++) {
            Map<String, NeedCalculationResponse.SkuNeed> sku = response.getNeedPerSKU().get(i);
            assertEquals(1, sku.size());
            sku.values().forEach(skuNeed -> {
                assertNotNull(skuNeed.getNeed());
                assertTrue(skuNeed.getNeed() >= 0);
            });
        }
    }

    @Test
    void testCalculateNeeds_DifferentProducts() throws Exception {
        // Test with different product sizes
        String[] products = {"Bisleri-0.5L", "Bisleri-1L", "Bisleri-10L"};

        for (String product : products) {
            Store store = new Store("str1", "west", null);
            NeedCalculationRequest request = new NeedCalculationRequest();
            request.setProductName(product);
            request.setMonth("June");
            request.setStores(List.of(store));

            mockMvc.perform(post(BASE_URL + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.needPerSKU").isArray())
                    .andExpect(jsonPath("$.needPerSKU", hasSize(1)))
                    .andExpect(jsonPath("$.needPerSKU[0]." + product + "@str1.need").exists());
        }
    }

    @Test
    void testCalculateNeeds_AllRegions() throws Exception {
        // Test calculation for all regions
        String[] regions = {
                "extreme_north", "rajasthan", "north_central", "northeast",
                "west", "south", "central", "southeast_coastal"
        };

        for (String region : regions) {
            Store store = new Store("testStore", region, null);
            NeedCalculationRequest request = new NeedCalculationRequest();
            request.setProductName("Bisleri-1L");
            request.setMonth("March");
            request.setStores(List.of(store));

            mockMvc.perform(post(BASE_URL + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.needPerSKU").isArray())
                    .andExpect(jsonPath("$.needPerSKU", hasSize(1)));
        }
    }

    @Test
    void testCalculateNeeds_AllMonths() throws Exception {
        // Test calculation for all months
        String[] months = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        for (String month : months) {
            Store store = new Store("str1", "south", null);
            NeedCalculationRequest request = new NeedCalculationRequest();
            request.setProductName("Bisleri-1L");
            request.setMonth(month);
            request.setStores(List.of(store));

            mockMvc.perform(post(BASE_URL + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.needPerSKU").isArray())
                    .andExpect(jsonPath("$.needPerSKU", hasSize(1)));
        }
    }

    @Test
    void testCalculateNeeds_CurrentMonthWhenNotSpecified() throws Exception {
        // Arrange - no month specified
        Store store = new Store("str1", "central", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setStores(List.of(store));
        // Month is intentionally not set

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needPerSKU").isArray())
                .andExpect(jsonPath("$.needPerSKU", hasSize(1)));
    }

    @Test
    void testCalculateNeeds_ValidationError_MissingProductName() throws Exception {
        // Arrange - missing product name
        Store store = new Store("str1", "extreme_north", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setStores(List.of(store));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.productName").value("Product name is required"));
    }

    @Test
    void testCalculateNeeds_ValidationError_EmptyStores() throws Exception {
        // Arrange - empty stores list
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setStores(List.of());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.stores").value("At least one store is required"));
    }

    @Test
    void testCalculateNeeds_ValidationError_InvalidStoreData() throws Exception {
        // Arrange - store with missing region
        Store store = new Store("str1", null, null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setStores(List.of(store));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors['stores[0].region']").value("Region is required"));
    }

    @Test
    void testCalculateNeeds_UnknownRegion_ReturnsZero() throws Exception {
        // Arrange - unknown region
        Store store = new Store("str1", "unknown_region", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("December");
        request.setStores(List.of(store));

        // Act & Assert
        MvcResult result = mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needPerSKU").isArray())
                .andExpect(jsonPath("$.needPerSKU", hasSize(1)))
                .andReturn();

        // Verify need is 0 for unknown region
        String responseJson = result.getResponse().getContentAsString();
        NeedCalculationResponse response = fromJson(responseJson, NeedCalculationResponse.class);

        Map<String, NeedCalculationResponse.SkuNeed> sku = response.getNeedPerSKU().get(0);
        assertEquals(0, sku.get("Bisleri-1L@str1").getNeed());
    }

    @Test
    void testCalculateNeeds_LargeScaleRequest() throws Exception {
        // Arrange - many stores
        List<Store> stores = Arrays.asList(
                new Store("str1", "extreme_north", null),
                new Store("str2", "rajasthan", null),
                new Store("str3", "north_central", null),
                new Store("str4", "northeast", null),
                new Store("str5", "west", null),
                new Store("str6", "south", null),
                new Store("str7", "central", null),
                new Store("str8", "southeast_coastal", null),
                new Store("str9", "extreme_north", null),
                new Store("str10", "rajasthan", null)
        );

        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("July");
        request.setStores(stores);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needPerSKU").isArray())
                .andExpect(jsonPath("$.needPerSKU", hasSize(10)));
    }
}