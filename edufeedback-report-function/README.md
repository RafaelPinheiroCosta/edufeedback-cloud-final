# edufeedback-report-function

Azure Function responsável por consolidar e enviar o relatório semanal do EduFeedback.

## Responsabilidades

- Executar por Timer Trigger.
- Calcular o período semanal no timezone configurado.
- Consolidar média, total e distribuição de urgências.
- Persistir o conteúdo e o estado do relatório.
- Impedir duplicidade por período.
- Enviar o relatório pelo Azure Communication Services Email.
- Expor diagnósticos protegidos por Function Key.
- Disponibilizar geração administrativa Quarkus protegida por JWT.

## Timer Trigger

| Function | Agenda NCRONTAB |
|---|---|
| `weeklyFeedbackReport` | `0 0 11 * * MON` |

A Azure interpreta a agenda em UTC. O período do relatório é calculado pela aplicação com `APP_TIMEZONE`, cujo valor padrão é `America/Sao_Paulo`.

## Conteúdo consolidado

- data inicial e final;
- média das notas;
- total de avaliações;
- total crítico;
- total atenção;
- total normal;
- conteúdo HTML enviado por e-mail;
- estado de geração e envio.

## Idempotência

A tabela `relatorio_semanal` possui restrição única para `data_inicio` e `data_fim`. Quando o período já foi enviado, o serviço retorna `JA_ENVIADO` e não repete o e-mail.

## Diagnósticos nativos

Os endpoints abaixo usam `AuthorizationLevel.FUNCTION`:

| Function | Método e caminho | Finalidade |
|---|---|---|
| `reportDiagnosticHealth` | `GET /api/diagnostics/reports/health` | Estado das configurações |
| `sendReportEmailDiagnostic` | `POST /api/diagnostics/reports/email` | Envio direto controlado |
| `runWeeklyFeedbackReportDiagnostic` | `POST /api/diagnostics/reports/weekly` | Execução do relatório para uma data de referência |

## Recurso administrativo Quarkus

| Método | Caminho | Acesso |
|---|---|---|
| `POST` | `/api/v1/admin/reports/weekly` | JWT `ADMIN` |

Corpo opcional:

```json
{
  "referenceDate": "2026-07-30"
}
```

## Configuração

```text
AZURE_REPORT_FUNCTION_APP
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
AZURE_COMMUNICATION_CONNECTION_STRING
EMAIL_SENDER
ADMIN_EMAIL
APP_TIMEZONE
```

A porta Quarkus de desenvolvimento é `8083`. As migrations não são executadas neste módulo.

## Build

```bash
./mvnw -pl edufeedback-report-function -am verify
```

No Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-report-function -am verify
```

## Empacotamento e execução com Core Tools

```bash
./mvnw -pl edufeedback-report-function -am package
func start --script-root edufeedback-report-function/target/azure-functions/<AZURE_REPORT_FUNCTION_APP> --port 7073
```

Para um teste seguro de idempotência, use uma data de referência cuja semana já esteja registrada ou consulte o estado antes de disparar o endpoint de diagnóstico.

## Dependências internas

```text
edufeedback-api-common
edufeedback-domain
edufeedback-persistence
edufeedback-email
```
