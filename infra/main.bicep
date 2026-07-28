targetScope = 'resourceGroup'

param location string = resourceGroup().location
param environment string = 'prod'
param suffix string = 'rpc2026'

param storageName string = 'stedufeedback${suffix}'
param workspaceName string = 'log-edufeedback-${environment}'
param appInsightsName string = 'appi-edufeedback-${environment}'
param keyVaultName string = 'kv-edufeedback-${suffix}'

param postgresName string = 'pg-edufeedback-${suffix}'
param databaseName string = 'edufeedbackdb'
param databaseAdmin string = 'edufeedbackadmin'

@secure()
param databaseAdminPassword string

param communicationName string = 'acs-edufeedback-${suffix}'
param emailServiceName string = 'ecs-edufeedback-${suffix}'

param functionPlanName string = 'plan-edufeedback-${environment}'
param httpFunctionName string = 'func-edufeedback-http-${suffix}'
param notificationFunctionName string = 'func-edufeedback-notification-${suffix}'
param reportFunctionName string = 'func-edufeedback-report-${suffix}'

param adminEmail string

var tags = {
  projeto: 'edufeedback'
  ambiente: environment
  gerenciadoPor: 'bicep'
}

module keyVault 'modules/keyvault.bicep' = {
  name: 'keyvault'
  params: {
    name: keyVaultName
    location: location
    tenantId: tenant().tenantId
    tags: tags
  }
}

module storage 'modules/storage.bicep' = {
  name: 'storage'
  params: {
    name: storageName
    location: location
    keyVaultName: keyVaultName
    tags: tags
  }
  dependsOn: [
    keyVault
  ]
}

module monitoring 'modules/monitoring.bicep' = {
  name: 'monitoring'
  params: {
    location: location
    workspaceName: workspaceName
    appInsightsName: appInsightsName
    tags: tags
  }
}

module postgres 'modules/postgres.bicep' = {
  name: 'postgres'
  params: {
    name: postgresName
    location: location
    databaseName: databaseName
    administratorLogin: databaseAdmin
    administratorPassword: databaseAdminPassword
    tags: tags
  }
}

module communication 'modules/communication.bicep' = {
  name: 'communication'
  params: {
    communicationName: communicationName
    emailServiceName: emailServiceName
    keyVaultName: keyVaultName
    tags: tags
  }
  dependsOn: [
    keyVault
  ]
}

resource vault 'Microsoft.KeyVault/vaults@2024-11-01' existing = {
  name: keyVaultName
}

resource databaseUrlSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: vault
  name: 'database-url'
  properties: {
    value: postgres.outputs.jdbcUrl
  }
  dependsOn: [
    keyVault
    postgres
  ]
}

resource databaseUserSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: vault
  name: 'database-username'
  properties: {
    value: databaseAdmin
  }
  dependsOn: [
    keyVault
  ]
}

resource databasePasswordSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: vault
  name: 'database-password'
  properties: {
    value: databaseAdminPassword
  }
  dependsOn: [
    keyVault
  ]
}

resource emailSenderSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: vault
  name: 'email-sender'
  properties: {
    value: communication.outputs.senderAddress
  }
  dependsOn: [
    keyVault
    communication
  ]
}

module functions 'modules/functions.bicep' = {
  name: 'functions'
  params: {
    location: location
    planName: functionPlanName

    storageConnectionString: storage.outputs.connectionString
    storageConnectionSecretUri: storage.outputs.connectionSecretUri

    databaseUrlSecretUri: databaseUrlSecret.properties.secretUriWithVersion
    databaseUserSecretUri: databaseUserSecret.properties.secretUriWithVersion
    databasePasswordSecretUri: databasePasswordSecret.properties.secretUriWithVersion

    communicationConnectionSecretUri: communication.outputs.connectionSecretUri
    emailSenderSecretUri: emailSenderSecret.properties.secretUriWithVersion

    appInsightsConnectionString: monitoring.outputs.connectionString
    keyVaultName: keyVaultName
    adminEmail: adminEmail
    tags: tags

    apps: [
      {
        name: httpFunctionName
        component: 'http'
        appNameSetting: 'AZURE_HTTP_FUNCTION_APP'
      }
      {
        name: notificationFunctionName
        component: 'notification'
        appNameSetting: 'AZURE_NOTIFICATION_FUNCTION_APP'
      }
      {
        name: reportFunctionName
        component: 'report'
        appNameSetting: 'AZURE_REPORT_FUNCTION_APP'
      }
    ]
  }
}

output storageAccountName string = storage.outputs.name
output keyVaultUri string = keyVault.outputs.uri
output postgresFqdn string = postgres.outputs.fqdn
output communicationServiceName string = communicationName
output emailSender string = communication.outputs.senderAddress
output functionApps array = functions.outputs.names