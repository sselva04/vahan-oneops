package com.vahan.oneops.material;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checked against Vahan's purchase order PO-2627-003 (steel, 7.85 g/cm3). */
class WeightCalculatorTest {

    private static final BigDecimal STEEL = new BigDecimal("7.85");

    private static BigDecimal n(String s) {
        return new BigDecimal(s);
    }

    private static void assertKg(String expected, BigDecimal actual) {
        assertEquals(0, n(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void blocksMatchThePurchaseOrder() {
        assertKg("2.010", WeightCalculator.perPieceKg(RmShape.BLOCK, STEEL, n("128"), n("100"), n("20"), null)); // PO 2.01
        assertKg("0.264", WeightCalculator.perPieceKg(RmShape.BLOCK, STEEL, n("42"), n("40"), n("20"), null));   // PO 0.26
        assertKg("0.261", WeightCalculator.perPieceKg(RmShape.BLOCK, STEEL, n("52"), n("32"), n("20"), null));   // PO 0.26
        assertKg("0.786", WeightCalculator.perPieceKg(RmShape.BLOCK, STEEL, n("77"), n("65"), n("20"), null));   // PO 0.79
    }

    @Test
    void roundsMatchThePurchaseOrder() {
        assertKg("0.240", WeightCalculator.perPieceKg(RmShape.ROUND, STEEL, n("32"), n("38"), null, null));     // PO 0.24
        assertKg("0.146", WeightCalculator.perPieceKg(RmShape.ROUND, STEEL, n("25"), n("38"), null, null));     // PO 0.15
        assertKg("0.254", WeightCalculator.perPieceKg(RmShape.ROUND, STEEL, n("22"), n("85"), null, null));     // PO 0.25
    }

    @Test
    void totalIsPerPieceTimesQuantity() {
        BigDecimal hook = WeightCalculator.perPieceKg(RmShape.ROUND, STEEL, n("40"), n("30"), null, null);
        assertKg("0.296", hook);
        assertKg("1.184", WeightCalculator.totalKg(hook, 4));   // PO shows 1.19 (see the note in the PR)
    }

    @Test
    void tubeSubtractsTheBore() {
        assertKg("0.555", WeightCalculator.perPieceKg(RmShape.TUBE, STEEL, n("50"), n("40"), n("100"), null));
    }

    @Test
    void densityDecidesTheWeight() {
        assertKg("0.270", WeightCalculator.perPieceKg(RmShape.BLOCK, n("2.7"), n("100"), n("100"), n("10"), null));
    }

    @Test
    void manualWeightIsUsedAsTyped() {
        assertKg("1.200", WeightCalculator.perPieceKg(RmShape.MANUAL, STEEL, null, null, null, n("1.2")));
    }
}
