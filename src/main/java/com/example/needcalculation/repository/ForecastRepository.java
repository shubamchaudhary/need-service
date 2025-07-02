package com.example.needcalculation.repository;

import com.example.needcalculation.entity.ForecastData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ForecastData operations.
 * Extends JpaRepository which provides basic CRUD operations.
 *
 * Even though we're reading from CSV, we keep this repository pattern
 * for future database integration.
 */
@Repository
public interface ForecastRepository extends JpaRepository<ForecastData, String> {

    /**
     * Find forecast data by composite key
     * @param productForecastKey The composite key (productName#month#region)
     * @return Optional of ForecastData
     */
    Optional<ForecastData> findByProductForecastKey(String productForecastKey);

    /**
     * Find all forecast data for a specific month
     * @param month Month name
     * @return List of ForecastData
     */
    List<ForecastData> findByMonth(String month);

    /**
     * Find forecast data by product name, month and region
     * @param productName Product name
     * @param month Month name
     * @param region Region name
     * @return Optional of ForecastData
     */
    @Query("SELECT f FROM ForecastData f WHERE f.productName = :productName " +
            "AND f.month = :month AND f.region = :region")
    Optional<ForecastData> findByProductMonthAndRegion(
            @Param("productName") String productName,
            @Param("month") String month,
            @Param("region") String region
    );

    /**
     * Find all unique regions
     * @return List of unique region names
     */
    @Query("SELECT DISTINCT f.region FROM ForecastData f")
    List<String> findAllUniqueRegions();

    /**
     * Find all unique products
     * @return List of unique product names
     */
    @Query("SELECT DISTINCT f.productName FROM ForecastData f")
    List<String> findAllUniqueProducts();
}