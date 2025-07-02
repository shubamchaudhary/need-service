# Need Calculation Service

A Spring Boot service for calculating product needs based on forecast data and store sizes.

## Overview

This service calculates the product needs for different stores based on:
- Historical forecast data (loaded from CSV)
- Store size (XS, S, M, L, XL)
- Geographic regions in India
- Month-specific demand patterns

## Key Concepts

### 1. **SKU (Stock Keeping Unit)**
- Format: `ProductName@StoreName`
- Example: `Bisleri-1L@str1`
- Represents a unique product-store combination

### 2. **Store Size Multipliers**
- XS: 1.0x (base)
- S: 1.5x
- M: 2.0x
- L: 3.0x
- XL: 5.0x

### 3. **Forecast Key**
- Format: `ProductName#Month#Region`
- Example: `Bisleri-1L#December#extreme_north`
- Primary key for forecast data

### 4. **Regions**
- extreme_north (Kashmir, Uttarakhand, Punjab, etc.)
- rajasthan
- north_central (UP, Bihar, MP, Bengal, Orissa, Jharkhand)
- northeast (Northeast states)
- west (Gujarat, Maharashtra)
- south (Southernmost states)
- central (Central states)
- southeast_coastal (Southeast coastal states)

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Gradle 8.10**
- **PostgreSQL** (Ready for future integration)
- **OpenCSV** (For CSV parsing)
- **Lombok** (Reducing boilerplate code)

