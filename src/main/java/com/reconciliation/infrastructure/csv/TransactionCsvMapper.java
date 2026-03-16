package com.reconciliation.infrastructure.csv;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.reconciliation.domain.model.Transaction;
import com.reconciliation.shared.exception.CsvParsingException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TransactionCsvMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    // CSV column indices - named constants for readability
    private static final int COL_EXTERNAL_ID = 0;
    private static final int COL_SOURCE = 1;
    private static final int COL_AMOUNT = 2;
    private static final int COL_CURRENCY = 3;
    private static final int COL_TRANSACTION_DATE = 4;
    private static final int COL_DESCRIPTION = 5;
    private static final int EXPECTED_COLUMNS = 6;

    public Transaction map(String[] fields, int lineNumber) {
        validateColumnCount(fields, lineNumber);

        String extarnalId = extractString(fields, COL_EXTERNAL_ID, "external_id", lineNumber);
        String source = extractString(fields, COL_SOURCE, "source", lineNumber);
        BigDecimal amount = extractAmount(fields, lineNumber);
        String currency = extractString(fields, COL_CURRENCY, "currency", lineNumber);
        LocalDate date = extractDate(fields, lineNumber);
        String description = fields[COL_DESCRIPTION].trim();

        try {
            return new Transaction(extarnalId, source, amount, currency, date, description);
        } catch (IllegalArgumentException e) {
            throw new CsvParsingException("Domain validation failde " + e.getMessage(), lineNumber,
                    String.join(",", fields), e);
        }
    }

    private void validateColumnCount(String[] fields, int lineNumber) {
        if (fields == null || fields.length < EXPECTED_COLUMNS) {
            throw new CsvParsingException(
                    String.format("Expected %d columns but got %d", EXPECTED_COLUMNS,
                            fields == null ? 0 : fields.length),
                    lineNumber, fields == null ? "" : String.join(",", fields));
        }
    }

    private String extractString(String[] fields, int colIndex, String fieldName, int lineNumber) {
        String value = fields[colIndex].trim();
        if (value.isBlank()) {
            throw new CsvParsingException("Required field '" + fieldName + "' is blank", lineNumber,
                    String.join(",", fields));
        }
        return value;
    }

    private BigDecimal extractAmount(String[] fields, int lineNumber) {
        String rawAmount = fields[COL_AMOUNT].trim();
        try {
            return new BigDecimal(rawAmount);
        } catch (NumberFormatException e) {
            throw new CsvParsingException("Invalid amount value: '" + rawAmount + "'", lineNumber,
                    String.join(",", fields), e);
        }
    }

    private LocalDate extractDate(String[] fields, int lineNumber) {
        String rawDate = fields[COL_TRANSACTION_DATE].trim();
        try {
            return LocalDate.parse(rawDate, DATE_FORMATTER);
        } catch (DateTimeException e) {
            throw new CsvParsingException("Invalid date format: '" + rawDate + "' (expected yyyy-MM-dd)", lineNumber,
                    String.join(",", fields), e);
        }
    }
}
