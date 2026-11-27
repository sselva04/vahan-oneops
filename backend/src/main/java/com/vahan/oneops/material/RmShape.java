package com.vahan.oneops.material;

/** How a piece of raw material is shaped, which decides how its weight is calculated. */
public enum RmShape {
    BLOCK,    // length x width x thickness
    ROUND,    // diameter x length
    TUBE,     // outer diameter, inner diameter, length
    MANUAL    // weight per piece typed in, no formula
}
