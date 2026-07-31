# edufeedback-messaging

Adapter de mensageria para publicação e leitura de eventos em Azure Storage Queue.

## Responsabilidades

- Definir o contrato `FeedbackCriticoEvent`.
- Serializar e desserializar eventos com Jackson.
- Publicar mensagens na fila configurada.
- Criar o cliente por connection string ou Managed Identity.
- Usar codificação Base64 compatível com o Queue Trigger da Azure Functions.
- Preservar `eventId`, `correlationId` e metadados do evento.
- Registrar sucesso e falha da publicação sem expor segredos.

## Contrato do evento

```json
{
  "eventId": "02523e2c-2cda-42bc-94ad-71c61d3e667a",
  "eventType": "FEEDBACK_CRITICAL_CREATED",
  "correlationId": "93320f68-6a97-4c1f-8ae8-c2715e61a582",
  "causationId": null,
  "occurredAt": "2026-07-30T23:14:44Z",
  "payload": {
    "feedbackId": "3efa7763-f24e-4f01-aaa1-059188d83893",
    "nota": 4,
    "urgencia": "CRITICA"
  }
}
```

## Componentes

| Classe | Finalidade |
|---|---|
| `QueueClientFactory` | Configura e cria o cliente da fila |
| `QueuePublisher` | Serializa e publica eventos |
| `QueueReceiver` | Recebe mensagens em fluxos administrativos |
| `FeedbackCriticoEvent` | Contrato imutável do evento |

## Configuração

```text
app.queue.name
app.queue.connection-string
app.queue.endpoint
```

Nas Functions, os valores são mapeados por:

```text
QUEUE_NAME
AZURE_STORAGE_CONNECTION_STRING
```

Quando a connection string não é informada, o cliente exige `app.queue.endpoint` e usa `DefaultAzureCredential`.

## Build

```bash
./mvnw -pl edufeedback-messaging -am test
```

No Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-messaging -am test
```

## Compatibilidade operacional

O produtor e o Queue Trigger devem usar a mesma codificação. O módulo publica com `QueueMessageEncoding.BASE64`, correspondente ao comportamento configurado no host da Azure Functions.
