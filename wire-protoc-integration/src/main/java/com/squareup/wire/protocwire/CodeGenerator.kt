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

import com.squareup.wire.protocwire.Plugin.DescriptorSource
import com.google.protobuf.compiler.PluginProtos

/**
 * Interface to be implemented by a code generator.
 */
interface CodeGenerator {
    /**
     * Generates code for the given proto file, generating one or more files to
     * the given response.
     */
    fun generate(
        request: PluginProtos.CodeGeneratorRequest,
        descriptorSource: DescriptorSource,
        response: Plugin.Response
    )
}
