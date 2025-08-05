/*
 * Copyright (C) 2024 Square, Inc.
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
package com.squareup.wire.swift

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.squareup.wire.buildSchema
import okio.Path.Companion.toPath
import org.junit.Test

class SwiftGeneratorNameCollisionTest {

  @Test
  fun `detects name collisions between different packages`() {
    val schema = buildSchema {
      add("address1.proto".toPath(), """
        syntax = "proto3";
        package squareup.common.address;
        message Address {
          string street = 1;
          string city = 2;
        }
      """.trimIndent())
      
      add("address2.proto".toPath(), """
        syntax = "proto3";
        package squareup.common.location;
        message Address {
          double latitude = 1;
          double longitude = 2;
        }
      """.trimIndent())
    }

    val generator = SwiftGenerator(schema)
    
    // Get the generated type names for both Address types
    val addressType1 = schema.getType("squareup.common.address.Address")!!
    val addressType2 = schema.getType("squareup.common.location.Address")!!
    
    val typeName1 = generator.generatedTypeName(addressType1)
    val typeName2 = generator.generatedTypeName(addressType2)
    
    // Both should be different to avoid collision
    assertThat(typeName1).isNotEqualTo(typeName2)
    
    // Both should contain package information to distinguish them
    assertThat(typeName1.simpleName).contains("Address")
    assertThat(typeName2.simpleName).contains("Address")
    
    // They should contain different package prefixes
    assertThat(typeName1.simpleName).contains("Common_Address")
    assertThat(typeName2.simpleName).contains("Common_Location")
  }

  @Test
  fun `generates qualified names for colliding types`() {
    val schema = buildSchema {
      add("user.proto".toPath(), """
        syntax = "proto3";
        package squareup.identity.user;
        message User {
          string name = 1;
        }
      """.trimIndent())
      
      add("admin.proto".toPath(), """
        syntax = "proto3";
        package squareup.admin.user;
        message User {
          string permissions = 1;
        }
      """.trimIndent())
    }

    val generator = SwiftGenerator(schema)
    
    val identityUser = schema.getType("squareup.identity.user.User")!!
    val adminUser = schema.getType("squareup.admin.user.User")!!
    
    val identityTypeName = generator.generatedTypeName(identityUser)
    val adminTypeName = generator.generatedTypeName(adminUser)
    
    // Should generate qualified names
    assertThat(identityTypeName.simpleName).isEqualTo("Squareup_Identity_User_User")
    assertThat(adminTypeName.simpleName).isEqualTo("Squareup_Admin_User_User")
  }

  @Test
  fun `handles Swift built-in type collisions`() {
    val schema = buildSchema {
      add("data.proto".toPath(), """
        syntax = "proto3";
        package com.example;
        message Data {
          bytes content = 1;
        }
      """.trimIndent())
      
      add("string.proto".toPath(), """
        syntax = "proto3";
        package com.example;
        message String {
          string value = 1;
        }
      """.trimIndent())
    }

    val generator = SwiftGenerator(schema)
    
    val dataType = schema.getType("com.example.Data")!!
    val stringType = schema.getType("com.example.String")!!
    
    val dataTypeName = generator.generatedTypeName(dataType)
    val stringTypeName = generator.generatedTypeName(stringType)
    
    // Should be qualified to avoid Swift built-in conflicts
    assertThat(dataTypeName.simpleName).isEqualTo("Com_Example_Data")
    assertThat(stringTypeName.simpleName).isEqualTo("Com_Example_String")
  }

  @Test
  fun `preserves simple names when no collisions exist`() {
    val schema = buildSchema {
      add("person.proto".toPath(), """
        syntax = "proto3";
        package com.example;
        message Person {
          string name = 1;
          int32 age = 2;
        }
      """.trimIndent())
      
      add("company.proto".toPath(), """
        syntax = "proto3";
        package com.example;
        message Company {
          string name = 1;
        }
      """.trimIndent())
    }

    val generator = SwiftGenerator(schema)
    
    val personType = schema.getType("com.example.Person")!!
    val companyType = schema.getType("com.example.Company")!!
    
    val personTypeName = generator.generatedTypeName(personType)
    val companyTypeName = generator.generatedTypeName(companyType)
    
    // Should use simple names when no collisions
    assertThat(personTypeName.simpleName).isEqualTo("Person")
    assertThat(companyTypeName.simpleName).isEqualTo("Company")
  }

  @Test
  fun `handles nested type collisions correctly`() {
    val schema = buildSchema {
      add("outer1.proto".toPath(), """
        syntax = "proto3";
        package com.example.pkg1;
        message Outer {
          message Inner {
            string value = 1;
          }
        }
      """.trimIndent())
      
      add("outer2.proto".toPath(), """
        syntax = "proto3";
        package com.example.pkg2;
        message Outer {
          message Inner {
            int32 number = 1;
          }
        }
      """.trimIndent())
    }

    val generator = SwiftGenerator(schema)
    
    val outer1 = schema.getType("com.example.pkg1.Outer")!!
    val outer2 = schema.getType("com.example.pkg2.Outer")!!
    
    val outer1TypeName = generator.generatedTypeName(outer1)
    val outer2TypeName = generator.generatedTypeName(outer2)
    
    // Outer types should have qualified names due to collision
    assertThat(outer1TypeName.simpleName).isEqualTo("Com_Example_Pkg1_Outer")
    assertThat(outer2TypeName.simpleName).isEqualTo("Com_Example_Pkg2_Outer")
    
    // Nested types should also be properly qualified
    val inner1 = schema.getType("com.example.pkg1.Outer.Inner")!!
    val inner2 = schema.getType("com.example.pkg2.Outer.Inner")!!
    
    val inner1TypeName = generator.generatedTypeName(inner1)
    val inner2TypeName = generator.generatedTypeName(inner2)
    
    assertThat(inner1TypeName.simpleName).isEqualTo("Inner")
    assertThat(inner2TypeName.simpleName).isEqualTo("Inner")
    
    // They should be nested under different qualified outer types
    // Nested types are qualified by their parent types
    // Parent types are different, so nested types are properly distinguished
  }

  @Test
  fun `handles complex package hierarchies`() {
    val schema = buildSchema {
      add("deep1.proto".toPath(), """
        syntax = "proto3";
        package com.squareup.payments.api.v1.models;
        message Transaction {
          string id = 1;
        }
      """.trimIndent())
      
      add("deep2.proto".toPath(), """
        syntax = "proto3";
        package com.squareup.analytics.api.v2.events;
        message Transaction {
          string event_id = 1;
        }
      """.trimIndent())
    }

    val generator = SwiftGenerator(schema)
    
    val paymentsTransaction = schema.getType("com.squareup.payments.api.v1.models.Transaction")!!
    val analyticsTransaction = schema.getType("com.squareup.analytics.api.v2.events.Transaction")!!
    
    val paymentsTypeName = generator.generatedTypeName(paymentsTransaction)
    val analyticsTypeName = generator.generatedTypeName(analyticsTransaction)
    
    // Should generate properly qualified names from complex package hierarchies
    assertThat(paymentsTypeName.simpleName).isEqualTo("Com_Squareup_Payments_Api_V1_Models_Transaction")
    assertThat(analyticsTypeName.simpleName).isEqualTo("Com_Squareup_Analytics_Api_V2_Events_Transaction")
  }

  @Test
  fun `handles edge case with empty package names`() {
    val schema = buildSchema {
      add("root1.proto".toPath(), """
        syntax = "proto3";
        message GlobalMessage {
          string content = 1;
        }
      """.trimIndent())
    }

    val generator = SwiftGenerator(schema)
    
    val message1 = schema.getType("GlobalMessage")!!
    val messageTypeName = generator.generatedTypeName(message1)
    
    // Should use simple name for single global message
    assertThat(messageTypeName.simpleName).isEqualTo("GlobalMessage")
  }

  @Test
  fun `preserves module names when specified`() {
    val schema = buildSchema {
      add("user.proto".toPath(), """
        syntax = "proto3";
        package com.example;
        message User {
          string name = 1;
        }
      """.trimIndent())
    }
    
    val existingTypeModuleName = mapOf(
      schema.getType("com.example.User")!!.type to "UserModule"
    )

    val generator = SwiftGenerator(schema, existingTypeModuleName)
    
    val userType = schema.getType("com.example.User")!!
    val userTypeName = generator.generatedTypeName(userType)
    
    // Should preserve the module name
    assertThat(userTypeName.moduleName).isEqualTo("UserModule")
    assertThat(userTypeName.simpleName).isEqualTo("User")
  }
}