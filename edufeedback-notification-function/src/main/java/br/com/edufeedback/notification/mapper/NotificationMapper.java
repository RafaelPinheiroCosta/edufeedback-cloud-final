package br.com.edufeedback.notification.mapper;

import br.com.edufeedback.notification.dto.NotificationResponse;
import br.com.edufeedback.notification.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationMapper {
  public NotificationResponse toResponse(NotificationService.ProcessingResult result) {
    return new NotificationResponse(result.status(), result.eventId());
  }
}
