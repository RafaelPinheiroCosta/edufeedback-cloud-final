package br.com.edufeedback.messaging;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.QueueMessageEncoding;
import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QueueClientFactory {
  @Inject QueueConfig config;

  public QueueClient create() {
    return create(config.name());
  }

  public QueueClient create(String queueName) {
    if (queueName == null || queueName.isBlank()) {
      throw new IllegalArgumentException("O nome da fila é obrigatório.");
    }

    QueueClientBuilder builder =
        new QueueClientBuilder()
            .queueName(queueName)
            // O Azure Functions Queue Trigger usa Base64 por padrão. Sem esta
            // configuração, o SDK v12 envia o JSON como texto puro e o host pode
            // falhar antes de entregar a mensagem ao método Java.
            .messageEncoding(QueueMessageEncoding.BASE64);

    if (config.connectionString().isPresent()) {
      builder.connectionString(config.connectionString().get());
    } else {
      builder
          .endpoint(
              config
                  .endpoint()
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Configure app.queue.connection-string ou app.queue.endpoint.")))
          .credential(new DefaultAzureCredentialBuilder().build());
    }

    QueueClient client = builder.buildClient();
    client.createIfNotExists();
    return client;
  }

  public String queueName() {
    return config.name();
  }

  @ConfigMapping(prefix = "app.queue")
  public interface QueueConfig {
    String name();

    java.util.Optional<String> connectionString();

    java.util.Optional<String> endpoint();
  }
}
