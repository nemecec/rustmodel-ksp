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
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProcessorConfigTest {

  @TempDir
  lateinit var tempDir: File

  @Test
  fun `test shouldProcess with no filters processes all`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath
    ).toConfig()

    assertThat(config.shouldProcess("com.example", "Test.kt")).isTrue()
    assertThat(config.shouldProcess("com.other", "Other.kt")).isTrue()
    assertThat(config.shouldProcess(null, null)).isTrue()
  }

  @Test
  fun `test shouldProcess with package filter`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.packages" to "com.example, org.test"
    ).toConfig()

    assertThat(config.shouldProcess("com.example", "Test.kt")).isTrue()
    assertThat(config.shouldProcess("com.example.models", "User.kt")).isTrue()
    assertThat(config.shouldProcess("org.test", "Other.kt")).isTrue()
    assertThat(config.shouldProcess("org.test.util", "Util.kt")).isTrue()
    assertThat(config.shouldProcess("com.other", "Other.kt")).isFalse()
    assertThat(config.shouldProcess("dev.nemecec", "Model.kt")).isFalse()
  }

  @Test
  fun `test shouldProcess with file filter`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.files" to "User.kt, Profile.kt"
    ).toConfig()

    assertThat(config.shouldProcess("com.example", "User.kt")).isTrue()
    assertThat(config.shouldProcess("com.example", "Profile.kt")).isTrue()
    assertThat(config.shouldProcess("com.example", "Other.kt")).isFalse()
    assertThat(config.shouldProcess("com.example", "Model.kt")).isFalse()
  }

  @Test
  fun `test shouldProcess with both package and file filters`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.packages" to "com.example",
      "rust.filter.files" to "User.kt"
    ).toConfig()

    // Should match if either filter matches
    assertThat(config.shouldProcess("com.example", "Other.kt")).isTrue()
    assertThat(config.shouldProcess("com.other", "User.kt")).isTrue()
    assertThat(config.shouldProcess("com.example", "User.kt")).isTrue()
    assertThat(config.shouldProcess("com.other", "Other.kt")).isFalse()
  }

  @Test
  fun `test shouldProcess with empty filters`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath
    ).toConfig()

    // When no filters are set, should process everything
    assertThat(config.shouldProcess("com.example", "Test.kt")).isTrue()
    assertThat(config.shouldProcess("com.other", "Other.kt")).isTrue()
    assertThat(config.shouldProcess(null, null)).isTrue()
  }

  @Test
  fun `test shouldProcess with null package and file`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.packages" to "com.example"
    ).toConfig()

    assertThat(config.shouldProcess(null, null)).isFalse()
    assertThat(config.shouldProcess(null, "Test.kt")).isFalse()
  }

  @Test
  fun `test package prefix matching`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.packages" to "com.example"
    ).toConfig()

    assertThat(config.shouldProcess("com.example", "Test.kt")).isTrue()
    assertThat(config.shouldProcess("com.example.models", "User.kt")).isTrue()
    assertThat(config.shouldProcess("com.example.models.dto", "UserDTO.kt")).isTrue()
    assertThat(config.shouldProcess("com.exampleother", "Test.kt")).isFalse()
    assertThat(config.shouldProcess("com.other.example", "Test.kt")).isFalse()
  }

  @Test
  fun `test exact file name matching`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.files" to "User.kt"
    ).toConfig()

    assertThat(config.shouldProcess("com.example", "User.kt")).isTrue()
    assertThat(config.shouldProcess("com.example", "UserModel.kt")).isFalse()
    assertThat(config.shouldProcess("com.example", "user.kt")).isFalse()
  }

  @Test
  fun `test outputDir configuration`() {
    val outputDir = File(tempDir, "custom/rust/output")
    val config = mapOf(
      "rust.output.dir" to outputDir.absolutePath
    ).toConfig()

    assertThat(config.outputDir).isEqualTo(outputDir)
  }

  @Test
  fun `test multiple package filters`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.packages" to "com.example, org.test, dev.nemecec"
    ).toConfig()

    assertThat(config.shouldProcess("com.example.models", "Test.kt")).isTrue()
    assertThat(config.shouldProcess("org.test.util", "Test.kt")).isTrue()
    assertThat(config.shouldProcess("dev.nemecec.core", "Test.kt")).isTrue()
    assertThat(config.shouldProcess("com.other", "Test.kt")).isFalse()
  }

  @Test
  fun `test multiple file filters`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.filter.files" to "User.kt, Profile.kt, Model.kt"
    ).toConfig()

    assertThat(config.shouldProcess("com.example", "User.kt")).isTrue()
    assertThat(config.shouldProcess("com.example", "Profile.kt")).isTrue()
    assertThat(config.shouldProcess("com.example", "Model.kt")).isTrue()
    assertThat(config.shouldProcess("com.example", "Other.kt")).isFalse()
  }

  @Test
  fun `test marker annotations configuration`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.markerAnnotations" to "com.example.CustomAnnotation, org.test.AnotherAnnotation"
    ).toConfig()

    // Verify marker annotations are configured correctly
    assertThat(config.markerAnnotations).isEqualTo(setOf("com.example.CustomAnnotation", "org.test.AnotherAnnotation"))
  }

  @Test
  fun `test discriminator annotations configuration`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.discriminatorAnnotations" to "com.example.CustomDiscriminator.type, org.test.TypeTag.value"
    ).toConfig()

    // Verify discriminator annotations are configured correctly
    assertThat(config.discriminatorAnnotations).isEqualTo(setOf("com.example.CustomDiscriminator.type", "org.test.TypeTag.value"))
  }

  @Test
  fun `test discriminator annotations default value`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath
    ).toConfig()

    // Verify default discriminator annotations are used
    assertThat(config.discriminatorAnnotations).isEqualTo(setOf(
      "kotlinx.serialization.json.JsonClassDiscriminator.discriminator",
      "kotlinx.serialization.Polymorphic.value"
    ))
  }

  @Test
  fun `test serialNameAnnotation default value`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath
    ).toConfig()

    // Verify default serial name annotation is used
    assertThat(config.serialNameAnnotation).isEqualTo("kotlinx.serialization.SerialName.value")
  }

  @Test
  fun `test serialNameAnnotation custom value`() {
    val config = mapOf(
      "rust.output.dir" to tempDir.absolutePath,
      "rust.serialNameAnnotation" to "com.example.CustomSerialName.name"
    ).toConfig()

    // Verify custom serial name annotation is configured correctly
    assertThat(config.serialNameAnnotation).isEqualTo("com.example.CustomSerialName.name")
  }
}
