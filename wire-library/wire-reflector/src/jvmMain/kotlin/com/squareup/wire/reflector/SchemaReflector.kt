package com.squareup.wire.reflector

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import grpc.reflection.v1alpha.FileDescriptorResponse
import grpc.reflection.v1alpha.ListServiceResponse
import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse
import grpc.reflection.v1alpha.ServiceResponse
import okio.ByteString.Companion.toByteString

/**
 * This converts a Wire Schema model to a protobuf DescriptorProtos model and serves that.
 */
class SchemaReflector(
  private val schema: Schema
) {
  fun process(request: ServerReflectionRequest): ServerReflectionResponse {
    when {
      request.list_services == "*" -> {
        val allServiceNames = mutableListOf<ServiceResponse>()
        for (protoFile in schema.protoFiles) {
          for (service in protoFile.services) {
            val packagePrefix = when {
              protoFile.packageName != null -> protoFile.packageName + "."
              else -> ""
            }
            val serviceName = packagePrefix + service.name
            allServiceNames += ServiceResponse.Builder()
              .name(serviceName)
              .build()
          }
        }

        return ServerReflectionResponse.Builder()
          .list_services_response(
            ListServiceResponse.Builder()
              .service(allServiceNames)
              .build()
          )
          .build()

      }

      request.file_by_filename != null -> {
        val protoFile = schema.protoFile(request.file_by_filename)!!
        return ServerReflectionResponse.Builder()
          .file_descriptor_response(protoFile.toFileDescriptorResponse())
          .build()

      }

      request.file_containing_symbol != null -> {

        val symbol = request.file_containing_symbol.removePrefix(".")

        val service = schema.getService(symbol)

        val location: Location
        if (service != null) {
          location = service.location
        } else {
          val type = schema.getType(symbol)
          if (type != null) {
            location = type.location
          } else {
            val method = symbol.substringAfterLast(".")
            val fullServiceName = symbol.substringBeforeLast(".")
            val serviceWithMethod = schema.getService(fullServiceName)
            if (serviceWithMethod != null) {
              location = serviceWithMethod.location
            } else {
              error("TODO: fail the call somehow?")
            }
          }
        }

        val protoFile = schema.protoFile(location.path)!!
        val result = protoFile.toFileDescriptorResponse()
        return ServerReflectionResponse.Builder()
          .file_descriptor_response(result)
          .build()

      }

      else -> {
        // TODO.
        println(request)
        return ServerReflectionResponse.Builder()
          .build()
      }
    }
  }


  private fun ProtoFile.toFileDescriptorResponse(): FileDescriptorResponse {
    val fileDescriptor = toFileDescriptor()
    return FileDescriptorResponse.Builder()
      .file_descriptor_proto(listOf(fileDescriptor.toByteArray().toByteString()))
      .build()
  }

  private fun ProtoFile.toFileDescriptor(): FileDescriptorProto {
    val result = FileDescriptorProto.newBuilder()
      .setName(this.location.path)
      .setPackage(this.packageName ?: "")

    for (service in services) {
      result.addService(service.toServiceDescriptorProto())
    }

    for (type in types) {
      if (type is MessageType) {
        result.addMessageType(type.toDescriptorProto())
      } else {
        // TODO.
      }
    }

    return result.build()
  }

  private fun Type.toDescriptorProto(): DescriptorProto {
    val result = DescriptorProto.newBuilder()
      .setName(type.simpleName)

    if (this is MessageType) {
      for (field in declaredFields) {
        val fieldDescriptorProto = field.toFieldDescriptorProto(type)
        result.addField(fieldDescriptorProto)
        if (field.type!!.isMap) {
          result.addNestedType(field.type!!.toMapType("${field.name}_entry"))
        }
      }
    }

    for (nested in this.nestedTypes) {
      result.addNestedType(nested.toDescriptorProto())
    }

    return result.build()
  }

  private fun Field.toFieldDescriptorProto(enclosingType: ProtoType): FieldDescriptorProto {
    val label = when {
      isRepeated || type!!.isMap -> FieldDescriptorProto.Label.LABEL_REPEATED
      isRequired -> FieldDescriptorProto.Label.LABEL_REQUIRED
      else -> FieldDescriptorProto.Label.LABEL_OPTIONAL
    }

    val typeName = if (type!!.isMap) {
      // TODO: case map "happy_map_entry" to "HappyMapEntry"
      enclosingType.nestedType("${name}_entry").protoTypeName()
    } else {
      type!!.protoTypeName()
    }

    return FieldDescriptorProto.newBuilder()
      .setName(name)
      .apply {
        if (typeName != null) {
          setTypeName(typeName)
        }
      }
      .setNumber(tag)
      .setType(type!!.toFieldDescriptorType())
      .setLabel(label)
      .build()
  }

  /**
   * Returns null for scalars like int32 because we don't have names for these.
   */
  private fun ProtoType.protoTypeName(): String? {
    return if (this.isScalar) null else ".$this"
  }

  /**
   * We synthesize a type for the map entry.
   */
  private fun ProtoType.toMapType(name: String): DescriptorProto {
    return DescriptorProto.newBuilder()
      .setName(name)
      .addField(
        FieldDescriptorProto.newBuilder()
          .setName("key")
          .apply {
            val type = keyType!!.protoTypeName()
            if (type != null) {
              setTypeName(type)
            }
          }
          .setNumber(1)
          .setType(keyType!!.toFieldDescriptorType())
          .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
          .build()
      )
      .addField(
        FieldDescriptorProto.newBuilder()
          .setName("value")
          .apply {
            val type = valueType!!.protoTypeName()
            if (type != null) {
              setTypeName(type)
            }
          }
          .setNumber(2)
          .setType(valueType!!.toFieldDescriptorType())
          .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
          .build()
      )
      .setOptions(DescriptorProtos.MessageOptions.newBuilder()
        .setMapEntry(true)
        .build())
      .build()
  }

  private fun ProtoType.toFieldDescriptorType(): FieldDescriptorProto.Type {
    when (this) {
      ProtoType.BOOL -> return FieldDescriptorProto.Type.TYPE_BOOL
      ProtoType.BYTES -> return FieldDescriptorProto.Type.TYPE_BYTES
      ProtoType.DOUBLE -> return FieldDescriptorProto.Type.TYPE_DOUBLE
      ProtoType.FLOAT -> return FieldDescriptorProto.Type.TYPE_FLOAT
      ProtoType.FIXED32 -> return FieldDescriptorProto.Type.TYPE_FIXED32
      ProtoType.FIXED64 -> return FieldDescriptorProto.Type.TYPE_FIXED64
      ProtoType.INT32 -> return FieldDescriptorProto.Type.TYPE_INT32
      ProtoType.INT64 -> return FieldDescriptorProto.Type.TYPE_INT64
      ProtoType.SFIXED32 -> return FieldDescriptorProto.Type.TYPE_SFIXED32
      ProtoType.SFIXED64 -> return FieldDescriptorProto.Type.TYPE_SFIXED64
      ProtoType.SINT32 -> return FieldDescriptorProto.Type.TYPE_SINT32
      ProtoType.SINT64 -> return FieldDescriptorProto.Type.TYPE_SINT64
      ProtoType.STRING -> return FieldDescriptorProto.Type.TYPE_STRING
      ProtoType.UINT32 -> return FieldDescriptorProto.Type.TYPE_UINT32
      ProtoType.UINT64 -> return FieldDescriptorProto.Type.TYPE_UINT64
    }
    val type = schema.getType(this)
    if (type is EnumType) return FieldDescriptorProto.Type.TYPE_ENUM
    return FieldDescriptorProto.Type.TYPE_MESSAGE
  }

  private fun Service.toServiceDescriptorProto(): ServiceDescriptorProto {
    val result = ServiceDescriptorProto.newBuilder()
      .setName(name)

    for (rpc in rpcs) {
      result.addMethod(rpc.toMethodDescriptorProto())
    }

    return result.build()
  }

  private fun Rpc.toMethodDescriptorProto(): MethodDescriptorProto {
    return MethodDescriptorProto.newBuilder()
      .setName(name)
      .setInputType(requestType!!.protoTypeName())
      .setOutputType(responseType!!.protoTypeName())
      .build()
  }
}
