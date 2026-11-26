package com.vahan.oneops.common;

import java.util.Map;

/** Every error response has this shape, so the frontend handles errors in one place. */
public record ApiError(String message, Map<String, String> fields) {
    public static ApiError of(String message) {
        return new ApiError(message, Map.of());
    }
}
