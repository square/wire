// Copyright 2022-2023 Buf Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.squareup.wire.protocwire

import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Front-end for protoc code generator plugins written in Kotlin (translated from Java).
 *
 *
 * To implement a protoc plugin in Kotlin, simply write an implementation of
 * [CodeGenerator], then create a main() method like:
 *
 * <pre>`
 * @JvmStatic
 * fun main(args: Array<String>) {
 *   Plugin.run(Generator())
 * }
`</pre> *
 * To get protoc to use the plugin, you'll have to create a launcher script:
 * <pre>`
 * #!/bin/sh
 * cd `dirname $0`
 * exec java -jar myplugin.jar
`</pre> *
 * On Windows, if it lives in the same folder as the plugin's JAR, it will
 * probably look like:
 * <pre>`
 * @echo off
 * java -jar "%~dp0\myplugin.jar"
 * exit %errorlevel%
`</pre> *
 * You'll then have to do one of the following:
 *
 *  * Place the plugin binary somewhere in the `PATH` and give it the
 * name `protoc-gen-NAME` (replacing `NAME` with the name of
 * your plugin). If you then invoke protoc with the parameter
 * `--NAME_out=OUT_DIR` (again, replace `NAME` with your
 * plugin's name), protoc will invoke your plugin to generate the output,
 * which will be placed in `OUT_DIR`.
 *  * Place the plugin binary anywhere, with any name, and pass the
 * `--plugin` parameter to protoc to direct it to your plugin like
 * so:
 * <pre>
 * protoc --plugin=protoc-gen-NAME=path/to/myscript --NAME_out=OUT_DIR
</pre> *
 * On Windows, make sure to include the `.bat` suffix:
 * <pre>
 * protoc --plugin=protoc-gen-NAME=path/to/myscript.bat --NAME_out=OUT_DIR
</pre> *
 *
 *
 * @author t.broyer@ltgt.net Thomas Broyer
 * <br></br>Based on the initial work of:
 * @author kenton@google.com Kenton Varda
 */
object Plugin {
    /**
     * Runs the given code generator, reading the request from [System.in]
     * and writing the response to [System.out].
     *
     * @see .run
     */
    @JvmOverloads
    fun run(generator: CodeGenerator, environment: Environment = DefaultEnvironment()) {
        var request: PluginProtos.CodeGeneratorRequest
        val rawRequest: ByteString
        try {
            rawRequest = ByteString.readFrom(environment.inputStream())
            request = PluginProtos.CodeGeneratorRequest.parseFrom(rawRequest)
        } catch (e: IOException) {
            throw PluginException("protoc sent unparseable request to plugin.", e)
        }
        var files = asDescriptors(request.protoFileList)
        val reg = createExtensionRegistry(files.values)

        // now we must *re-parse* the request, but this time we can properly parse any
        // custom options therein
        request = try {
            PluginProtos.CodeGeneratorRequest.parseFrom(rawRequest, reg)
        } catch (e: IOException) {
            throw PluginException("protoc sent unparseable request to plugin.", e)
        }
        files = asDescriptors(request.protoFileList)
        val output = CodedOutputStream.newInstance(
            environment.outputStream()
        )
        try {
            // go ahead and write response preamble
            PluginProtos.CodeGeneratorResponse
                .newBuilder() // add more here as more features are introduced and then supported
                .setSupportedFeatures(
                    toFeatureBitmask(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL)
                )
                .build()
                .writeTo(output)
        } catch (e: IOException) {
            throw PluginException("protoc sent unparseable request to plugin.", e)
        }
        generator.generate(request, DescriptorSource(files), Response(output))
    }

    private fun toFeatureBitmask(vararg features: PluginProtos.CodeGeneratorResponse.Feature): Long {
        var result: Long = 0
        for (feature in features) {
            result = result or feature.number.toLong()
        }
        return result
    }

    private fun createExtensionRegistry(files: Collection<Descriptors.FileDescriptor>): ExtensionRegistry {
        val registry = ExtensionRegistry.newInstance()
        for (fileDescriptor in files) {
            addAllExtensionsFromFile(registry, fileDescriptor)
        }
        return registry
    }

    private fun addAllExtensionsFromFile(registry: ExtensionRegistry, fileDescriptor: Descriptors.FileDescriptor) {
        for (ext in fileDescriptor.extensions) {
            if (ext.type == Descriptors.FieldDescriptor.Type.MESSAGE) {
                registry.add(ext, DynamicMessage.newBuilder(ext.messageType).build())
            } else {
                registry.add(ext)
            }
        }
        for (message in fileDescriptor.messageTypes) {
            addAllExtensionsFromMessage(registry, message)
        }
    }

    private fun addAllExtensionsFromMessage(
        registry: ExtensionRegistry,
        message: Descriptors.Descriptor
    ) {
        for (ext in message.extensions) {
            if (ext.type == Descriptors.FieldDescriptor.Type.MESSAGE) {
                registry.add(ext, DynamicMessage.newBuilder(ext.messageType).build())
            } else {
                registry.add(ext)
            }
        }
        for (nested in message.nestedTypes) {
            addAllExtensionsFromMessage(registry, nested)
        }
    }

    /**
     * Parse the request's proto files and returns the list of parsed descriptors
     * corresponding only to the files to generate (i.e. dependencies not listed
     * explicitly are not included in the returned list).
     */
    private fun asDescriptors(protoFiles: List<DescriptorProtos.FileDescriptorProto>): Map<String, Descriptors.FileDescriptor> {
        val filesByName: MutableMap<String, Descriptors.FileDescriptor> = HashMap(protoFiles.size)
        for (protoFile in protoFiles) {
            val dependencies = arrayOfNulls<Descriptors.FileDescriptor>(protoFile.dependencyCount)
            var i = 0
            val l = protoFile.dependencyCount
            while (i < l) {
                val dependency = filesByName[protoFile.getDependency(i)]
                    ?: throw PluginException(
                        "protoc asked plugin to generate a file " +
                            "but did not provide a descriptor for a dependency (or " +
                            "provided it after the file that depends on it): ${protoFile.getDependency(i)}"
                    )
                dependencies[i] = dependency
                i++
            }
            try {
                filesByName[protoFile.name] = Descriptors.FileDescriptor.buildFrom(
                    protoFile, dependencies
                )
            } catch (e: Descriptors.DescriptorValidationException) {
                throw PluginException(e)
            }
        }
        return filesByName
    }

    /**
     * Thrown when something went wrong in the plugin infrastructure.
     *
     *
     * This is an unrecoverable error. You shouldn't handle it.
     */
    class PluginException : RuntimeException {
        internal constructor(message: String?) : super(message)
        internal constructor(message: String?, cause: Throwable?) : super(message, cause)
        internal constructor(cause: Throwable?) : super(cause)

        companion object {
            private const val serialVersionUID = 4028115971354639383L
        }
    }

    /**
     * Provides access to the input and output streams used to communicate with
     * protoc.
     *
     * @see DefaultEnvironment
     */
    interface Environment {
        /**
         * Returns the input stream to read the protoc code generation request
         * from.
         */
        fun inputStream(): InputStream

        /**
         * Returns the output stream to write the code generation response to.
         */
        fun outputStream(): OutputStream
    }

    /**
     * An [Environment] giving access to the "standard" input and output
     * streams.
     */
    open class DefaultEnvironment : Environment {
        override fun inputStream(): InputStream = System.`in`
        override fun outputStream(): OutputStream = System.out
    }

    class DescriptorSource(private val files: Map<String, Descriptors.FileDescriptor>) {
        fun findMessageTypeByName(fullName: String): Descriptors.Descriptor? {
            for (fileDescriptor in files.values) {
                return fileDescriptor.findMessageTypeByName(fullName) ?: continue
            }
            return null
        }
    }

    /**
     * Streams generated files from a [CodeGenerator] to a [CodedOutputStream].
     */
    class Response internal constructor(private val output: CodedOutputStream) {
        fun addFile(filename: String, content: String) {
            val file = PluginProtos.CodeGeneratorResponse.File.newBuilder()
            file.name = filename
            file.content = content
            try {
                // Protocol format guarantees that concatenated messages are parsed as
                // if they had been merged in a single message prior to being serialized.
                PluginProtos.CodeGeneratorResponse.newBuilder().addFile(file).build().writeTo(
                    output
                )
                output.flush()
            } catch (e: IOException) {
                throw PluginException("Error writing to stdout.", e)
            }
        }
    }
}
