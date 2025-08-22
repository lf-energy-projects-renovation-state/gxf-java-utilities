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

- The signature is set on a field of the message, via `FlexibleSignableMessageWrapper`;
- The signature is set as a `signature` header on the Kafka `ProducerRecord`.

### Wrapping messages
Wrapping messages is required when using the 'sign using _field_' method.

Here's how to wrap your message in FlexibleSignableMessageWrapper:

```kotlin
class MyMessage(val body: String, var sig: ByteBuffer? = null)

val myMessage = MyMessage(body = "Hello, GXF!")

val wrapped : FlexibleSignableMessageWrapper<MyMessage> = FlexibleSignableMessageWrapper(
    myMessage,
    messageGetter = { msg -> ByteBuffer.wrap(msg.body.toByteArray()) },
    signatureGetter = { msg -> msg.sig },
    signatureSetter = { msg, sig -> msg.sig = sig }
)
```

### Signing and Verifying Messages
The `MessageSigner` class is used for both signing and verifying a signature.

To sign a message, use one of `MessageSigner`'s sign methods: 
- `signUsingField(...)` using the `FlexibleSignableMessageWrapper` to wrap your Avro object;
- `signUsingHeader(...)` to add the signature to the Avro object's header.

To verify a signature, use one of `MessageSigner`'s verify methods:
- `verifyUsingField(...)` using the `FlexibleSignableMessageWrapper` to wrap your Avro object;
- `verifyUsingHeader(...)` to read the signature from the Avro object's header.

### Spring Auto Configuration

You can configure the settings in your `application.yaml` (or properties), for example:

```yaml
message-signing:
  signing-enabled: true
  strip-avro-header: true
  signature-algorithm: SHA256withRSA
  signature-provider: SunRsaSign
  key-algorithm: RSA
  private-key-file: classpath:rsa-private.pem
  public-key-file: classpath:rsa-public.pem
```

### Custom or multiple certificates configuration

You can create your own `MessageSigningProperties` object and use `MessageSigner.newMessageSigner(props)`.
Spring Boot users can extend the `MessageSigningProperties` to add `@ConfigurationProperties` capabilities and/or to
support multiple configurations.

If you want to support multiple keys, you also have to instantiate multiple `MessageSigner` beans. 

Auto configuration (see above) will see when you defined your own MessageSigner or MessageSigningProperties bean and will not auto-create one.

```java
@ConfigurationProperties(prefix = "your-app.your-message-signing")
class YourMessageSigningProperties extends MessageSigningProperties {
}

@Configuration
class MessageSigningConfiguration {
    @Bean
    public MessageSigner yourMessageSigner(YourMessageSigningProperties yourMessageSigningProperties) {
        return new MessageSigner(yourMessageSigningProperties);
    }
}
```
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

## oslp-message-signing

Library for signing OSLP messages and for verification of signed OSLP messages with two methods:
- createSignature() to create a signature for an OSLP message based on the private key
- verifySignature() to verify the signature of an OSLP message based on the public key

The keys can be provided through the KeyProvider interface, which can be implemented to provide custom key retrieval logic.

The SigningUtil class use the security provider `SunRsaSign` and the algorithm `SHA256withRSA` as defaults.
Autoconfiguration is available for Spring Boot applications to change these defaults.