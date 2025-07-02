package com.example.needcalculation.service;

import com.example.needcalculation.entity.ForecastData;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for loading forecast data from CSV file.
 * This simulates database operations while reading from CSV.
 *
 * @PostConstruct ensures the CSV is loaded when the application starts.
 */
@Service
@Slf4j
public class CsvDataLoaderService {

    @Value("${csv.file.path}")
    private String csvFilePath;

    // In-memory storage for forecast data (simulating database)
    private Map<String, ForecastData> forecastDataMap = new HashMap<>();

    /**
     * Load CSV data into memory when the application starts
     */
    @PostConstruct
    public void loadCsvData() {
        log.info("Loading forecast data from CSV file: {}", csvFilePath);

        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            List<String[]> rows = reader.readAll();

            // Skip header row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length >= 4) {
                    ForecastData data = new ForecastData();
                    data.setMonth(row[0].trim());
                    data.setRegion(row[1].trim());
                    data.setProductName(row[2].trim());
                    data.setBaseNeedXs(Integer.parseInt(row[3].trim()));

                    // Generate composite key
                    String key = ForecastData.generateKey(
                            data.getProductName(),
                            data.getMonth(),
                            data.getRegion()
                    );
                    data.setProductForecastKey(key);

                    forecastDataMap.put(key, data);
                }
            }

            log.info("Successfully loaded {} forecast records", forecastDataMap.size());

        } catch (IOException | CsvException e) {
            log.error("Error loading CSV data: ", e);
            throw new RuntimeException("Failed to load forecast data from CSV", e);
        }
    }

    /**
     * Find forecast data by composite key
     */
    public Optional<ForecastData> findByKey(String key) {
        return Optional.ofNullable(forecastDataMap.get(key));
    }

    /**
     * Find forecast data by product, month, and region
     */
    public Optional<ForecastData> findByProductMonthAndRegion(String productName, String month, String region) {
        String key = ForecastData.generateKey(productName, month, region);
        return findByKey(key);
    }

    /**
     * Get all forecast data
     */
    public List<ForecastData> findAll() {
        return new ArrayList<>(forecastDataMap.values());
    }

    /**
     * Get all unique regions
     */
    public List<String> getAllRegions() {
        return forecastDataMap.values().stream()
                .map(ForecastData::getRegion)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all unique products
     */
    public List<String> getAllProducts() {
        return forecastDataMap.values().stream()
                .map(ForecastData::getProductName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}