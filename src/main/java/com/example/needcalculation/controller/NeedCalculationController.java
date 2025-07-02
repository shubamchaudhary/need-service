package com.example.needcalculation.controller;

import com.example.needcalculation.dto.NeedCalculationRequest;
import com.example.needcalculation.dto.NeedCalculationResponse;
import com.example.needcalculation.service.NeedCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for need calculation endpoints.
 * This controller handles all HTTP requests related to need calculations.
 *
 * @RestController: Combines @Controller and @ResponseBody
 * @RequestMapping: Base path for all endpoints in this controller
 * @RequiredArgsConstructor: Lombok creates constructor for final fields
 */
@RestController
@RequestMapping("/api/v1/need-calculation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow CORS for testing
public class NeedCalculationController {

    private final NeedCalculationService needCalculationService;

    /**
     * Main endpoint to calculate needs based on request
     *
     * @param request NeedCalculationRequest with product and stores
     * @return NeedCalculationResponse with calculated needs
     */
    @PostMapping("/calculate")
    public ResponseEntity<NeedCalculationResponse> calculateNeeds(
            @Valid @RequestBody NeedCalculationRequest request) {

        log.info("Received need calculation request for product: {}",
                request.getProductName());

        try {
            NeedCalculationResponse response = needCalculationService.calculateNeeds(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing need calculation request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available configuration data (months, regions, products)
     * This is helpful for UI to show available options
     *
     * @return Map containing available months, regions, and products
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = new HashMap<>();

        try {
            config.put("availableMonths", needCalculationService.getAvailableMonths());
            config.put("availableRegions", needCalculationService.getAvailableRegions());
            config.put("availableProducts", needCalculationService.getAvailableProducts());
            config.put("storeSizes", List.of("xs", "s", "m", "l", "xl"));

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            log.error("Error fetching configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     *
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Need Calculation Service");
        return ResponseEntity.ok(health);
    }

    /**
     * Get service information
     *
     * @return Service information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Need Calculation Service");
        info.put("version", "1.0.0");
        info.put("description", "Service to calculate product needs based on forecast data");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("calculate", "POST /api/v1/need-calculation/calculate");
        endpoints.put("config", "GET /api/v1/need-calculation/config");
        endpoints.put("health", "GET /api/v1/need-calculation/health");

        info.put("endpoints", endpoints);

        return ResponseEntity.ok(info);
    }
}