/*
 * Copyright 2021 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.internal.SchemaEncoder
import grpc.reflection.v1alpha.ErrorResponse
import grpc.reflection.v1alpha.FileDescriptorResponse
import grpc.reflection.v1alpha.ListServiceResponse
import grpc.reflection.v1alpha.ServerReflectionRequest
import grpc.reflection.v1alpha.ServerReflectionResponse
import grpc.reflection.v1alpha.ServiceResponse
import okio.ByteString

/**
 * This converts a Wire Schema model to a protobuf DescriptorProtos model and serves that.
 */
class SchemaReflector(
  private val schema: Schema
) {
  fun process(request: ServerReflectionRequest): ServerReflectionResponse {
    val response = when {
      request.list_services != null -> listServices()
      request.file_by_filename != null -> fileByFilename(request)
      request.file_containing_symbol != null -> fileContainingSymbol(request.file_containing_symbol)
      //TODO: request.file_containing_extension request.all_extension_numbers_of_type
      else -> {
        ServerReflectionResponse(
          error_response = ErrorResponse(
            error_code = GrpcStatus.INVALID_ARGUMENT.code,
            "unsupported request"
          )
        )
      }
    }
    return response.copy(
      valid_host = request.host,
      original_request = request
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
        service = allServiceNames.sortedBy { it.name }
      )
    )
  }

  private fun fileByFilename(request: ServerReflectionRequest): ServerReflectionResponse {
    val protoFile = schema.protoFile(request.file_by_filename!!)!!

    return ServerReflectionResponse(
      file_descriptor_response = SchemaEncoder(schema).encode(protoFile).toFileDescriptorResponse()
    )
  }

  private fun fileContainingSymbol(file_containing_symbol: String): ServerReflectionResponse {
    val symbol = file_containing_symbol.removePrefix(".")

    val service = schema.getService(symbol)

    // TODO(juliaogris): Happy path to the left
    val location: Location
    if (service != null) {
      location = service.location
    } else {
      val type = schema.getType(symbol)
      if (type != null) {
        location = type.location
      } else {
        val fullServiceName = symbol.substringBeforeLast(".")
        val serviceWithMethod = schema.getService(fullServiceName)
        if (serviceWithMethod != null) {
          location = serviceWithMethod.location
        } else {
          return ServerReflectionResponse(
            error_response = ErrorResponse(
              error_code = GrpcStatus.NOT_FOUND.code,
              "unknown symbol: $file_containing_symbol"
            )
          )
        }
      }
    }

    val protoFile = schema.protoFile(location.path)!!
    val result = SchemaEncoder(schema).encode(protoFile).toFileDescriptorResponse()
    return ServerReflectionResponse(
      file_descriptor_response = result
    )
  }

  private fun ByteString.toFileDescriptorResponse(): FileDescriptorResponse {
    return FileDescriptorResponse(
      file_descriptor_proto = listOf(this)
    )
  }
}
