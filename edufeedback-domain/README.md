# edufeedback-domain

Módulo independente de frameworks responsável pelas regras centrais do EduFeedback.

## Responsabilidades

- Classificar a urgência de uma nota.
- Definir enumerações compartilhadas de urgência, notificação e relatório.
- Manter regras determinísticas sem dependência de banco, HTTP ou Azure.

## Regra de urgência

| Nota | Resultado |
|---:|---|
| 0 a 4 | `CRITICA` |
| 5 a 7 | `ATENCAO` |
| 8 a 10 | `NORMAL` |

Valores fora do intervalo de 0 a 10 geram `IllegalArgumentException`.

## Principais tipos

| Tipo | Uso |
|---|---|
| `CalculadoraUrgencia` | Executa a classificação da nota |
| `Urgencia` | `CRITICA`, `ATENCAO` ou `NORMAL` |
| `StatusNotificacao` | Estado de uma notificação |
| `TipoNotificacao` | Tipo do envio registrado |
| `StatusRelatorio` | Estado do relatório semanal |

## Dependências

O código de produção utiliza somente a biblioteca padrão do Java. JUnit 5 e AssertJ são usados nos testes.

## Testes

```bash
./mvnw -pl edufeedback-domain test
```

No Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-domain test
```

## Integração

O módulo é consumido por HTTP, persistência, Notification e Report. Alterações em regras ou enumerações devem permanecer compatíveis com os valores aceitos pelas constraints das migrations SQL.
