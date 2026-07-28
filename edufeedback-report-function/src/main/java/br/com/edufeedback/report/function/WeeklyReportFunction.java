package br.com.edufeedback.report.function;

import br.com.edufeedback.report.service.WeeklyReportService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import jakarta.inject.Inject;

public class WeeklyReportFunction {
  @Inject WeeklyReportService service;

  @FunctionName("weeklyFeedbackReport")
  public void run(
      @TimerTrigger(name = "timer", schedule = "0 0 11 * * MON") String timerInfo,
      ExecutionContext context) {
    var referenceDate = service.hoje();
    context
        .getLogger()
        .info("event=timer.trigger.started referenceDate=" + referenceDate + " timer=" + timerInfo);
    try {
      var result = service.gerar(referenceDate);
      context
          .getLogger()
          .info(
              "event=timer.trigger.completed reportId="
                  + result.reportId()
                  + " status="
                  + result.status());
    } catch (RuntimeException exception) {
      context
          .getLogger()
          .severe(
              "event=timer.trigger.failed referenceDate="
                  + referenceDate
                  + " error="
                  + exception.getClass().getSimpleName());
      throw exception;
    }
  }
}
