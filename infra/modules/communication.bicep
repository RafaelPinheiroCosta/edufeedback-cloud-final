param communicationName string
param emailServiceName string
param keyVaultName string
param dataLocation string = 'Brazil'
param tags object

resource emailService 'Microsoft.Communication/emailServices@2025-09-01' = {
  name: emailServiceName
  location: 'global'
  tags: tags
  properties: { dataLocation: dataLocation }
}

resource domain 'Microsoft.Communication/emailServices/domains@2025-09-01' = {
  parent: emailService
  name: 'AzureManagedDomain'
  location: 'global'
  properties: {
    domainManagement: 'AzureManaged'
    userEngagementTracking: 'Disabled'
  }
}

resource sender 'Microsoft.Communication/emailServices/domains/senderUsernames@2025-09-01' = {
  parent: domain
  name: 'DoNotReply'
  properties: {
    username: 'DoNotReply'
    displayName: 'EduFeedback'
  }
}

resource communication 'Microsoft.Communication/communicationServices@2025-09-01' = {
  name: communicationName
  location: 'global'
  tags: tags
  properties: {
    dataLocation: dataLocation
    disableLocalAuth: false
    linkedDomains: [domain.id]
    publicNetworkAccess: 'Enabled'
  }
}

resource vault 'Microsoft.KeyVault/vaults@2024-11-01' existing = {
  name: keyVaultName
}

resource communicationSecret 'Microsoft.KeyVault/vaults/secrets@2024-11-01' = {
  parent: vault
  name: 'communication-connection-string'
  properties: {
    value: communication.listKeys().primaryConnectionString
  }
}

output id string = communication.id
output connectionSecretUri string = communicationSecret.properties.secretUriWithVersion
output senderDomain string = domain.properties.mailFromSenderDomain
output senderAddress string = 'DoNotReply@${domain.properties.mailFromSenderDomain}'
