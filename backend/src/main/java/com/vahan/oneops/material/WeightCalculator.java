package com.vahan.oneops.material;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Theoretical weight of raw material from its size and density. Sizes are in mm, density in g/cm3.
 * Volume in mm3 x density (g/cm3) / 1,000,000 gives kg. Example from PO-2627-003: OHNS 128 x 100 x 20
 * is 256,000 mm3 x 7.85 / 1,000,000 = 2.01 kg.
 */
public final class WeightCalculator {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal QUARTER = new BigDecimal("0.25");
    // Plenty of digits for the circle area; the final answer is rounded to grams.
    private static final BigDecimal PI = new BigDecimal("3.14159265358979323846");
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private WeightCalculator() {
    }

    /** Weight in kg of ONE piece, rounded to grams. a, b, c are the sizes in the order given for the shape. */
    public static BigDecimal perPieceKg(RmShape shape, BigDecimal density,
                                        BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal manualKg) {
        if (shape == RmShape.MANUAL) {
            return manualKg.setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal volumeMm3 = switch (shape) {
            case BLOCK -> a.multiply(b, MC).multiply(c, MC);
            case ROUND -> circle(a).multiply(b, MC);
            case TUBE -> circle(a).subtract(circle(b), MC).multiply(c, MC);
            default -> throw new IllegalArgumentException("Unknown shape");
        };
        return volumeMm3.multiply(density, MC).divide(MILLION, MC).setScale(3, RoundingMode.HALF_UP);
    }

    /** Weight in kg of the whole part quantity. */
    public static BigDecimal totalKg(BigDecimal perPieceKg, int quantity) {
        return perPieceKg.multiply(BigDecimal.valueOf(quantity)).setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal circle(BigDecimal diameter) {
        return PI.multiply(QUARTER, MC).multiply(diameter, MC).multiply(diameter, MC);
    }
}
