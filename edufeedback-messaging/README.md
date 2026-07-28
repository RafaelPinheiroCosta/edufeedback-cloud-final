# edufeedback-messaging

Integração compartilhada com Azure Storage Queue.

## Responsabilidades

- serializar eventos de feedback crítico;
- publicar mensagens na fila configurada;
- preservar identificadores de evento e correlação;
- registrar logs de publicação e falhas.

## Configuração

```text
AZURE_STORAGE_CONNECTION_STRING
QUEUE_NAME
```

## Build

```bash
mvn -pl edufeedback-messaging -am clean verify
```
