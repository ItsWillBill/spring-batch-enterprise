package com.springbatch.project.domain.error;

import com.springbatch.project.domain.dto.TransactionCsvDTO;

public record TransactionError(TransactionCsvDTO source, String reason) {

}
