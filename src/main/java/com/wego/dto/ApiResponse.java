package com.wego.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Unified API Response wrapper.
 *
 * All API endpoints return this format for consistency.
 *
 * @param <T> The type of data payload
 *
 * @contract
 *   - invariant: timestamp is never null
 *   - invariant: success is always set
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    /**
     * Creates a successful response with data.
     *
     * @contract
     *   - pre: data != null
     *   - post: success == true, data is set
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    /**
     * Creates a successful response with data and message.
     *
     * @contract
     *   - pre: data != null
     *   - post: success == true, data and message are set
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .message(message)
            .build();
    }

    /**
     * Creates an error response.
     *
     * @contract
     *   - pre: errorCode != null
     *   - post: success == false, errorCode and message are set
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .errorCode(errorCode)
            .message(message)
            .build();
    }
}
