# edufeedback-api-common

Infraestrutura HTTP compartilhada pelas Function Apps.

## Responsabilidades

- tratamento global de exceções;
- respostas Problem Details;
- exceções semânticas reutilizáveis;
- criação e propagação de `X-Correlation-ID`;
- logs de início, conclusão, rejeição e falha das requisições.

O módulo evita mappers e modelos de erro duplicados nos módulos HTTP.

## Build

```bash
mvn -pl edufeedback-api-common -am clean verify
```
