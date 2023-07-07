/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.reflector

import com.squareup.wire.GrpcStatus
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.internal.SchemaEncoder
import grpc.reflection.v1alpha.ErrorResponse
import grpc.reflection.v1alpha.ExtensionNumberResponse
import grpc.reflection.v1alpha.ExtensionRequest
import grpc.reflection.v1alpha.FileDescriptorResponse
import grpc.reflection.v1alpha.ListServiceResponse
import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse
import grpc.reflection.v1alpha.ServiceResponse

/**
 * This converts a Wire Schema model to a protobuf DescriptorProtos model and serves that.
 */
class SchemaReflector(
  private val schema: Schema,
  private val includeDependencies: Boolean = true,
) {
  fun process(request: ServerReflectionRequest): ServerReflectionResponse {
    val response = when {
      request.list_services != null -> listServices()
      request.file_by_filename != null -> fileByFilename(request)
      request.file_containing_symbol != null -> fileContainingSymbol(request.file_containing_symbol)
      request.all_extension_numbers_of_type != null -> allExtensionNumbersOfType(
        request.all_extension_numbers_of_type,
      )
      request.file_containing_extension != null -> fileContainingExtension(
        request.file_containing_extension,
      )
      else -> {
        ServerReflectionResponse(
          error_response = ErrorResponse(
            error_code = GrpcStatus.INVALID_ARGUMENT.code,
            "unsupported request",
          ),
        )
      }
    }
    return response.copy(
      valid_host = request.host,
      original_request = request,
    )
  }

  private fun allExtensionNumbersOfType(type: String): ServerReflectionResponse {
    val wireType = schema.getType(type)
    if (wireType == null || wireType !is MessageType) {
      return ServerReflectionResponse(
        error_response = ErrorResponse(
          error_code = GrpcStatus.NOT_FOUND.code,
          error_message = "unknown type: \"$type\"",
        ),
      )
    }

    val extensionNumbers = wireType.extensionFields.map { it.tag }

    return ServerReflectionResponse(
      all_extension_numbers_response = ExtensionNumberResponse(
        base_type_name = type,
        extension_number = extensionNumbers,
      ),
    )
  }

  private fun fileContainingExtension(extension: ExtensionRequest): ServerReflectionResponse {
    val wireType = schema.getType(extension.containing_type)

    if (wireType == null || wireType !is MessageType) {
      return ServerReflectionResponse(
        error_response = ErrorResponse(
          error_code = GrpcStatus.NOT_FOUND.code,
          error_message = "unknown type: \"$extension.containing_type\"",
        ),
      )
    }

    val field = wireType.extensionFields
      .firstOrNull { it.tag == extension.extension_number }
      ?: return ServerReflectionResponse(
        error_response = ErrorResponse(
          error_code = GrpcStatus.NOT_FOUND.code,
          error_message = "unknown type: \"$extension.containing_type\"",
        ),
      )

    val location = field.location

    val protoFile = schema.protoFile(location.path)!!
    val allProtoFiles = allDependencies(protoFile)
    val schemaEncoder = SchemaEncoder(schema)
    return ServerReflectionResponse(
      file_descriptor_response = FileDescriptorResponse(allProtoFiles.map { schemaEncoder.encode(it) }),
    )
  }

  private fun listServices(): ServerReflectionResponse {
    val allServiceNames = mutableListOf<ServiceResponse>()
    for (protoFile in schema.protoFiles) {
      for (service in protoFile.services) {
        val packagePrefix = when {
          protoFile.packageName != null -> protoFile.packageName + "."
          else -> ""
        }
        val serviceName = packagePrefix + service.name
        allServiceNames += ServiceResponse(name = serviceName)
      }
    }

    return ServerReflectionResponse(
      list_services_response = ListServiceResponse(
        service = allServiceNames.sortedBy { it.name },
      ),
    )
  }

  private fun fileByFilename(request: ServerReflectionRequest): ServerReflectionResponse {
    val protoFile = schema.protoFile(request.file_by_filename!!)!!
    val allProtoFiles = allDependencies(protoFile)
    val schemaEncoder = SchemaEncoder(schema)
    return ServerReflectionResponse(
      file_descriptor_response = FileDescriptorResponse(allProtoFiles.map { schemaEncoder.encode(it) }),
    )
  }

  private fun fileContainingSymbol(file_containing_symbol: String): ServerReflectionResponse {
    val symbol = file_containing_symbol.removePrefix(".")

    val location = location(symbol)
      ?: return ServerReflectionResponse(
        error_response = ErrorResponse(
          error_code = GrpcStatus.NOT_FOUND.code,
          "unknown symbol: $file_containing_symbol",
        ),
      )

    val protoFile = schema.protoFile(location.path)!!
    val allProtoFiles = allDependencies(protoFile)
    val schemaEncoder = SchemaEncoder(schema)
    return ServerReflectionResponse(
      file_descriptor_response = FileDescriptorResponse(allProtoFiles.map { schemaEncoder.encode(it) }),
    )
  }

  private fun location(symbol: String): Location? {
    val service = schema.getService(symbol)
    if (service != null) return service.location

    val type = schema.getType(symbol)
    if (type != null) return type.location

    val beforeLastDotName = symbol.substringBeforeLast(".")
    val afterLastDot = symbol.substring(beforeLastDotName.length + 1)

    val serviceWithMethod = schema.getService(beforeLastDotName)
    if (serviceWithMethod?.rpc(afterLastDot) != null) return serviceWithMethod.location

    val extend = schema.protoFiles
      .filter { it.packageName == beforeLastDotName }
      .flatMap { it.extendList }
      .flatMap { it.fields }
      .firstOrNull { it.name == afterLastDot }
    if (extend != null) return extend.location

    return null
  }

  /**
   * Returns [protoFile] and all its transitive dependencies in topographical order such that files
   * always appear before their dependencies.
   */
  private fun allDependencies(protoFile: ProtoFile): List<ProtoFile> {
    if (!includeDependencies) return listOf(protoFile)

    val result = mutableListOf<ProtoFile>()
    collectAllDependencies(protoFile, mutableSetOf(), result)
    return result
  }

  private fun collectAllDependencies(protoFile: ProtoFile, visited: MutableSet<String>, result: MutableList<ProtoFile>) {
    if (visited.add(protoFile.name())) {
      result.add(protoFile)
      protoFile.imports.forEach {
        collectAllDependencies(schema.protoFile(it)!!, visited, result)
      }
      protoFile.publicImports.forEach {
        collectAllDependencies(schema.protoFile(it)!!, visited, result)
      }
    }
  }
}
