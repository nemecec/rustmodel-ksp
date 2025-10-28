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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.message
import org.junit.jupiter.api.Test

class RustCodeGeneratorTest {

  private val generator = RustCodeGenerator(
    discriminatorAnnotations = setOf(
      "kotlinx.serialization.json.JsonClassDiscriminator.discriminator",
      "kotlinx.serialization.Polymorphic.value"
    ),
    serialNameAnnotation = "kotlinx.serialization.SerialName.value"
  )

  @Test
  fun `test generateFileHeader`() {
    val header = generator.generateFileHeader("TestModule")

    assertThat(header).contains("//! Auto-generated Rust module from Kotlin sources")
    assertThat(header).contains("//! Module: TestModule")
    assertThat(header).contains("use serde::{Deserialize, Serialize};")
    assertThat(header).contains("use std::collections::{HashMap, HashSet};")
  }

  @Test
  fun `test generateFileHeader with different module names`() {
    val header1 = generator.generateFileHeader("User")
    assertThat(header1).contains("//! Module: User")

    val header2 = generator.generateFileHeader("UserProfile")
    assertThat(header2).contains("//! Module: UserProfile")

    val header3 = generator.generateFileHeader("ComplexModelName")
    assertThat(header3).contains("//! Module: ComplexModelName")
  }

  @Test
  fun `test file header includes required imports`() {
    val header = generator.generateFileHeader("Test")

    // Check for required imports
    assertThat(header).contains("use serde::{Deserialize, Serialize};")
    assertThat(header).contains("use std::collections::{HashMap, HashSet};")
  }

  @Test
  fun `test file header has proper comment format`() {
    val header = generator.generateFileHeader("Test")

    val lines = header.lines()
    assertThat(lines.any { it.startsWith("//!") }).isTrue()
  }

  @Test
  fun `test invalid serialNameAnnotation without dot fails`() {
    assertFailure {
      RustCodeGenerator(
        discriminatorAnnotations = setOf("kotlinx.serialization.json.JsonClassDiscriminator.discriminator"),
        serialNameAnnotation = "NoDotsInThisString"
      )
    }.isInstanceOf(IllegalArgumentException::class)
      .message().isNotNull()
      .contains("Invalid annotation configuration format", "NoDotsInThisString")
  }

  @Test
  fun `test invalid serialNameAnnotation with trailing dot fails`() {
    assertFailure {
      RustCodeGenerator(
        discriminatorAnnotations = setOf("kotlinx.serialization.json.JsonClassDiscriminator.discriminator"),
        serialNameAnnotation = "kotlinx.serialization.SerialName."
      )
    }.isInstanceOf(IllegalArgumentException::class)
      .message().isNotNull()
      .contains("Invalid annotation configuration format")
  }

  @Test
  fun `test invalid serialNameAnnotation with empty argument name fails`() {
    assertFailure {
      RustCodeGenerator(
        discriminatorAnnotations = setOf("kotlinx.serialization.json.JsonClassDiscriminator.discriminator"),
        serialNameAnnotation = "."
      )
    }.isInstanceOf(IllegalArgumentException::class)
      .message().isNotNull()
      .contains("Invalid annotation configuration")
  }

  @Test
  fun `test invalid discriminatorAnnotation without dot fails`() {
    assertFailure {
      RustCodeGenerator(
        discriminatorAnnotations = setOf("NoDot"),
        serialNameAnnotation = "kotlinx.serialization.SerialName.value"
      )
    }.isInstanceOf(IllegalArgumentException::class)
      .message().isNotNull()
      .contains("Invalid annotation configuration format", "NoDot")
  }

  @Test
  fun `test invalid discriminatorAnnotation with multiple invalid entries fails`() {
    assertFailure {
      RustCodeGenerator(
        discriminatorAnnotations = setOf(
          "kotlinx.serialization.json.JsonClassDiscriminator.discriminator",
          "InvalidAnnotation",
          "kotlinx.serialization.Polymorphic.value"
        ),
        serialNameAnnotation = "kotlinx.serialization.SerialName.value"
      )
    }.isInstanceOf(IllegalArgumentException::class)
      .message().isNotNull()
      .contains("Invalid annotation configuration format", "InvalidAnnotation")
  }
}
