# edufeedback-notification-function

Azure Function responsável por consumir eventos de feedback crítico e enviar alertas por e-mail.

## Responsabilidades

- Consumir a fila `feedback-critical-notifications`.
- Desserializar e validar `FeedbackCriticoEvent`.
- Garantir idempotência por `eventId` e consumidor.
- Controlar tentativas e estado da notificação.
- Enviar o alerta pelo Azure Communication Services Email.
- Registrar falhas sem mascarar a exceção original.
- Expor diagnósticos protegidos por Function Key.
- Disponibilizar recursos administrativos Quarkus protegidos por JWT.

## Queue Trigger

| Function | Trigger | Origem |
|---|---|---|
| `feedbackCriticalNotification` | Azure Storage Queue | `%QUEUE_NAME%` |

Fluxo:

1. o host recebe e decodifica a mensagem Base64;
2. a Function registra `messageId` e `dequeueCount`;
3. o evento é processado pelo `NotificationService`;
4. o banco impede processamento duplicado;
5. o e-mail é enviado;
6. notificação e evento são confirmados como processados.

Uma exceção é relançada para permitir a política de retry da Azure Functions. Mensagens que excedem o limite de tentativas podem seguir para a poison queue.

## Idempotência

A tabela `evento_processado` possui unicidade por `event_id` e `consumer`. A tabela `notificacao` também restringe a combinação de evento e tipo. Uma reentrega não cria um segundo envio quando o evento já foi concluído.

## Diagnósticos nativos

Os endpoints abaixo usam `AuthorizationLevel.FUNCTION` e exigem `x-functions-key` ou `?code=`:

| Function | Método e caminho | Finalidade |
|---|---|---|
| `notificationDiagnosticHealth` | `GET /api/diagnostics/notifications/health` | Estado das configurações |
| `sendNotificationEmailDiagnostic` | `POST /api/diagnostics/notifications/email` | Envio direto controlado |
| `enqueueCriticalFeedbackDiagnostic` | `POST /api/diagnostics/notifications/critical` | Publicação de evento de teste |
| `getNotificationDiagnosticStatus` | `GET /api/diagnostics/notifications/status/{eventId}` | Consulta de processamento |
| `getNotificationQueueDiagnostic` | `GET /api/diagnostics/notifications/queues` | Quantidade nas filas ativa e poison |

## Recursos administrativos Quarkus

| Método | Caminho | Acesso |
|---|---|---|
| `POST` | `/api/v1/admin/notifications/process-one` | JWT `ADMIN` |
| `POST` | `/api/v1/admin/notifications/test` | JWT `ADMIN` |

O fluxo principal em produção é o Queue Trigger. Os recursos HTTP administrativos existem para operação controlada.

## Configuração

```text
AZURE_NOTIFICATION_FUNCTION_APP
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_STORAGE_CONNECTION_STRING
QUEUE_NAME
AZURE_COMMUNICATION_CONNECTION_STRING
EMAIL_SENDER
ADMIN_EMAIL
```

A porta Quarkus de desenvolvimento é `8082`. As migrations não são executadas neste módulo.

## Build

```bash
./mvnw -pl edufeedback-notification-function -am verify
```

No Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-notification-function -am verify
```

## Empacotamento e execução com Core Tools

```bash
./mvnw -pl edufeedback-notification-function -am package
func start --script-root edufeedback-notification-function/target/azure-functions/<AZURE_NOTIFICATION_FUNCTION_APP> --port 7072
```

Para executar localmente, configure PostgreSQL, Storage/Azurite e as variáveis do serviço de e-mail no processo do Azure Functions Core Tools.

## Dependências internas

```text
edufeedback-api-common
edufeedback-domain
edufeedback-persistence
edufeedback-email
edufeedback-messaging
```
