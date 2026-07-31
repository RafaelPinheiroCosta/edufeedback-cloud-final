# edufeedback-http-function

Azure Function HTTP que hospeda a API REST principal do EduFeedback em Quarkus.

## Responsabilidades

- Receber feedbacks públicos.
- Validar o contrato de entrada.
- Classificar a urgência.
- Persistir avaliações no PostgreSQL.
- Publicar eventos para avaliações críticas.
- Criar o administrador bootstrap.
- Validar credenciais com BCrypt.
- Emitir JWT RS256.
- Proteger consultas administrativas com `ADMIN`.
- Expor OpenAPI, Swagger UI, health, métricas e logs correlacionados.

## Endpoints

| Método | Caminho | Acesso | Retorno esperado |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Público | `200` com JWT |
| `POST` | `/api/v1/feedbacks` | Público | `201` com feedback criado |
| `GET` | `/api/v1/feedbacks` | `ADMIN` | `200` com lista |
| `GET` | `/api/v1/feedbacks/{id}` | `ADMIN` | `200` ou `404` |

## Contratos

Login:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Feedback:

```json
{
  "descricao": "Descrição com pelo menos dez caracteres.",
  "nota": 4
}
```

## Fluxo de criação

1. `CorrelationIdFilter` valida ou cria `X-Correlation-ID`.
2. Bean Validation valida descrição e nota.
3. `CalculadoraUrgencia` classifica a nota.
4. A avaliação é persistida em uma transação confirmada.
5. Feedback crítico produz `FeedbackCriticoEvent`.
6. A resposta devolve `X-Correlation-ID` e, quando aplicável, `X-Event-ID`.

A confirmação da persistência antes da publicação evita que um consumidor rápido consulte um feedback ainda não visível em outra conexão do banco.

## Segurança

A Azure Function HTTP usa uma rota curinga com autorização anônima para permitir que o Quarkus controle a segurança por endpoint.

- Login e criação de feedback: `@PermitAll`.
- Listagem e consulta por ID: `@RolesAllowed("ADMIN")`.
- Algoritmo JWT: RS256.
- Emissor: `edufeedback`.
- Duração padrão: 3600 segundos.
- Senha administrativa persistida somente como hash BCrypt.

## Bootstrap administrativo

Na primeira inicialização, o módulo cria:

```text
admin / admin123 / ADMIN
```

O registro existente é preservado nas inicializações seguintes.

## Persistência

Este é o módulo responsável por executar as migrations Flyway no início. O schema padrão é `edufeedback`, e o Hibernate valida a estrutura existente.

## Configuração

```text
AZURE_HTTP_FUNCTION_APP
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_STORAGE_CONNECTION_STRING
QUEUE_NAME
```

A porta de desenvolvimento é `8081`.

## OpenAPI e health

```text
/q/openapi
/q/swagger-ui
/q/health
```

No host da Azure, use o prefixo `/api`.

## Execução em desenvolvimento

Linux ou macOS:

```bash
./mvnw -pl edufeedback-http-function -am quarkus:dev
```

Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-http-function -am quarkus:dev
```

## Testes

```bash
./mvnw -pl edufeedback-http-function -am verify
```

Os testes usam H2 em memória com compatibilidade PostgreSQL e não dependem de Docker.

## Empacotamento Azure Functions

```bash
./mvnw -pl edufeedback-http-function -am package
```

O pacote é produzido em:

```text
edufeedback-http-function/target/azure-functions/<AZURE_HTTP_FUNCTION_APP>/
```

## Dependências internas

```text
edufeedback-api-common
edufeedback-domain
edufeedback-persistence
edufeedback-messaging
```
