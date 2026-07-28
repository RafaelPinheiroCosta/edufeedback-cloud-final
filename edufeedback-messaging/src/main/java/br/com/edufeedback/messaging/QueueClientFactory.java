package br.com.edufeedback.messaging;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.*;
import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QueueClientFactory {
  @Inject QueueConfig config;

  public QueueClient create() {
    QueueClientBuilder builder = new QueueClientBuilder().queueName(config.name());
    if (config.connectionString().isPresent()) {
      builder.connectionString(config.connectionString().get());
    } else {
      builder
          .endpoint(config.endpoint().orElseThrow())
          .credential(new DefaultAzureCredentialBuilder().build());
    }
    QueueClient client = builder.buildClient();
    client.createIfNotExists();
    return client;
  }

  @ConfigMapping(prefix = "app.queue")
  public interface QueueConfig {
    String name();

    java.util.Optional<String> connectionString();

    java.util.Optional<String> endpoint();
  }
}
