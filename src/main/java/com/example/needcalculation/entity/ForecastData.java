package com.example.needcalculation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing the forecast data.
 * This class maps to the forecast table in the database.
 * In this implementation, we're reading from CSV, but the structure is ready for DB integration.
 *
 * @Data: Lombok annotation that generates getters, setters, toString, equals, and hashCode
 * @NoArgsConstructor: Generates a no-argument constructor
 * @AllArgsConstructor: Generates a constructor with all fields
 */
@Entity
@Table(name = "forecast_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForecastData {

    /**
     * Composite primary key: productName#month#region
     * Example: "Bisleri-1L#December#extreme_north"
     */
    @Id
    @Column(name = "product_forecast_key")
    private String productForecastKey;

    @Column(name = "month", nullable = false)
    private String month;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "product_name", nullable = false)
    private String productName;

    /**
     * Base need value for XS size store.
     * Other store sizes will multiply this value by their factor.
     */
    @Column(name = "base_need_xs", nullable = false)
    private Integer baseNeedXs;

    /**
     * Helper method to generate the composite key
     */
    public static String generateKey(String productName, String month, String region) {
        return String.format("%s#%s#%s", productName, month, region);
    }

    /**
     * Pre-persist method to generate the key before saving
     */
    @PrePersist
    @PreUpdate
    public void generateKey() {
        if (this.productForecastKey == null) {
            this.productForecastKey = generateKey(this.productName, this.month, this.region);
        }
    }
}