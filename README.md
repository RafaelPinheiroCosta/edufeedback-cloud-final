# EduFeedback Cloud

Backend serverless para registro de feedbacks educacionais, envio assíncrono de alertas críticos e geração de relatórios semanais. A solução utiliza Java 21, Quarkus, Azure Functions, PostgreSQL, Azure Storage Queue, Azure Communication Services Email, Application Insights e infraestrutura declarada em Bicep.

## Escopo funcional

- Receber feedbacks públicos com descrição e nota de 0 a 10.
- Classificar automaticamente a urgência do feedback.
- Persistir os dados em PostgreSQL com schema versionado pelo Flyway.
- Publicar eventos apenas para feedbacks críticos.
- Consumir eventos por Queue Trigger e enviar alertas por e-mail.
- Garantir idempotência no processamento de mensagens.
- Gerar e enviar um relatório semanal consolidado.
- Autenticar o administrador com BCrypt e JWT RS256.
- Proteger consultas administrativas por papel `ADMIN`.
- Padronizar erros com Problem Details e rastrear operações por `X-Correlation-ID`.
- Disponibilizar OpenAPI, Swagger UI, health checks, métricas e logs estruturados.

## Arquitetura

```text
Cliente, Postman ou Swagger UI
              |
              v
Azure Function HTTP + Quarkus REST
  - envio público de feedback
  - login administrativo
  - consultas protegidas por JWT
              |
              +-----------------------> PostgreSQL
              |
              +--> Azure Storage Queue
                         |
                         v
              Azure Function Notification
                - Queue Trigger
                - idempotência
                - persistência de status
                - Azure Communication Services Email

Azure Function Report
  - Timer Trigger semanal
  - consolidação no PostgreSQL
  - idempotência por período
  - Azure Communication Services Email

Application Insights recebe a telemetria das três Function Apps.
Key Vault armazena os segredos usados pelas aplicações.
```

## Regras de negócio

| Nota | Urgência | Comportamento |
|---:|---|---|
| 0 a 4 | `CRITICA` | Persiste e publica evento na fila |
| 5 a 7 | `ATENCAO` | Persiste sem publicar evento |
| 8 a 10 | `NORMAL` | Persiste sem publicar evento |

A descrição deve possuir entre 10 e 2000 caracteres. A nota deve estar entre 0 e 10, inclusive.

## Módulos

| Módulo | Responsabilidade |
|---|---|
| [`edufeedback-domain`](edufeedback-domain/README.md) | Regras e enumerações independentes de infraestrutura |
| [`edufeedback-api-common`](edufeedback-api-common/README.md) | Problem Details, exceções e correlação HTTP |
| [`edufeedback-persistence`](edufeedback-persistence/README.md) | Entidades, repositórios Panache e migrations Flyway |
| [`edufeedback-messaging`](edufeedback-messaging/README.md) | Contrato do evento e integração com Azure Storage Queue |
| [`edufeedback-email`](edufeedback-email/README.md) | Integração com Azure Communication Services Email |
| [`edufeedback-http-function`](edufeedback-http-function/README.md) | API pública, autenticação e consultas administrativas |
| [`edufeedback-notification-function`](edufeedback-notification-function/README.md) | Consumo da fila e alertas críticos |
| [`edufeedback-report-function`](edufeedback-report-function/README.md) | Relatório semanal por Timer Trigger |
| [`infra`](infra/README.md) | Infraestrutura Azure e automação de deployment |

## Endpoints da API HTTP

A Function HTTP usa uma rota curinga da Azure e encaminha as requisições para o Quarkus. O nível de autorização da Function é anônimo; a API aplica autenticação e autorização por JWT.

| Método | Caminho | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Público | Emite JWT administrativo |
| `POST` | `/api/v1/feedbacks` | Público | Registra um feedback |
| `GET` | `/api/v1/feedbacks` | `ADMIN` | Lista os feedbacks |
| `GET` | `/api/v1/feedbacks/{id}` | `ADMIN` | Consulta um feedback pelo ID |

Exemplo de envio:

```json
{
  "descricao": "A aula apresentou o conteúdo com clareza.",
  "nota": 9
}
```

Exemplo de resposta:

```json
{
  "id": "5c9024cb-0e1f-46fe-92cc-63684830a51a",
  "descricao": "A aula apresentou o conteúdo com clareza.",
  "nota": 9,
  "urgencia": "NORMAL",
  "dataEnvio": "2026-07-30T23:20:00Z",
  "correlationId": "9dfabb14-1836-49d7-853a-d5a3900b1b0f"
}
```

A criação retorna `201`, o cabeçalho `Location` e `X-Correlation-ID`. Feedbacks críticos também retornam `X-Event-ID`.

## Autenticação e autorização

Na primeira inicialização, a HTTP Function cria o administrador de demonstração somente quando ele ainda não existe:

```text
username: admin
password: admin123
role: ADMIN
```

A senha é convertida para BCrypt antes da persistência. O login emite um JWT RS256 com duração padrão de 3600 segundos.

```http
Authorization: Bearer <accessToken>
```

As chaves RSA presentes nos resources atendem ao ambiente acadêmico e à execução reproduzível. Em um ambiente corporativo, as chaves devem ser externas, rotacionadas e administradas por um provedor de identidade ou cofre de segredos.

## Processamento assíncrono

Um feedback `CRITICA` gera um `FeedbackCriticoEvent` com `eventId`, `correlationId`, data do evento e dados mínimos do feedback. A avaliação é confirmada no banco antes da publicação.

A Notification Function:

1. recebe a mensagem da fila `feedback-critical-notifications`;
2. valida e desserializa o evento;
3. verifica se o evento já foi processado;
4. prepara ou recupera o registro de notificação;
5. envia o e-mail;
6. registra o resultado e o evento processado.

Falhas fazem a Azure Functions repetir a mensagem conforme a política do Queue Trigger. Após o limite de tentativas, a mensagem pode ser movida para a fila poison.

## Relatório semanal

A Report Function executa com a expressão NCRONTAB:

```text
0 0 11 * * MON
```

O disparo ocorre às 11:00 UTC de segunda-feira. A aplicação calcula o período no timezone configurado por `APP_TIMEZONE`, consolida quantidade, média e distribuição por urgência, persiste o relatório e envia o e-mail. A restrição única por período impede duplicidade.

## Diagnósticos internos

Os endpoints abaixo pertencem às Functions de Notification e Report e exigem Function Key. Eles são destinados a validação operacional, pipeline ou uso administrativo controlado; não devem ser expostos em frontend ou aplicativo mobile.

### Notification

| Método | Caminho |
|---|---|
| `GET` | `/api/diagnostics/notifications/health` |
| `POST` | `/api/diagnostics/notifications/email` |
| `POST` | `/api/diagnostics/notifications/critical` |
| `GET` | `/api/diagnostics/notifications/status/{eventId}` |
| `GET` | `/api/diagnostics/notifications/queues` |

### Report

| Método | Caminho |
|---|---|
| `GET` | `/api/diagnostics/reports/health` |
| `POST` | `/api/diagnostics/reports/email` |
| `POST` | `/api/diagnostics/reports/weekly` |

## Problem Details

Erros da API são representados em JSON com campos compatíveis com RFC 7807 e extensões para diagnóstico:

```json
{
  "type": "about:blank",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Feedback não encontrado.",
  "instance": "/api/v1/feedbacks/00000000-0000-0000-0000-000000000000",
  "errorCode": "FEEDBACK_NOT_FOUND",
  "traceId": "c7058ec9-7819-4a60-a514-8e291bf9a858",
  "timestamp": "2026-07-30T23:20:00Z"
}
```

A aplicação trata validação, JSON malformado, credenciais inválidas, acesso não autorizado, recurso inexistente, conflito, método não permitido, tipo de mídia não suportado, falhas externas e erros inesperados.

## Persistência

O banco usa o schema `edufeedback` e migrations Flyway:

| Migration | Estrutura |
|---|---|
| `V1` | Criação do schema |
| `V2` | Avaliações |
| `V3` | Notificações |
| `V4` | Relatórios semanais |
| `V5` | Eventos processados |
| `V6` | Usuário administrativo |

O Hibernate usa `validate`; alterações estruturais são feitas por migration.

## Observabilidade

Cada requisição recebe ou gera um `X-Correlation-ID`. O valor é devolvido na resposta e utilizado nos logs de negócio. Eventos relevantes incluem:

```text
event=http.request.started
event=http.request.completed
event=auth.login.succeeded
event=feedback.created
event=queue.message.published
event=queue.trigger.received
event=notification.sent
event=report.sent
event=email.send.succeeded
event=api.error
```

Em produção, os logs são emitidos em JSON e enviados ao Application Insights. Senhas, hashes, tokens e connection strings não são registrados.

Consulta KQL básica:

```kusto
traces
| where timestamp > ago(30m)
| where message contains "event="
| project timestamp, cloud_RoleName, message, severityLevel
| order by timestamp desc
```

## OpenAPI, Swagger e health

Em desenvolvimento Quarkus:

```text
/q/openapi
/q/swagger-ui
/q/health
```

Nas Azure Functions, o prefixo padrão é `/api`:

```text
/api/q/openapi
/api/q/swagger-ui
/api/q/health
```

## Configuração

### HTTP Function

| Variável | Finalidade |
|---|---|
| `AZURE_HTTP_FUNCTION_APP` | Nome usado no pacote da Function |
| `DATABASE_URL` | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` | Usuário do banco |
| `DATABASE_PASSWORD` | Senha do banco |
| `AZURE_STORAGE_CONNECTION_STRING` | Acesso à Storage Queue |
| `QUEUE_NAME` | Nome da fila de feedback crítico |

### Notification Function

| Variável | Finalidade |
|---|---|
| `AZURE_NOTIFICATION_FUNCTION_APP` | Nome usado no pacote da Function |
| `DATABASE_URL` | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` | Usuário do banco |
| `DATABASE_PASSWORD` | Senha do banco |
| `AZURE_STORAGE_CONNECTION_STRING` | Acesso à Storage Queue |
| `QUEUE_NAME` | Fila consumida pelo Queue Trigger |
| `AZURE_COMMUNICATION_CONNECTION_STRING` | Acesso ao serviço de e-mail |
| `EMAIL_SENDER` | Endereço remetente validado |
| `ADMIN_EMAIL` | Destinatário dos alertas |

### Report Function

| Variável | Finalidade |
|---|---|
| `AZURE_REPORT_FUNCTION_APP` | Nome usado no pacote da Function |
| `DATABASE_URL` | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` | Usuário do banco |
| `DATABASE_PASSWORD` | Senha do banco |
| `AZURE_COMMUNICATION_CONNECTION_STRING` | Acesso ao serviço de e-mail |
| `EMAIL_SENDER` | Endereço remetente validado |
| `ADMIN_EMAIL` | Destinatário do relatório |
| `APP_TIMEZONE` | Timezone usado no cálculo do período |

A infraestrutura também configura `AzureWebJobsStorage`, `FUNCTIONS_WORKER_RUNTIME`, `FUNCTIONS_EXTENSION_VERSION`, `QUARKUS_PROFILE` e `APPLICATIONINSIGHTS_CONNECTION_STRING`.

## Pré-requisitos

- JDK 21;
- Maven 3.9 ou Maven Wrapper;
- PostgreSQL para execução local persistente;
- Azurite ou uma conta Azure Storage para o fluxo de fila;
- Azure Functions Core Tools para executar Queue e Timer Triggers localmente;
- Azure CLI e Bicep CLI para administrar a infraestrutura.

## Build e testes

Linux ou macOS:

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

O build executa compilação, testes JUnit, testes HTTP com Quarkus, Spotless e JaCoCo. Os testes da HTTP Function usam H2 em memória no modo de compatibilidade PostgreSQL.

## Execução local da API HTTP

Linux ou macOS:

```bash
./mvnw -pl edufeedback-http-function -am quarkus:dev
```

Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-http-function -am quarkus:dev
```

A porta padrão é `8081`.

## Coleção Postman

Importe:

```text
postman/EduFeedback.postman_collection.json
```

A coleção contém cenários de sucesso e falha e pode ser executada integralmente pelo Collection Runner. Ela gera os IDs, o JWT e os identificadores de correlação durante a execução. Para a validação completa, preencha somente as Function Keys de Notification e Report. A API HTTP normal não exige Function Key.

## CI/CD

| Workflow | Finalidade |
|---|---|
| `.github/workflows/ci.yml` | Build, testes, formatação e publicação dos relatórios |
| `.github/workflows/application.yml` | Empacotamento e deployment das três Function Apps |
| `.github/workflows/infra.yml` | Validação, what-if e deployment da infraestrutura Bicep |

Os workflows de aplicação e infraestrutura usam autenticação OIDC. Secrets necessários no GitHub:

```text
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
ADMIN_EMAIL
POSTGRES_ADMIN_PASSWORD
```

Os dois últimos são usados somente pelo workflow de infraestrutura.

## Estrutura do repositório

```text
.github/workflows/               Pipelines de CI/CD
edufeedback-api-common/          Contratos HTTP compartilhados
edufeedback-domain/              Regras de domínio
edufeedback-email/               Adapter de e-mail
edufeedback-http-function/       API REST em Azure Function
edufeedback-messaging/           Adapter de fila
edufeedback-notification-function/ Queue Trigger e notificação
edufeedback-persistence/         Persistência e migrations
edufeedback-report-function/     Timer Trigger e relatório
infra/                           Recursos Azure em Bicep
postman/                         Coleção de validação
pom.xml                          Agregador Maven
```

## Segurança operacional

- Function Keys de diagnóstico permanecem fora do código-fonte.
- Segredos de aplicação são referenciados pelo Key Vault.
- Managed Identities recebem acesso de leitura aos segredos.
- Deployment de aplicação e infraestrutura usa OIDC, sem Publish Profile versionado.
- O acesso HTTP é restrito por TLS 1.2 ou superior e HTTPS obrigatório.
- O endpoint público registra somente feedbacks; consultas de dados exigem JWT `ADMIN`.
