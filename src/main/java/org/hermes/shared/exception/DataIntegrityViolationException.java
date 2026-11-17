package org.hermes.shared.exception;

import java.io.Serial;

public final class DataIntegrityViolationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String sqlState;
    private final String constraintName;

    public DataIntegrityViolationException(String message) {
        this(message, null, null, null);
    }

    public DataIntegrityViolationException(
            String message,
            Throwable cause
    ) {
        this(message, null, null, cause);
    }

    public DataIntegrityViolationException(
            String message,
            String sqlState,
            String constraintName,
            Throwable cause
    ) {
        super(message, cause);
        this.sqlState = sqlState;
        this.constraintName = constraintName;
    }

    public String getSqlState() {
        return sqlState;
    }

    public String getConstraintName() {
        return constraintName;
    }
}