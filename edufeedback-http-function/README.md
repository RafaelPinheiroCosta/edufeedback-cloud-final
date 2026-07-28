# edufeedback-http-function

Azure Function HTTP responsável pela entrada pública do sistema e pela autenticação administrativa.

## Responsabilidades

- receber e validar feedbacks;
- persistir avaliações no PostgreSQL;
- publicar eventos críticos na Azure Storage Queue;
- criar o administrador bootstrap na primeira inicialização;
- autenticar o administrador com BCrypt;
- emitir JWT RS256 com papel `ADMIN`;
- disponibilizar consultas administrativas protegidas;
- expor Swagger, OpenAPI, health checks e logs correlacionados.

## Bootstrap de demonstração

```text
admin / admin123
```

O valor `admin123` é usado somente para criar a credencial na primeira inicialização. O banco recebe apenas o hash BCrypt. Se o usuário já existir, o bootstrap não altera a credencial.

## Principais endpoints

```text
POST /api/v1/auth/login
POST /api/v1/avaliacoes
GET  /api/v1/avaliacoes
```

Consulte `/q/swagger-ui` para a relação completa e os modelos de resposta.

## Dependências

- `edufeedback-api-common`;
- `edufeedback-domain`;
- `edufeedback-persistence`;
- `edufeedback-messaging`;
- Quarkus REST, SmallRye JWT, Hibernate Validator, OpenAPI e Azure Functions HTTP.

## Configuração

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_STORAGE_CONNECTION_STRING
QUEUE_NAME
```

## Build e execução

```bash
mvn -pl edufeedback-http-function -am clean verify
mvn -pl edufeedback-http-function -am quarkus:dev
```
