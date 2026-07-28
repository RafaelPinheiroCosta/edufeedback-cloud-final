# Migração para Bicep sem regressão

## Preservado

Todos os módulos Java, migrations, testes, propriedades, JWT, filas e nomes já usados pelo projeto foram preservados. A mudança adiciona infraestrutura como código, scripts e pipelines; não remove regras de negócio nem altera contratos da API.

## Recursos gerenciados

- Storage Account e filas existentes
- Log Analytics e Application Insights existentes
- Key Vault existente, RBAC e segredos
- PostgreSQL Flexible Server 16 e banco `edufeedbackdb`
- Azure Communication Services, Email Service e Azure Managed Domain
- Plano Linux Consumption e três Azure Function Apps Java 21
- Managed Identity em cada Function e leitura de segredos via Key Vault references

## Segurança

O GitHub autentica por OIDC. A senha do banco fica como parâmetro seguro no deployment e segredo no Key Vault. As Functions não recebem segredos no repositório.

## Observação operacional

O Bicep mantém autenticação por connection string porque o código atual usa `AZURE_STORAGE_CONNECTION_STRING` e `AZURE_COMMUNICATION_CONNECTION_STRING`. Isso evita regressão. A evolução para SDK com `DefaultAzureCredential` pode ser feita depois, com testes específicos.
