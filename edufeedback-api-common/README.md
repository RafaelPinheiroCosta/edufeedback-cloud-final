# edufeedback-api-common

Biblioteca compartilhada pelas APIs Quarkus para padronização de erros, exceções e rastreabilidade HTTP.

## Responsabilidades

- Representar respostas Problem Details.
- Traduzir exceções para códigos HTTP consistentes.
- Consolidar violações de validação.
- Tratar JSON vazio, incompleto ou malformado.
- Receber, validar, gerar e devolver `X-Correlation-ID`.
- Registrar início, conclusão, rejeição e falha das requisições.

## Problem Details

`ApiProblem` contém os campos básicos de RFC 7807 e extensões úteis:

| Campo | Descrição |
|---|---|
| `type` | Identificador do tipo do problema |
| `title` | Resumo legível |
| `status` | Status HTTP |
| `detail` | Explicação do erro |
| `instance` | Caminho da requisição |
| `errorCode` | Código estável da aplicação |
| `traceId` | Identificador de correlação |
| `timestamp` | Momento do erro |
| `violations` | Campos inválidos, quando aplicável |

## Exceções compartilhadas

| Exceção | Categoria |
|---|---|
| `AuthenticationException` | Falha de autenticação |
| `BusinessException` | Regra de negócio |
| `ExternalServiceException` | Dependência externa |
| `InvalidRequestException` | Requisição semanticamente inválida |
| `ResourceNotFoundException` | Recurso inexistente |

## Correlação HTTP

`CorrelationIdFilter` aceita um UUID em `X-Correlation-ID`. Quando o cabeçalho não é enviado, um UUID é criado. Valores inválidos geram resposta `400` com `INVALID_CORRELATION_ID`.

O identificador é inserido no MDC, incluído nos logs e devolvido no cabeçalho da resposta.

## CDI

O módulo inclui `META-INF/beans.xml` para descoberta dos providers, filtros e exception mappers quando empacotado como dependência.

## Build

```bash
./mvnw -pl edufeedback-api-common -am test
```

No Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-api-common -am test
```

## Uso

Adicione o módulo como dependência e mantenha o pacote `br.com.edufeedback.api` disponível no índice do Quarkus. As aplicações HTTP do repositório já possuem essa configuração.
