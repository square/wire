/*
 * Copyright 2023 Block Inc.
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
package com.squareup.wire.protocwire

import com.squareup.wire.protocwire.Plugin.DescriptorSource
import com.google.protobuf.compiler.PluginProtos

/**
 * CodeGenerator interface used by `Plugin.run()` to generate code.
 * This would be implemented for each Target type (one-to-one relationship). 
 * Since Wire has an  internal ProtoFile representation, each Target can 
 * rely on the same underlying file descriptor to ProtoFile translation.
 */
interface CodeGenerator {
    /**
     * Generates code for the given proto file, generating one or more files to
     * the given response. Called by `Plugin`to pass a parsed proto file through
     * as a request and writes to disk the files in the response upon method return.
     * 
     * @params request the code generator request that contains the file descriptor set for the protoc input.
     * @params descriptorSource the descriptor source for the protoc input.
     * @params response the response of the generator which contains the serialized file to generate.
     */
    fun generate(
        request: PluginProtos.CodeGeneratorRequest,
        descriptorSource: DescriptorSource,
        response: Plugin.Response
    )
}
