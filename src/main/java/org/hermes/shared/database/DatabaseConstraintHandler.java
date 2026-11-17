package org.hermes.shared.database;

import org.hermes.shared.exception.DataIntegrityViolationException;
import org.hermes.shared.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public final class DatabaseConstraintHandler {

    private static final Logger log =
            LoggerFactory.getLogger(DatabaseConstraintHandler.class);

    private DatabaseConstraintHandler() {
    }

    public static <T> Result<T> handle(
            DataIntegrityViolationException exception
    ) {

        String sqlState = resolveSqlState(exception);
        String constraint = exception.getConstraintName();

        if (sqlState == null) {
            log.error(
                    "Database error without SQLSTATE. Constraint={}",
                    constraint,
                    exception
            );

            return Result.failure(
                    "Unable to complete database operation",
                    500
            );
        }

        return switch (sqlState) {

            case "23502" -> Result.failure(
                    "A required field is missing",
                    422
            );

            case "23503" -> {
                log.warn(
                        "Foreign key violation. Constraint={}",
                        constraint
                );

                yield Result.failure(
                        "Referenced resource does not exist or is currently in use",
                        409
                );
            }

            case "23505" -> Result.failure(
                    "A record with this unique value already exists",
                    409
            );

            case "23514" -> Result.failure(
                    "Operation violated database validation rules",
                    422
            );

            case "23P01" -> Result.failure(
                    "Operation conflicts with existing data",
                    409
            );

            case "22001" -> Result.failure(
                    "A field exceeded the allowed size",
                    422
            );

            case "22003" -> Result.failure(
                    "Numeric value is out of allowed range",
                    422
            );

            case "22P02" -> Result.failure(
                    "Invalid data format",
                    400
            );

            default -> handleUnknown(exception, sqlState, constraint);
        };
    }

    private static <T> Result<T> handleUnknown(
            DataIntegrityViolationException exception,
            String sqlState,
            String constraint
    ) {

        if (sqlState.startsWith("23")) {
            log.warn(
                    "Unmapped database constraint violation. SQLSTATE={}, Constraint={}",
                    sqlState,
                    constraint
            );

            return Result.failure(
                    "Operation conflicts with existing data",
                    409
            );
        }

        if (sqlState.startsWith("22")) {
            return Result.failure(
                    "Invalid data supplied for database operation",
                    422
            );
        }

        log.error(
                "Unexpected database error. SQLSTATE={}, Constraint={}",
                sqlState,
                constraint,
                exception
        );

        return Result.failure(
                "Unable to complete database operation",
                500
        );
    }

    private static String resolveSqlState(Throwable throwable) {

        if (throwable instanceof DataIntegrityViolationException exception
                && exception.getSqlState() != null) {
            return exception.getSqlState();
        }

        Throwable current = throwable;

        while (current != null) {

            if (current instanceof SQLException sqlException) {

                SQLException candidate = sqlException;

                while (candidate != null) {
                    if (candidate.getSQLState() != null) {
                        return candidate.getSQLState();
                    }

                    candidate = candidate.getNextException();
                }
            }

            current = current.getCause();
        }

        return null;
    }
}