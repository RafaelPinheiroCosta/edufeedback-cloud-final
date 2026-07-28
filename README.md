# EduFeedback Cloud

Plataforma serverless para registrar feedbacks educacionais, enviar alertas de avaliações críticas e produzir relatórios semanais. O projeto foi desenvolvido em Java 21 e Quarkus, executado em Azure Functions e integrado a PostgreSQL, Azure Storage Queue, Azure Communication Services Email e Application Insights.

## Arquitetura

```text
Cliente / Postman / Swagger
          |
          v
Azure Function HTTP (Quarkus REST)
  - feedback público
  - autenticação administrativa
  - consultas protegidas por JWT
          |
          +--> PostgreSQL
          |
          +--> Azure Storage Queue
                    |
                    v
          Azure Function Notification
          - Queue Trigger
          - endpoint administrativo de teste
          - ACS Email

Azure Function Report
- Timer Trigger semanal
- endpoint administrativo de teste
- PostgreSQL + ACS Email

As três Function Apps enviam logs ao Azure Application Insights.
```

## Módulos

| Módulo | Responsabilidade |
|---|---|
| `edufeedback-domain` | Regras, modelos e contratos de domínio |
| `edufeedback-api-common` | Problem Details, tratamento global e correlação HTTP |
| `edufeedback-persistence` | Entidades, repositórios Panache e migrations Flyway |
| `edufeedback-messaging` | Publicação e contratos da Azure Storage Queue |
| `edufeedback-email` | Integração com Azure Communication Services Email |
| `edufeedback-http-function` | API REST pública, login e consultas administrativas |
| `edufeedback-notification-function` | Consumo da fila e envio de alertas críticos |
| `edufeedback-report-function` | Geração e envio do relatório semanal |

Cada módulo possui um README próprio com suas responsabilidades e forma de execução.

## Autenticação e administrador bootstrap

O projeto mantém autenticação JWT RS256 e RBAC. O envio de feedback é público; consultas e operações administrativas exigem o papel `ADMIN`.

Para simplificar a avaliação do Tech Challenge, a HTTP Function cria automaticamente, na primeira inicialização, um administrador de demonstração:

```text
usuário: admin
senha: admin123
```

A senha **não é salva em texto puro**. O bootstrap aplica BCrypt e persiste somente o hash na tabela `edufeedback.usuario`. Em reinicializações posteriores, o usuário existente é preservado e a senha não é redefinida.

Fluxo:

```text
POST /api/v1/auth/login
  -> busca o usuário ativo no PostgreSQL
  -> valida a senha com BCrypt
  -> gera JWT RS256 com papel ADMIN
  -> retorna accessToken do tipo Bearer
```

Exemplo de resposta:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Os endpoints protegidos recebem:

```http
Authorization: Bearer <accessToken>
```

As chaves RSA incluídas nos resources existem apenas para tornar a demonstração reproduzível. Em produção, as credenciais devem ser gerenciadas por um provedor de identidade e as chaves armazenadas no Azure Key Vault, com rotação e auditoria.

## Problem Details e tratamento de erros

O módulo `edufeedback-api-common` centraliza o tratamento de erros e devolve respostas compatíveis com Problem Details. São tratados, entre outros:

- validação e JSON malformado: 400;
- autenticação: 401;
- autorização: 403;
- recurso inexistente: 404;
- método não permitido: 405;
- conflito: 409;
- mídia não suportada: 415;
- regra de negócio: 422;
- falha inesperada: 500.

O mesmo padrão é reutilizado pelas três APIs HTTP, sem pacotes de erro duplicados.

## Observabilidade

O código registra logs estruturados de autenticação, feedback, fila, notificação, relatório, e-mail e falhas. Um filtro HTTP recebe ou gera `X-Correlation-ID`, inclui o valor no MDC e o devolve na resposta.

Eventos relevantes incluem:

```text
event=security.bootstrap.created
event=auth.login.succeeded
event=feedback.created
event=queue.message.published
event=queue.trigger.received
event=notification.sent
event=report.sent
event=email.send.succeeded
event=api.error
```

Não são registrados senhas, hashes, JWTs, connection strings ou o conteúdo completo dos e-mails.

No Azure, mantenha nas três Function Apps:

```text
APPLICATIONINSIGHTS_CONNECTION_STRING
APPLICATIONINSIGHTS_ENABLE_AGENT
```

Consulta KQL inicial:

```kusto
traces
| where timestamp > ago(30m)
| where message contains "event="
| project timestamp, message, severityLevel
| order by timestamp desc
```

## Swagger e OpenAPI

Em execução local:

```text
/q/swagger-ui
/q/openapi
```

Na Azure Functions, normalmente há o prefixo `/api`:

```text
/api/q/swagger-ui
/api/q/openapi
```

O Swagger apresenta o botão **Authorize** para informar o Bearer Token.

## Variáveis de ambiente

### HTTP Function

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_STORAGE_CONNECTION_STRING
QUEUE_NAME
```

### Notification Function

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

### Report Function

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_COMMUNICATION_CONNECTION_STRING
EMAIL_SENDER
ADMIN_EMAIL
APP_TIMEZONE
```

Valores internos da plataforma, como `AzureWebJobsStorage`, `DEPLOYMENT_STORAGE_CONNECTION_STRING` e `APPLICATIONINSIGHTS_CONNECTION_STRING`, devem ser preservados nas Function Apps.

## Banco de dados

O Flyway executa as migrations ao iniciar. A migration `V6__create_usuario.sql` cria a tabela usada pelo administrador bootstrap. A estratégia do Hibernate é `validate`, evitando alterações automáticas no schema.

## Execução local

Pré-requisitos:

- JDK 21;
- Maven 3.9+;
- PostgreSQL acessível;
- emulador ou conta de Azure Storage para os fluxos de fila.

Na raiz:

```bash
mvn spotless:apply
mvn clean verify
```

Executar a HTTP Function em desenvolvimento:

```bash
mvn -pl edufeedback-http-function -am quarkus:dev
```

As demais Functions podem ser iniciadas pelos respectivos módulos conforme seus READMEs.

## Testes com Postman

Importe:

```text
postman/EduFeedback-Azure-JWT.postman_collection.json
```

A coleção está preparada para autenticar com `admin/admin123`, salvar o token e usá-lo nas requisições administrativas. Ajuste apenas as URLs das três Function Apps quando necessário.

## CI/CD com GitHub Actions

Estrutura:

```text
.github/workflows/
├── ci.yml
├── cd-http.yml
├── cd-notification.yml
├── cd-report.yml
└── reusable-function-deploy.yml
```

O CI compila, testa, executa Spotless e gera relatórios JaCoCo. Após sucesso na branch `main`, cada workflow de CD chama o mesmo workflow reutilizável.

Secrets exigidos no repositório:

```text
AZURE_FUNCTIONAPP_PUBLISH_PROFILE_HTTP
AZURE_FUNCTIONAPP_PUBLISH_PROFILE_NOTIFICATION
AZURE_FUNCTIONAPP_PUBLISH_PROFILE_REPORT
```

Function Apps esperadas pelos workflows:

```text
func-edufeedback-http
func-edufeedback-notification
func-edufeedback-report
```

## Substituição das Functions existentes

1. Confirme as variáveis de ambiente e o Application Insights nas três Function Apps.
2. Habilite a autenticação básica SCM necessária ao Publish Profile.
3. Baixe um Publish Profile novo de cada Function App.
4. Cadastre os três secrets no GitHub Actions.
5. Execute `mvn spotless:apply` e `mvn clean verify` localmente.
6. Faça push para `main`.
7. Verifique o CI e os três CDs.
8. Teste login, feedback, notificação, relatório, Swagger e logs.

O deploy substitui o pacote de código, mas mantém a infraestrutura, URLs, configurações e integrações já existentes na Azure.

## Infraestrutura Azure com Bicep

A infraestrutura completa e os pipelines OIDC estão em [`infra/`](infra/README.md). A migração preserva o código funcional e adiciona `validate`, `what-if`, deployment modular, Key Vault references, Managed Identity e deploy das três Functions.
