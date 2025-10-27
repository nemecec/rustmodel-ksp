/*
 * Copyright (C) 2025 Neeme Praks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nemecec.rustmodel.ksp

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RustGeneratorProcessorTest {

  @TempDir
  lateinit var tempDir: File

  @Test
  fun `test simple data class generation`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "User.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class User(
                val id: Int,
                val name: String,
                val email: String
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub struct User")
    assertThat(content).contains("pub id: i32")
    assertThat(content).contains("pub name: String")
    assertThat(content).contains("pub email: String")
    assertThat(content).contains("#[derive(Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize)]")
  }

  @Test
  fun `test data class with nullable fields`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Profile.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Profile(
                val userId: Int,
                val bio: String?,
                val avatar: String?
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub user_id: i32")
    assertThat(content).contains("pub bio: Option<String>")
    assertThat(content).contains("pub avatar: Option<String>")
    assertThat(content).contains("#[serde(skip_serializing_if = \"Option::is_none\")]")
  }

  @Test
  fun `test data class with collections`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Container.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Container(
                val items: List<String>,
                val tags: Set<String>,
                val metadata: Map<String, String>
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub items: Vec<String>")
    assertThat(content).contains("pub tags: std::collections::HashSet<String>")
    assertThat(content).contains("pub metadata: std::collections::HashMap<String, String>")
  }

  @Test
  fun `test enum generation`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Status.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            enum class Status {
                PENDING,
                APPROVED,
                REJECTED
            }
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub enum Status")
    assertThat(content).contains("Pending,")
    assertThat(content).contains("Approved,")
    assertThat(content).contains("Rejected,")
    assertThat(content).contains("#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, serde::Serialize, serde::Deserialize)]")
  }

  @Test
  fun `test sealed class generation`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Result.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            sealed class Result

            @Serializable
            data class Success(val value: String) : Result()

            @Serializable
            data class Failure(val error: String) : Result()
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    // Should generate sealed enum
    assertThat(content).contains("pub enum Result")
    assertThat(content).contains("Success(Success)")
    assertThat(content).contains("Failure(Failure)")
    // Should generate structs for subclasses
    assertThat(content).contains("pub struct Success")
    assertThat(content).contains("pub struct Failure")
    // The generated code uses tagged representation
    assertThat(content).contains("#[serde(tag = \"type\")]")
  }

  @Test
  fun `test SerialName annotation`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Entity.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable
            import kotlinx.serialization.SerialName

            @Serializable
            data class Entity(
                @SerialName("entity_id")
                val id: Int,
                @SerialName("entity_name")
                val name: String
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("#[serde(rename = \"entity_id\")]")
    assertThat(content).contains("#[serde(rename = \"entity_name\")]")
    assertThat(content).contains("pub id: i32")
    assertThat(content).contains("pub name: String")
  }

  @Test
  fun `test multiple classes in same file`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Models.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Person(val name: String, val age: Int)

            @Serializable
            data class Address(val street: String, val city: String)
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub struct Person")
    assertThat(content).contains("pub struct Address")
  }

  @Test
  fun `test numeric types mapping`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Numbers.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Numbers(
                val byteVal: Byte,
                val shortVal: Short,
                val intVal: Int,
                val longVal: Long,
                val floatVal: Float,
                val doubleVal: Double,
                val ubyteVal: UByte,
                val ushortVal: UShort,
                val uintVal: UInt,
                val ulongVal: ULong
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub byte_val: i8")
    assertThat(content).contains("pub short_val: i16")
    assertThat(content).contains("pub int_val: i32")
    assertThat(content).contains("pub long_val: i64")
    assertThat(content).contains("pub float_val: f32")
    assertThat(content).contains("pub double_val: f64")
    assertThat(content).contains("pub ubyte_val: u8")
    assertThat(content).contains("pub ushort_val: u16")
    assertThat(content).contains("pub uint_val: u32")
    assertThat(content).contains("pub ulong_val: u64")
  }

  @Test
  fun `test nested generic types`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Nested.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Nested(
                val listOfLists: List<List<String>>,
                val mapOfLists: Map<String, List<Int>>,
                val optionalList: List<String>?
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub list_of_lists: Vec<Vec<String>>")
    assertThat(content).contains("pub map_of_lists: std::collections::HashMap<String, Vec<i32>>")
    assertThat(content).contains("pub optional_list: Option<Vec<String>>")
  }

  @Test
  fun `test custom type references`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Custom.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Address(val street: String, val city: String)

            @Serializable
            data class Person(
                val name: String,
                val address: Address,
                val alternateAddresses: List<Address>
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub address: Address")
    assertThat(content).contains("pub alternate_addresses: Vec<Address>")
  }

  @Test
  fun `test file header generation`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Test.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Test(val value: String)
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("//! Auto-generated Rust module from Kotlin sources")
    assertThat(content).contains("//! Module: Test")
    assertThat(content).contains("use serde::{Deserialize, Serialize};")
    assertThat(content).contains("use std::collections::{HashMap, HashSet};")
  }

  @Test
  fun `test camelCase to snake_case conversion`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "CamelCase.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class CamelCaseTest(
                val firstName: String,
                val lastName: String,
                val emailAddress: String,
                val phoneNumber: String
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub first_name: String")
    assertThat(content).contains("pub last_name: String")
    assertThat(content).contains("pub email_address: String")
    assertThat(content).contains("pub phone_number: String")
  }

  @Test
  fun `test filter by package`() {
    val rustFiles = generateSources(
      listOf(
        KotlinSourceFile(
          "Included.kt",
          """
            package com.example.models

            import kotlinx.serialization.Serializable

            @Serializable
            data class Included(val value: String)
            """.trimIndent()
        ),
        KotlinSourceFile(
          "Excluded.kt",
          """
            package com.other

            import kotlinx.serialization.Serializable

            @Serializable
            data class Excluded(val value: String)
            """.trimIndent()
        )
      ),
      mapOf("rust.filter.packages" to "com.example")
    )

    assertThat(rustFiles[0].exists()).isTrue()
    assertThat(rustFiles[1].exists()).isFalse()
  }

  @Test
  fun `test filter by file`() {
    val rustFiles = generateSources(
      listOf(
        KotlinSourceFile(
          "Included.kt",
          """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Included(val value: String)
            """.trimIndent()
        ),
        KotlinSourceFile(
          "Excluded.kt",
          """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Excluded(val value: String)
            """.trimIndent()
        )
      ),
      mapOf("rust.filter.files" to "Included.kt")
    )

    assertThat(rustFiles[0].exists()).isTrue()
    assertThat(rustFiles[1].exists()).isFalse()
  }

  @Test
  fun `test process multiple files`() {
    val rustFiles = generateSources(
      listOf(
        KotlinSourceFile(
          "Test.kt",
          """
            package com.anything

            import kotlinx.serialization.Serializable

            @Serializable
            data class Test(val value: String)
            """.trimIndent()
        ),
        KotlinSourceFile(
          "Test2.kt",
          """
            package com.anything

            import kotlinx.serialization.Serializable

            @Serializable
            data class Test2(val value: String)
            """.trimIndent()
        ),
      )
    )

    assertThat(rustFiles[0].exists()).isTrue()
    assertThat(rustFiles[1].exists()).isTrue()
    assertThat(rustFiles.size).isEqualTo(2)
  }

  @Test
  fun `test boolean type mapping`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "Flags.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            @Serializable
            data class Flags(
                val isActive: Boolean,
                val isEnabled: Boolean?
            )
            """.trimIndent()
      )
    )

    val content = rustFile.readText()
    assertThat(content).contains("pub is_active: bool")
    assertThat(content).contains("pub is_enabled: Option<bool>")
  }

  @Test
  fun `test custom serialNameAnnotation configuration`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "CustomAnnotation.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            annotation class CustomSerialName(val value: String)

            @Serializable
            data class CustomEntity(
                @CustomSerialName("custom_id")
                val id: Int,
                @CustomSerialName("custom_name")
                val name: String
            )
            """.trimIndent()
      ),
      additionalKspArgs = mapOf("rust.serialNameAnnotation" to "com.example.CustomSerialName.value")
    )

    val content = rustFile.readText()
    assertThat(content).contains("#[serde(rename = \"custom_id\")]")
    assertThat(content).contains("#[serde(rename = \"custom_name\")]")
    assertThat(content).contains("pub id: i32")
    assertThat(content).contains("pub name: String")
  }

  @Test
  fun `test custom serialNameAnnotation with different argument name`() {
    val rustFile = generateSource(
      KotlinSourceFile(
        "CustomAnnotation2.kt",
        """
            package com.example

            import kotlinx.serialization.Serializable

            annotation class AltSerialName(val foo: String)

            @Serializable
            data class AltEntity(
                @AltSerialName(foo = "alt_id")
                val id: Int,
                @AltSerialName(foo = "alt_value")
                val value: String
            )
            """.trimIndent()
      ),
      additionalKspArgs = mapOf("rust.serialNameAnnotation" to "com.example.AltSerialName.foo")
    )

    val content = rustFile.readText()
    assertThat(content).contains("#[serde(rename = \"alt_id\")]")
    assertThat(content).contains("#[serde(rename = \"alt_value\")]")
    assertThat(content).contains("pub id: i32")
    assertThat(content).contains("pub value: String")
  }

  private data class KotlinSourceFile(val name: String, val contents: String)

  private fun generateSource(
    source: KotlinSourceFile,
    additionalKspArgs: Map<String, String> = emptyMap()
  ) = generateSources(listOf(source), additionalKspArgs).single()

  @Test
  fun `test invalid serialNameAnnotation configuration fails`() {
    val sourcesDir = tempDir.resolve("rust-sources").apply {
      mkdirs()
    }
    val compilation = KotlinCompilation().apply {
      sources = listOf(SourceFile.kotlin(
        "Test.kt",
        """
          package com.example

          import kotlinx.serialization.Serializable

          @Serializable
          data class Test(val value: String)
        """.trimIndent()
      ))
      symbolProcessorProviders = listOf(RustGeneratorProcessorProvider())
      inheritClassPath = true
      kspArgs = mutableMapOf(
        "rust.output.dir" to sourcesDir.absolutePath,
        "rust.serialNameAnnotation" to "InvalidFormat"
      )
    }

    // Should fail due to invalid format
    assertThat(compilation.compile().exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
  }

  @Test
  fun `test invalid discriminatorAnnotations configuration fails`() {
    val sourcesDir = tempDir.resolve("rust-sources").apply {
      mkdirs()
    }
    val compilation = KotlinCompilation().apply {
      sources = listOf(SourceFile.kotlin(
        "Test.kt",
        """
          package com.example

          import kotlinx.serialization.Serializable

          @Serializable
          data class Test(val value: String)
        """.trimIndent()
      ))
      symbolProcessorProviders = listOf(RustGeneratorProcessorProvider())
      inheritClassPath = true
      kspArgs = mutableMapOf(
        "rust.output.dir" to sourcesDir.absolutePath,
        "rust.discriminatorAnnotations" to "InvalidFormat,AnotherInvalid"
      )
    }

    // Should fail due to invalid format
    assertThat(compilation.compile().exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
  }

  private fun generateSources(
    kotlinSourceFiles: List<KotlinSourceFile>,
    additionalKspArgs: Map<String, String> = emptyMap()
  ): List<File> {
    val sourcesDir = tempDir.resolve("rust-sources").apply {
      mkdirs()
    }
    val compilation = KotlinCompilation().apply {
      sources = kotlinSourceFiles.map { SourceFile.kotlin(it.name, it.contents) }
      symbolProcessorProviders = listOf(RustGeneratorProcessorProvider())
      inheritClassPath = true
      kspArgs = mutableMapOf(
        "rust.output.dir" to sourcesDir.absolutePath
      ).apply {
        putAll(additionalKspArgs)
      }
    }

    assertThat(compilation.compile().exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    return kotlinSourceFiles.map { sourcesDir.resolve(it.name.toRustSourceFileName()) }
  }
}

private fun String.toRustSourceFileName() =
  TypeMapper.toRustSnakeCaseName(removeSuffix(".kt")) + ".rs"
