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

## kafka-message-signing

Library for signing Kafka messages and for verification of signed Kafka messages.

Two variations are supported:

- The signature is set on the message, via `SignableMessageWrapper`'s `signature` field;
- The signature is set as a `signature` header on the Kafka `ProducerRecord`.

The `MessageSigner` class is used for both signing and verifying a signature.

To sign a message, use `MessageSigner`'s `sign()` method: choose between `SignableMessageWrapper` or `ProducerRecord`.

To verify a signature, use `MessageSigner`'s `verify()` method: choose between `SignableMessageWrapper`
or `ProducerRecord`.

The `MessageSigner` class can be created using `MessageSigner.newBuilder()` with the following configuration options:

- signingEnabled
- stripAvroHeader
- signatureAlgorithm
- signatureProvider
- signatureKeyAlgorithm
- signatureKeySize
- privateKeyFile
- publicKeyFile

### Spring Auto Configuration

You can configure the settings in your `application.yaml` (or properties), for example:

```yaml
message-signing:
  signing-enabled: true
  strip-avro-header: true
  algorithm: SHA256withRSA
  provider: SunRsaSign
  key-algorithm: RSA
  key-size: 2048
  private-key-file: classpath:rsa-private.pem
  public-key-file: classpath:rsa-public.pem
```

### Custom or multiple certificates configuration

You can create your own `MessageSigningProperties` object and use `MessageSigner.newMessageSigner(props)`.
Spring Boot users can extend the `MessageSigningProperties` to add @ConfigurationProperties capabilities and/or to
support multiple configurations

### Creating a public/private key for signing

To generate a public/private keypair, use these two commands:

```shell
openssl genrsa -out rsa-private.pem 2048
openssl rsa -in rsa-private.pem -pubout -out rsa-public.pem
```

The private key (PKCS#8) and public key can then be set in the configuration as shown above.
To view the generated files, you can use

```shell
openssl rsa -noout -text -in rsa-private.pem
openssl rsa -noout -text -pubin -in rsa-public.pem
```

## oauth-token-client

Library that easily configures the [msal4j](https://github.com/AzureAD/microsoft-authentication-library-for-java) oauth
token provider.

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
