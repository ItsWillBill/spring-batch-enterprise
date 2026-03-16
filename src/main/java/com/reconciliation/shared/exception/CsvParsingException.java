package com.reconciliation.shared.exception;

import lombok.Getter;

@Getter
public class CsvParsingException extends ReconciliationException {

    private final int lineNumber;
    private final String rawline;

    public CsvParsingException(String message, int lineNumber, String rawline) {
        super(message);
        this.lineNumber = lineNumber;
        this.rawline = rawline;
    }

    public CsvParsingException(String message, int lineNumber, String rawline, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.rawline = rawline;
    }

    @Override
    public String toString() {
        return String.format("CsvParsingException[line=%d, raw='%s', message='%s']", lineNumber, rawline, getMessage());
    }

}
