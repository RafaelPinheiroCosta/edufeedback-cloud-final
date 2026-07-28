package br.com.edufeedback.email;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendStatus;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AzureCommunicationEmailSender implements EmailSender {
  private static final Logger LOG = Logger.getLogger(AzureCommunicationEmailSender.class);

  @ConfigProperty(name = "app.email.connection-string", defaultValue = "")
  String connectionString;

  @ConfigProperty(name = "app.email.sender", defaultValue = "")
  String senderAddress;

  private volatile EmailClient client;

  @Override
  public SendResult sendHtml(String to, String subject, String html) {
    validateConfiguration();
    long startedAt = System.nanoTime();
    LOG.infof("event=email.send.started recipient=%s subject=%s", mask(to), subject);
    try {
      var message =
          new EmailMessage()
              .setSenderAddress(senderAddress)
              .setToRecipients(to)
              .setSubject(subject)
              .setBodyHtml(html);

      var result = client().beginSend(message).getFinalResult();
      if (!EmailSendStatus.SUCCEEDED.equals(result.getStatus())) {
        String detail =
            result.getError() == null
                ? "Falha sem detalhe retornado pela Azure."
                : result.getError().getMessage();
        throw new EmailDeliveryException(
            "Falha ao enviar e-mail pelo Azure Communication Services: " + detail);
      }
      LOG.infof(
          "event=email.send.succeeded operationId=%s recipient=%s durationMs=%d",
          result.getId(), mask(to), elapsedMillis(startedAt));
      return new SendResult(result.getId(), result.getStatus().toString());
    } catch (EmailDeliveryException e) {
      LOG.errorf(
          e,
          "event=email.send.failed recipient=%s durationMs=%d",
          mask(to),
          elapsedMillis(startedAt));
      throw e;
    } catch (RuntimeException e) {
      LOG.errorf(
          e,
          "event=email.send.failed recipient=%s durationMs=%d",
          mask(to),
          elapsedMillis(startedAt));
      throw new EmailDeliveryException(
          "Não foi possível concluir o envio pelo Azure Communication Services.", e);
    }
  }

  private EmailClient client() {
    var current = client;
    if (current == null) {
      synchronized (this) {
        current = client;
        if (current == null) {
          current = new EmailClientBuilder().connectionString(connectionString).buildClient();
          client = current;
        }
      }
    }
    return current;
  }

  private long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }

  private String mask(String email) {
    if (email == null || !email.contains("@")) {
      return "***";
    }
    return "***@" + email.substring(email.indexOf('@') + 1);
  }

  private void validateConfiguration() {
    if (connectionString == null || connectionString.isBlank()) {
      throw new EmailDeliveryException(
          "AZURE_COMMUNICATION_CONNECTION_STRING não foi configurada.");
    }
    if (senderAddress == null || senderAddress.isBlank()) {
      throw new EmailDeliveryException("EMAIL_SENDER não foi configurado.");
    }
  }
}
