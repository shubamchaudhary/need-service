package com.example.needcalculation.service;

import com.example.needcalculation.enums.StoreSize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Service to manage store configurations.
 * In a real application, this would fetch store details from a database or another service.
 * For this demo, we'll simulate store sizes.
 */
@Service
@Slf4j
public class StoreConfigurationService {

    // Simulated store configuration database
    private Map<String, StoreSize> storeConfigurations = new HashMap<>();

    /**
     * Initialize some sample store configurations
     * In production, this would be loaded from a database
     */
    @PostConstruct
    public void initializeStoreConfigurations() {
        log.info("Initializing store configurations...");

        // Sample store configurations
        storeConfigurations.put("str1", StoreSize.S);
        storeConfigurations.put("str2", StoreSize.M);
        storeConfigurations.put("str3", StoreSize.XS);
        storeConfigurations.put("str4", StoreSize.L);
        storeConfigurations.put("str5", StoreSize.XL);
        storeConfigurations.put("str6", StoreSize.M);
        storeConfigurations.put("str7", StoreSize.S);
        storeConfigurations.put("str8", StoreSize.L);
        storeConfigurations.put("str9", StoreSize.XS);
        storeConfigurations.put("str10", StoreSize.XL);

        log.info("Initialized {} store configurations", storeConfigurations.size());
    }

    /**
     * Get store size for a given store name
     * If store is not found, randomly assign a size (for demo purposes)
     *
     * @param storeName Name of the store
     * @return StoreSize enum
     */
    public StoreSize getStoreSize(String storeName) {
        StoreSize size = storeConfigurations.get(storeName);

        if (size == null) {
            // For demo: randomly assign a size if store not found
            StoreSize[] sizes = StoreSize.values();
            size = sizes[new Random().nextInt(sizes.length)];

            log.warn("Store '{}' not found in configuration. Assigned random size: {}",
                    storeName, size);

            // Cache it for consistency
            storeConfigurations.put(storeName, size);
        }

        return size;
    }

    /**
     * Add or update store configuration
     *
     * @param storeName Name of the store
     * @param size Size of the store
     */
    public void updateStoreConfiguration(String storeName, StoreSize size) {
        storeConfigurations.put(storeName, size);
        log.info("Updated store configuration: {} -> {}", storeName, size);
    }

    /**
     * Get all store configurations
     *
     * @return Map of store name to store size
     */
    public Map<String, StoreSize> getAllStoreConfigurations() {
        return new HashMap<>(storeConfigurations);
    }
}