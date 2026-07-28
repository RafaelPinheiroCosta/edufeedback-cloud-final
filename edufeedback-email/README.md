# edufeedback-email

Integração compartilhada com Azure Communication Services Email.

## Responsabilidades

- enviar e-mails de alertas e relatórios;
- encapsular o cliente da Azure;
- registrar início, sucesso e falha sem expor dados sensíveis.

## Configuração

```text
AZURE_COMMUNICATION_CONNECTION_STRING
EMAIL_SENDER
ADMIN_EMAIL
```

## Build

```bash
mvn -pl edufeedback-email -am clean verify
```
