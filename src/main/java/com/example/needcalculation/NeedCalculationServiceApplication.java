package com.example.needcalculation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application class.
 * @SpringBootApplication annotation combines:
 * - @Configuration: Marks this as a configuration class
 * - @EnableAutoConfiguration: Enables Spring Boot's auto-configuration
 * - @ComponentScan: Enables component scanning in this package and sub-packages
 */
@SpringBootApplication
public class NeedCalculationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeedCalculationServiceApplication.class, args);
    }
}