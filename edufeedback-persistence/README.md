# edufeedback-persistence

Camada compartilhada de persistência.

## Responsabilidades

- entidades JPA/Panache;
- repositórios;
- migrations Flyway;
- schema `edufeedback` no PostgreSQL;
- armazenamento do usuário administrativo com hash BCrypt.

A migration `V6__create_usuario.sql` cria a tabela `usuario`. A criação do registro bootstrap pertence à HTTP Function, evitando que módulos de infraestrutura assumam regras de autenticação.

## Dependências

- `edufeedback-domain`;
- Hibernate ORM Panache;
- PostgreSQL JDBC;
- Flyway.

## Build

```bash
mvn -pl edufeedback-persistence -am clean verify
```
