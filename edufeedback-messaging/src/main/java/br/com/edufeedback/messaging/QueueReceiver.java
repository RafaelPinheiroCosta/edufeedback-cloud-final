package br.com.edufeedback.messaging;

import br.com.edufeedback.api.exception.ExternalServiceException;
import com.azure.storage.queue.models.QueueMessageItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QueueReceiver {
  private static final Logger LOG = Logger.getLogger(QueueReceiver.class);

  @Inject QueueClientFactory factory;
  @Inject ObjectMapper mapper;

  public Optional<ReceivedEvent> receiveOne() {
    var client = factory.create();
    QueueMessageItem item =
        client.receiveMessages(1, Duration.ofSeconds(30), null, null).stream()
            .findFirst()
            .orElse(null);
    if (item == null) {
      LOG.info("event=queue.receive.empty");
      return Optional.empty();
    }
    try {
      var event = mapper.readValue(item.getBody().toString(), FeedbackCriticoEvent.class);
      LOG.infof(
          "event=queue.receive.succeeded eventId=%s feedbackId=%s correlationId=%s",
          event.eventId(), event.payload().feedbackId(), event.correlationId());
      return Optional.of(
          new ReceivedEvent(
              event, () -> client.deleteMessage(item.getMessageId(), item.getPopReceipt())));
    } catch (Exception exception) {
      LOG.errorf(exception, "event=queue.receive.invalid messageId=%s", item.getMessageId());
      throw new ExternalServiceException(
          "QUEUE_MESSAGE_INVALID", "Mensagem inválida na fila.", exception);
    }
  }

  public record ReceivedEvent(FeedbackCriticoEvent event, Runnable ack) {}
}
