
package org.hermes.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseHTTP<T>(
        T data,
        String message,
        boolean success,
        String traceId,
        OffsetDateTime timestamp,
        String code,
        Map<String, List<String>> errors,
        PageMeta pagination
) {

    public ResponseHTTP {
        timestamp = timestamp != null
                ? timestamp
                : OffsetDateTime.now(ZoneOffset.UTC);

        traceId = traceId != null && !traceId.isBlank()
                ? traceId
                : resolveCurrentTraceId();

        if (errors != null && !errors.isEmpty()) {
            Map<String, List<String>> normalized = new LinkedHashMap<>();

            errors.forEach((field, messages) -> {
                if (field != null && messages != null) {
                    List<String> validMessages = messages.stream()
                            .filter(Objects::nonNull)
                            .toList();

                    if (!validMessages.isEmpty()) {
                        normalized.put(field, validMessages);
                    }
                }
            });

            errors = normalized.isEmpty()
                    ? null
                    : Collections.unmodifiableMap(normalized);
        } else {
            errors = null;
        }

        if (success && errors != null) {
            throw new IllegalArgumentException(
                    "Successful responses cannot contain errors"
            );
        }
    }

    // =========================
    // SUCCESS RESPONSES
    // =========================

    public static <T> ResponseHTTP<T> ok(T data) {
        return new ResponseHTTP<>(
                data, "Success", true,
                null, null, null, null, null
        );
    }

    public static <T> ResponseHTTP<T> ok(
            T data,
            String traceId
    ) {
        return new ResponseHTTP<>(
                data, "Success", true,
                traceId, null, null, null, null
        );
    }

    public static <T> ResponseHTTP<T> ok(
            T data,
            String traceId,
            String message
    ) {
        return new ResponseHTTP<>(
                data, message, true,
                traceId, null, null, null, null
        );
    }

    public static <T> ResponseHTTP<T> okWithMessage(
            T data,
            String message
    ) {
        return new ResponseHTTP<>(
                data, message, true,
                null, null, null, null, null
        );
    }

    public static ResponseHTTP<Void> ok() {
        return new ResponseHTTP<>(
                null, "Success", true,
                null, null, null, null, null
        );
    }

    public static ResponseHTTP<Void> okTraceId(String traceId) {
        return new ResponseHTTP<>(
                null, "Success", true,
                traceId, null, null, null, null
        );
    }

    public static <T> ResponseHTTP<T> paginated(
            T data,
            PageMeta pagination
    ) {
        return new ResponseHTTP<>(
                data, "Success", true,
                null, null, null, null, pagination
        );
    }

    // =========================
    // ERROR RESPONSES
    // =========================

    public static <T> ResponseHTTP<T> error(String message) {
        return failure("REQUEST_FAILED", message);
    }

    public static <T> ResponseHTTP<T> error(
            String message,
            String customTraceId
    ) {
        return failure(
                "REQUEST_FAILED",
                message,
                customTraceId,
                null
        );
    }

    public static <T> ResponseHTTP<T> failure(
            String code,
            String message
    ) {
        return failure(code, message, null, null);
    }

    public static <T> ResponseHTTP<T> failure(
            String code,
            String message,
            Map<String, List<String>> errors
    ) {
        return failure(code, message, null, errors);
    }

    public static <T> ResponseHTTP<T> failure(
            String code,
            String message,
            String traceId,
            Map<String, List<String>> errors
    ) {
        return new ResponseHTTP<>(
                null,
                message,
                false,
                traceId,
                null,
                code,
                errors,
                null
        );
    }

    public static <T> ResponseHTTP<T> validationError(
            Map<String, List<String>> errors
    ) {
        return failure(
                "VALIDATION_ERROR",
                "One or more fields are invalid",
                errors
        );
    }

    // =========================
    // UTILITY METHODS
    // =========================

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasData() {
        return data != null;
    }

    private static String resolveCurrentTraceId() {
        SpanContext context = Span.current().getSpanContext();

        return context.isValid()
                ? context.getTraceId()
                : null;
    }

    // =========================
    // PAGINATION METADATA
    // =========================

    public record PageMeta(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {

        public PageMeta {
            if (page < 0) {
                throw new IllegalArgumentException(
                        "Page cannot be negative"
                );
            }

            if (size <= 0) {
                throw new IllegalArgumentException(
                        "Size must be greater than zero"
                );
            }

            if (totalElements < 0) {
                throw new IllegalArgumentException(
                        "Total elements cannot be negative"
                );
            }
        }

        public static PageMeta of(
                int page,
                int size,
                long totalElements
        ) {
            int totalPages = Math.toIntExact(
                    totalElements / size
                            + (totalElements % size == 0 ? 0 : 1)
            );

            return new PageMeta(
                    page,
                    size,
                    totalElements,
                    totalPages,
                    page < totalPages - 1,
                    page > 0
            );
        }
    }
}
