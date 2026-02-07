package com.springbatch.project.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TransactionCsvDTO {

    private String reference;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime transactionDate;
}
