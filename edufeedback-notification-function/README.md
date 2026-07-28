# edufeedback-notification-function

Azure Function responsável pelas notificações de feedback crítico.

## Responsabilidades

- consumir mensagens pela Queue Trigger;
- garantir idempotência por evento processado;
- recuperar os dados da avaliação;
- persistir o estado da notificação;
- enviar e-mail pelo Azure Communication Services;
- expor endpoint administrativo de teste protegido por JWT `ADMIN`;
- produzir logs correlacionados para o Application Insights.

O Queue Trigger é o fluxo produtivo e não depende de JWT. O token é exigido apenas no endpoint HTTP administrativo.

## Configuração

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_STORAGE_CONNECTION_STRING
QUEUE_NAME
AZURE_COMMUNICATION_CONNECTION_STRING
EMAIL_SENDER
ADMIN_EMAIL
```

## Build

```bash
mvn -pl edufeedback-notification-function -am clean verify
```
