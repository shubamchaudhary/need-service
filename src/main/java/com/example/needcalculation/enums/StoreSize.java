package com.example.needcalculation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing different store sizes and their multiplication factors.
 * The multiplication factor is used to calculate the actual need based on store size.
 */
@Getter
@AllArgsConstructor
public enum StoreSize {
    XS("xs", 1.0),
    S("s", 1.5),
    M("m", 2.0),
    L("l", 3.0),
    XL("xl", 5.0);

    private final String code;
    private final Double multiplicationFactor;

    /**
     * Get StoreSize enum from string code
     * @param code Store size code (xs, s, m, l, xl)
     * @return StoreSize enum
     */
    public static StoreSize fromCode(String code) {
        for (StoreSize size : StoreSize.values()) {
            if (size.code.equalsIgnoreCase(code)) {
                return size;
            }
        }
        throw new IllegalArgumentException("Invalid store size: " + code);
    }
}