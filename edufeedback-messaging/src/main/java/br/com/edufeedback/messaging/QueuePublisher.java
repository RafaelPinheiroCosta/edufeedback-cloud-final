package br.com.edufeedback.messaging;

import br.com.edufeedback.api.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QueuePublisher {
  private static final Logger LOG = Logger.getLogger(QueuePublisher.class);

  @Inject QueueClientFactory factory;
  @Inject ObjectMapper mapper;

  public void publish(FeedbackCriticoEvent event) {
    try {
      factory.create().sendMessage(mapper.writeValueAsString(event));
      LOG.infof(
          "event=queue.publish.succeeded eventId=%s feedbackId=%s correlationId=%s",
          event.eventId(), event.payload().feedbackId(), event.correlationId());
    } catch (Exception exception) {
      LOG.errorf(
          exception,
          "event=queue.publish.failed eventId=%s feedbackId=%s correlationId=%s",
          event.eventId(),
          event.payload().feedbackId(),
          event.correlationId());
      throw new ExternalServiceException(
          "QUEUE_PUBLISH_FAILED", "Falha ao publicar evento na fila.", exception);
    }
  }
}
