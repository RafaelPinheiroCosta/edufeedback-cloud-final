param location string
param planName string
param storageConnectionSecretUri string
param databaseUrlSecretUri string
param databaseUserSecretUri string
param databasePasswordSecretUri string
param communicationConnectionSecretUri string
param emailSenderSecretUri string
param appInsightsConnectionString string
param keyVaultName string
param adminEmail string
param tags object
param apps array

var keyVaultSecretsUserRoleId = '4633458b-17de-408a-b874-0445c86b69e6'

resource plan 'Microsoft.Web/serverfarms@2024-11-01' = {
  name: planName
  location: location
  tags: tags
  kind: 'linux'
  sku: { name: 'Y1', tier: 'Dynamic' }
  properties: {
    reserved: true
  }
}

resource functions 'Microsoft.Web/sites@2024-11-01' = [for app in apps: {
  name: app.name
  location: location
  tags: union(tags, { componente: app.component })
  kind: 'functionapp,linux'
  identity: { type: 'SystemAssigned' }
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
    publicNetworkAccess: 'Enabled'
    siteConfig: {
      alwaysOn: false
      ftpsState: 'Disabled'
      http20Enabled: true
      minTlsVersion: '1.2'
      linuxFxVersion: 'Java|21'
      appSettings: [
        { name: 'FUNCTIONS_EXTENSION_VERSION', value: '~4' }
        { name: 'FUNCTIONS_WORKER_RUNTIME', value: 'java' }
        { name: 'WEBSITE_RUN_FROM_PACKAGE', value: '1' }
        { name: 'QUARKUS_PROFILE', value: 'prod' }
        { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: appInsightsConnectionString }
        { name: 'AzureWebJobsStorage', value: '@Microsoft.KeyVault(SecretUri=${storageConnectionSecretUri})' }
        { name: 'AZURE_STORAGE_CONNECTION_STRING', value: '@Microsoft.KeyVault(SecretUri=${storageConnectionSecretUri})' }
        { name: 'DATABASE_URL', value: '@Microsoft.KeyVault(SecretUri=${databaseUrlSecretUri})' }
        { name: 'DATABASE_USERNAME', value: '@Microsoft.KeyVault(SecretUri=${databaseUserSecretUri})' }
        { name: 'DATABASE_PASSWORD', value: '@Microsoft.KeyVault(SecretUri=${databasePasswordSecretUri})' }
        { name: 'AZURE_COMMUNICATION_CONNECTION_STRING', value: '@Microsoft.KeyVault(SecretUri=${communicationConnectionSecretUri})' }
        { name: 'EMAIL_SENDER', value: '@Microsoft.KeyVault(SecretUri=${emailSenderSecretUri})' }
        { name: 'ADMIN_EMAIL', value: adminEmail }
        { name: 'QUEUE_NAME', value: 'feedback-critical-notifications' }
        { name: 'APP_TIMEZONE', value: 'America/Sao_Paulo' }
        { name: app.appNameSetting, value: app.name }
      ]
    }
  }
}]

resource vault 'Microsoft.KeyVault/vaults@2024-11-01' existing = {
  name: keyVaultName
}

resource keyVaultReader 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for (app, i) in apps: {
  name: guid(resourceGroup().id, keyVaultName, app.name, keyVaultSecretsUserRoleId)
  scope: vault
  properties: {
    principalId: functions[i].identity.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', keyVaultSecretsUserRoleId)
  }
}]

output names array = [for (app, i) in apps: functions[i].name]
output principalIds array = [for (app, i) in apps: functions[i].identity.principalId]
