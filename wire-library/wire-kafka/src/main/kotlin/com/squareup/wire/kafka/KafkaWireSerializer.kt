package com.squareup.wire.kafka

import com.google.protobuf.Descriptors.Descriptor
import com.squareup.wire.Message
import com.squareup.wire.protos.ProtoTypeDescriptor
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.entities.Schema
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDe
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig
import io.confluent.kafka.serializers.subject.strategy.ReferenceSubjectNameStrategy
import org.apache.kafka.common.cache.Cache
import org.apache.kafka.common.cache.LRUCache
import org.apache.kafka.common.cache.SynchronizedCache
import org.apache.kafka.common.errors.InvalidConfigurationException
import org.apache.kafka.common.errors.SerializationException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class KafkaWireSerializer<T: Message<T, *>>(
  private val client: SchemaRegistryClient?,
  private val props: Map<String?, *>?
): AbstractKafkaSchemaSerDe() {

  private val isKey = false
  private val config = KafkaProtobufSerializerConfig(props)
  private var schemaCache: Cache<Descriptor, ProtobufSchema>? = null

  init {
    schemaCache = SynchronizedCache(LRUCache(DEFAULT_CACHE_CAPACITY))
  }

  constructor(): this(null, null)

  constructor(client: SchemaRegistryClient): this(client, null)

  fun serialize(topic: String?, record: T): ByteArray? {
    val descriptor: Descriptor = ProtoTypeDescriptor.getMessageDescriptor(record)
    var schema = schemaCache!![descriptor]
    if (schema == null) {
      schema = ProtobufSchema(descriptor)
      try {
        // Ensure dependencies are resolved before caching
        schema = resolveDependencies(
          schemaRegistry, config.autoRegisterSchema(),
          config.useLatestVersion(), config.latestCompatibilityStrict, latestVersions,
          config.referenceSubjectNameStrategyInstance(), topic, isKey, schema
        )
      } catch (e: IOException) {
        throw SerializationException("Error serializing Protobuf message", e)
      } catch (e: RestClientException) {
        throw SerializationException("Error serializing Protobuf message", e)
      }
      schemaCache!!.put(descriptor, schema)
    }
    return serializeImpl(
      getSubjectName(topic, isKey, record, schema),
      topic, isKey, record, schema
    )
  }

  fun close() {}

  @Throws(
    SerializationException::class,
    InvalidConfigurationException::class
  ) protected fun serializeImpl(
    subject: String?, topic: String?, isKey: Boolean, record: T?, schema: ProtobufSchema
  ): ByteArray? {
    // null needs to treated specially since the client most likely just wants to send
    // an individual null value instead of making the subject a null type. Also, null in
    // Kafka has a special meaning for deletion in a topic with the compact retention policy.
    // Therefore, we will bypass schema registration and return a null value in Kafka, instead
    // of an encoded null.
    var schema = schema
    if (record == null) {
      return null
    }
    var restClientErrorMsg = ""
    return try {
      schema = resolveDependencies(
        schemaRegistry, config.autoRegisterSchema(),
        config.useLatestVersion(), config.latestCompatibilityStrict, latestVersions,
        config.referenceSubjectNameStrategyInstance(), topic, isKey, schema
      )!!
      val id: Int
      if (config.autoRegisterSchema()) {
        restClientErrorMsg = "Error registering Protobuf schema: "
        id = schemaRegistry.register(subject, schema)
      } else if (config.useLatestVersion()) {
        restClientErrorMsg = "Error retrieving latest version: "
        schema = lookupLatestVersion(subject, schema, config.latestCompatibilityStrict) as ProtobufSchema
        id = schemaRegistry.getId(subject, schema)
      } else {
        restClientErrorMsg = "Error retrieving Protobuf schema: "
        id = schemaRegistry.getId(subject, schema)
      }
      val out = ByteArrayOutputStream()
      out.write(MAGIC_BYTE.toInt())
      out.write(ByteBuffer.allocate(idSize).putInt(id).array())
      // TODO: load from classpath
      val indexes = schema.toMessageIndexes(ProtoTypeDescriptor.getMessageDescriptor(record).fullName)
      out.write(indexes.toByteArray())
      record.encode(out)
      val bytes: ByteArray = out.toByteArray()
      out.close()
      bytes
    } catch (e: IOException) {
      throw SerializationException("Error serializing Protobuf message", e)
    } catch (e: RuntimeException) {
      throw SerializationException("Error serializing Protobuf message", e)
    } catch (e: RestClientException) {
      throw toKafkaException(e, restClientErrorMsg + schema)
    }
  }

  /**
   * Resolve schema dependencies recursively.
   *
   * @param schemaRegistry     schema registry client
   * @param autoRegisterSchema whether to automatically register schemas
   * @param useLatestVersion   whether to use the latest schema version for serialization
   * @param latestCompatStrict whether to check that the latest schema version is backward
   * compatible with the schema of the object
   * @param latestVersions     an optional cache of latest schema versions, may be null
   * @param strategy           the strategy for determining the subject name for a reference
   * @param topic              the topic
   * @param isKey              whether the object is the record key
   * @param schema             the schema
   * @return the schema with resolved dependencies
   */
  @Throws(
    IOException::class,
    RestClientException::class
  ) private fun resolveDependencies(
    schemaRegistry: SchemaRegistryClient?,
    autoRegisterSchema: Boolean,
    useLatestVersion: Boolean,
    latestCompatStrict: Boolean,
    latestVersions: Cache<SubjectSchema, ParsedSchema>,
    strategy: ReferenceSubjectNameStrategy?,
    topic: String?,
    isKey: Boolean,
    schema: ProtobufSchema
  ): ProtobufSchema? {
    if (schema.dependencies().isEmpty() || !schema.references().isEmpty()) {
      // Dependencies already resolved
      return schema
    }
    val s = resolveDependencies(
      schemaRegistry!!,
      autoRegisterSchema,
      useLatestVersion,
      latestCompatStrict,
      latestVersions,
      strategy!!,
      topic!!,
      isKey,
      null,
      schema.rawSchema(),
      schema.dependencies()
    )
    return schema.copy(s.references)
  }

  @Throws(
    IOException::class,
    RestClientException::class
  ) private fun resolveDependencies(
    schemaRegistry: SchemaRegistryClient,
    autoRegisterSchema: Boolean,
    useLatestVersion: Boolean,
    latestCompatStrict: Boolean,
    latestVersions: Cache<SubjectSchema, ParsedSchema>,
    strategy: ReferenceSubjectNameStrategy,
    topic: String,
    isKey: Boolean,
    name: String?,
    protoFileElement: ProtoFileElement?,
    dependencies: Map<String, ProtoFileElement>
  ): Schema {
    val references: MutableList<SchemaReference> = ArrayList()
    for (dep in protoFileElement!!.imports) {
      val subschema: Schema = resolveDependencies(
        schemaRegistry,
        autoRegisterSchema,
        useLatestVersion,
        latestCompatStrict,
        latestVersions,
        strategy,
        topic,
        isKey,
        dep,
        dependencies[dep],
        dependencies
      )
      references.add(SchemaReference(dep, subschema.getSubject(), subschema.getVersion()))
    }
    for (dep in protoFileElement.publicImports) {
      val subschema: Schema = resolveDependencies(
        schemaRegistry,
        autoRegisterSchema,
        useLatestVersion,
        latestCompatStrict,
        latestVersions,
        strategy,
        topic,
        isKey,
        dep,
        dependencies[dep],
        dependencies
      )
      references.add(SchemaReference(dep, subschema.getSubject(), subschema.getVersion()))
    }
    var schema = ProtobufSchema(protoFileElement, references, dependencies)
    var id: Int? = null
    var version: Int? = null
    val subject: String? =
      if (name != null) strategy.subjectName(name, topic, isKey, schema) else null
    if (subject != null) {
      if (autoRegisterSchema) {
        id = schemaRegistry.register(subject, schema)
      } else if (useLatestVersion) {
        schema = lookupLatestVersion(
          schemaRegistry, subject, schema, latestVersions, latestCompatStrict
        ) as ProtobufSchema
        id = schemaRegistry.getId(subject, schema)
      } else {
        id = schemaRegistry.getId(subject, schema)
      }
      version = schemaRegistry.getVersion(subject, schema)
    }
    return Schema(
      subject,
      version,
      id,
      schema.schemaType(),
      schema.references(),
      schema.canonicalString()
    )
  }

  companion object {
    const val DEFAULT_CACHE_CAPACITY = 1000
  }
}