# edufeedback-report-function

Azure Function responsável pelo relatório semanal.

## Responsabilidades

- executar a geração periódica pelo Timer Trigger;
- consultar avaliações do período;
- calcular indicadores;
- persistir o relatório;
- enviar o conteúdo por Azure Communication Services Email;
- expor endpoint administrativo protegido por JWT `ADMIN` para demonstração imediata;
- registrar logs correlacionados e falhas.

O endpoint administrativo reutiliza o mesmo serviço do Timer Trigger.

## Configuração

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_COMMUNICATION_CONNECTION_STRING
EMAIL_SENDER
ADMIN_EMAIL
APP_TIMEZONE
```

## Build

```bash
mvn -pl edufeedback-report-function -am clean verify
```
