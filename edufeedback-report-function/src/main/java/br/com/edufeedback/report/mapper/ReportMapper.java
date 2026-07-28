package br.com.edufeedback.report.mapper;

import br.com.edufeedback.report.dto.ReportResponse;
import br.com.edufeedback.report.service.WeeklyReportService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReportMapper {
  public ReportResponse toResponse(WeeklyReportService.Result result) {
    return new ReportResponse(result.status(), result.reportId(), result.inicio(), result.fim());
  }
}
