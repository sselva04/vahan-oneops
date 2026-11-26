package com.vahan.oneops.project;

import java.util.Locale;

/** Vahan's Excel files have stray spaces and mixed case in drawing numbers; store one clean form. */
final class DrawingNumbers {

    private DrawingNumbers() {
    }

    static String normalize(String raw) {
        return raw.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
