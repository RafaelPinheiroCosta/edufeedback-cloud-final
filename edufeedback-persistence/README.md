# edufeedback-persistence

Módulo de persistência compartilhado, implementado com Hibernate ORM Panache, PostgreSQL e Flyway.

## Responsabilidades

- Mapear as entidades JPA do domínio persistido.
- Disponibilizar repositórios Panache.
- Versionar o schema `edufeedback` com migrations SQL.
- Preservar constraints e índices no banco.
- Armazenar auditoria, idempotência e estados de processamento.

## Entidades

| Entidade | Tabela | Finalidade |
|---|---|---|
| `AvaliacaoEntity` | `avaliacao` | Feedback recebido e sua urgência |
| `NotificacaoEntity` | `notificacao` | Estado do alerta por e-mail |
| `RelatorioSemanalEntity` | `relatorio_semanal` | Consolidação semanal |
| `EventoProcessadoEntity` | `evento_processado` | Idempotência dos consumidores |
| `UsuarioEntity` | `usuario` | Administrador e hash BCrypt |
| `EntidadeAuditavel` | Classe base | Datas de criação e atualização |

## Migrations

| Arquivo | Conteúdo |
|---|---|
| `V1__create_schema.sql` | Schema `edufeedback` |
| `V2__create_avaliacao.sql` | Avaliações, validações e índices |
| `V3__create_notificacao.sql` | Notificações e unicidade por evento/tipo |
| `V4__create_relatorio.sql` | Relatórios e unicidade por período |
| `V5__create_evento_processado.sql` | Controle idempotente por consumidor |
| `V6__create_usuario.sql` | Usuário administrativo |

## Integridade

- Nota limitada a 0..10.
- Descrição limitada a 10..2000 caracteres.
- Chaves estrangeiras entre notificação e avaliação.
- Unicidade de notificação por `event_id` e tipo.
- Unicidade de evento por consumidor.
- Unicidade de relatório por período.
- Unicidade do username administrativo.
- Controle otimista por campo de versão nas entidades mutáveis.

## Configuração

Os módulos executáveis fornecem:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

A HTTP Function executa o Flyway na inicialização. Notification e Report usam o schema já migrado e mantêm `quarkus.flyway.migrate-at-start=false`.

## Build

```bash
./mvnw -pl edufeedback-persistence -am test
```

No Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-persistence -am test
```

## Evolução do banco

Mudanças estruturais devem ser adicionadas em uma nova migration numerada. Migrations já aplicadas não devem ser reescritas. O Hibernate permanece com estratégia `validate` nos ambientes executáveis.
