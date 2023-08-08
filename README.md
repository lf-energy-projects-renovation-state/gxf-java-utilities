# GXF Java utilities
This repository contains utility libraries for GXF java applications.


## kafka-azure-oauth
The library makes it possible to configure OAUTH Kafka authentication using environment variables

OAuth is enabled by setting the `kafka-oauth` profile.
The following environment variables are required:
- `AZURE_CLIENT_ID`: client id of the oauth server
- `AZURE_AUTHORITY_HOST`: url of the oauth server (ex: http://oauth-server.com/) 
- `AZURE_TENANT_ID`: Tenant id of the oauth resource
- `AZURE_FEDERATED_TOKEN_FILE`: File containing the oauth token.
- `OAUTH_SCOPE`: Scope of the oauth client


## kafka-avro
Library containing a Kafka serializer and deserializer for avro objects.

It can be configured with the encoder / decoder of a specific object using:
```java
new DefaultKafkaConsumerFactory(
  kafkaProperties.buildConsumerProperties(),
  ErrorHandlingDeserializer(AvroSerializer(AvroMessage.getEncoder())),
  ErrorHandlingDeserializer(AvroDeserializer(AvroMessage.getDecoder()))
)
```


## oauth-token-client
Library that easily configures the [msal4j](https://github.com/AzureAD/microsoft-authentication-library-for-java) oauth token provider. 

The client requires the following properties:
```properties
# If not set to true no other configuration is required
oauth.client.enabled=true

oauth.client.token-endpoint=https://localhost:56788/token
oauth.client.client-id=client-id
oauth.client.scope=client-scope

# Resources
oauth.client.private-key=classpath:keys/private-key.key
oauth.client.certificate=classpath:keys/certificate.crt
```
