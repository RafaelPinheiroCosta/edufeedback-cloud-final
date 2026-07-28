param name string
param location string
param keyVaultName string
param tags object

resource account 'Microsoft.Storage/storageAccounts@2025-06-01' = {
  name: name
  location: location
  tags: tags
  sku: { name: 'Standard_LRS' }
  kind: 'StorageV2'
  properties: {
    allowBlobPublicAccess: false
    allowCrossTenantReplication: false
    supportsHttpsTrafficOnly: true
    minimumTlsVersion: 'TLS1_2'
    publicNetworkAccess: 'Enabled'
  }
}

resource queueService 'Microsoft.Storage/storageAccounts/queueServices@2025-06-01' = {
  parent: account
  name: 'default'
}

resource criticalQueue 'Microsoft.Storage/storageAccounts/queueServices/queues@2025-06-01' = {
  parent: queueService
  name: 'feedback-critical-notifications'
}

resource weeklyQueue 'Microsoft.Storage/storageAccounts/queueServices/queues@2025-06-01' = {
  parent: queueService
  name: 'weekly-feedback-report'
}

resource vault 'Microsoft.KeyVault/vaults@2024-11-01' existing = {
  name: keyVaultName
}

var storageKey = account.listKeys().keys[0].value
var connectionString = 'DefaultEndpointsProtocol=https;AccountName=${account.name};AccountKey=${storageKey};EndpointSuffix=${environment().suffixes.storage}'

resource storageSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: vault
  name: 'storage-connection-string'
  properties: {
    value: connectionString
  }
}

output id string = account.id
output name string = account.name
output connectionSecretUri string = storageSecret.properties.secretUriWithVersion
