package br.com.edufeedback.report.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReportResponse(
    String status, UUID reportId, LocalDate startDate, LocalDate endDate) {}
