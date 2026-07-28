param name string
param location string
param databaseName string
param administratorLogin string
@secure()
param administratorPassword string
param tags object
param allowAzureServices bool = true

resource server 'Microsoft.DBforPostgreSQL/flexibleServers@2025-08-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    administratorLogin: administratorLogin
    administratorLoginPassword: administratorPassword
    version: '16'
    authConfig: {
      activeDirectoryAuth: 'Disabled'
      passwordAuth: 'Enabled'
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: { mode: 'Disabled' }
    network: { publicNetworkAccess: 'Enabled' }
    storage: {
      autoGrow: 'Enabled'
      storageSizeGB: 32
      tier: 'P4'
    }
  }
}

resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2025-08-01' = {
  parent: server
  name: databaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

resource allowAzure 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2025-08-01' = if (allowAzureServices) {
  parent: server
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

output id string = server.id
output fqdn string = server.properties.fullyQualifiedDomainName
output jdbcUrl string = 'jdbc:postgresql://${server.properties.fullyQualifiedDomainName}:5432/${databaseName}?sslmode=require'
