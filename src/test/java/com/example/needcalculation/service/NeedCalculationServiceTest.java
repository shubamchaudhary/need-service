package com.example.needcalculation.service;

import com.example.needcalculation.dto.NeedCalculationRequest;
import com.example.needcalculation.dto.NeedCalculationResponse;
import com.example.needcalculation.entity.ForecastData;
import com.example.needcalculation.enums.StoreSize;
import com.example.needcalculation.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NeedCalculationService
 * This demonstrates how to test the business logic using Mockito
 */
@ExtendWith(MockitoExtension.class)
class NeedCalculationServiceTest {

    @Mock
    private CsvDataLoaderService csvDataLoaderService;

    @Mock
    private StoreConfigurationService storeConfigurationService;

    @InjectMocks
    private NeedCalculationService needCalculationService;

    private ForecastData sampleForecastData;

    @BeforeEach
    void setUp() {
        // Create sample forecast data
        sampleForecastData = new ForecastData();
        sampleForecastData.setProductName("Bisleri-1L");
        sampleForecastData.setMonth("December");
        sampleForecastData.setRegion("extreme_north");
        sampleForecastData.setBaseNeedXs(100);
        sampleForecastData.setProductForecastKey("Bisleri-1L#December#extreme_north");
    }

    @Test
    void testCalculateNeeds_WithSingleStore() {
        // Arrange
        Store store = new Store("str1", "extreme_north", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("December");
        request.setStores(List.of(store));

        // Mock the dependencies
        when(storeConfigurationService.getStoreSize("str1"))
                .thenReturn(StoreSize.S); // Size S has factor 1.5

        when(csvDataLoaderService.findByProductMonthAndRegion(
                "Bisleri-1L", "December", "extreme_north"))
                .thenReturn(Optional.of(sampleForecastData));

        // Act
        NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getNeedPerSKU().size());

        Map<String, NeedCalculationResponse.SkuNeed> firstSku =
                response.getNeedPerSKU().get(0);
        assertTrue(firstSku.containsKey("Bisleri-1L@str1"));

        // Base need 100 * 1.5 (S size factor) = 150
        assertEquals(150, firstSku.get("Bisleri-1L@str1").getNeed());
    }

    @Test
    void testCalculateNeeds_WithMultipleStores() {
        // Arrange
        Store store1 = new Store("str1", "extreme_north", null);
        Store store2 = new Store("str2", "extreme_north", null);

        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("December");
        request.setStores(Arrays.asList(store1, store2));

        // Mock the dependencies
        when(storeConfigurationService.getStoreSize("str1"))
                .thenReturn(StoreSize.S); // Size S has factor 1.5
        when(storeConfigurationService.getStoreSize("str2"))
                .thenReturn(StoreSize.M); // Size M has factor 2.0

        when(csvDataLoaderService.findByProductMonthAndRegion(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(sampleForecastData));

        // Act
        NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getNeedPerSKU().size());

        // Verify calculations
        Map<String, NeedCalculationResponse.SkuNeed> sku1 =
                response.getNeedPerSKU().get(0);
        Map<String, NeedCalculationResponse.SkuNeed> sku2 =
                response.getNeedPerSKU().get(1);

        // Store 1: Base need 100 * 1.5 = 150
        assertTrue(sku1.containsKey("Bisleri-1L@str1"));
        assertEquals(150, sku1.get("Bisleri-1L@str1").getNeed());

        // Store 2: Base need 100 * 2.0 = 200
        assertTrue(sku2.containsKey("Bisleri-1L@str2"));
        assertEquals(200, sku2.get("Bisleri-1L@str2").getNeed());
    }

    @Test
    void testCalculateNeeds_NoForecastData() {
        // Arrange
        Store store = new Store("str1", "unknown_region", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("December");
        request.setStores(List.of(store));

        when(storeConfigurationService.getStoreSize("str1"))
                .thenReturn(StoreSize.S);

        when(csvDataLoaderService.findByProductMonthAndRegion(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getNeedPerSKU().size());

        Map<String, NeedCalculationResponse.SkuNeed> firstSku =
                response.getNeedPerSKU().get(0);

        // Should return 0 when no forecast data found
        assertEquals(0, firstSku.get("Bisleri-1L@str1").getNeed());
    }

    @Test
    void testCalculateNeeds_DifferentStoreSizes() {
        // Arrange
        Store store = new Store("str1", "extreme_north", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setMonth("December");
        request.setStores(List.of(store));

        when(csvDataLoaderService.findByProductMonthAndRegion(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(sampleForecastData));

        // Test with XL size (factor 5.0)
        when(storeConfigurationService.getStoreSize("str1"))
                .thenReturn(StoreSize.XL);

        // Act
        NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

        // Assert
        Map<String, NeedCalculationResponse.SkuNeed> sku =
                response.getNeedPerSKU().get(0);

        // Base need 100 * 5.0 = 500
        assertEquals(500, sku.get("Bisleri-1L@str1").getNeed());
    }

    @Test
    void testCalculateNeeds_CurrentMonthWhenNotProvided() {
        // Arrange
        Store store = new Store("str1", "extreme_north", null);
        NeedCalculationRequest request = new NeedCalculationRequest();
        request.setProductName("Bisleri-1L");
        request.setStores(List.of(store));
        // Month not set - should use current month

        when(storeConfigurationService.getStoreSize("str1"))
                .thenReturn(StoreSize.S);

        when(csvDataLoaderService.findByProductMonthAndRegion(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(sampleForecastData));

        // Act
        NeedCalculationResponse response = needCalculationService.calculateNeeds(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getNeedPerSKU().size());
        // Should still calculate properly with current month
    }
}