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
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Tests for keyword template functionality */
class KeywordTemplateTest {

  @TempDir lateinit var tempDir: File

  @Test
  fun `test raw identifier template generates correct code`() {
    val rustCode = generateRustCode(keywordTemplate = "r#{field}")

    assertThat(rustCode).contains("pub r#type: String")
    assertThat(rustCode).contains("pub r#match: String")
    assertThat(rustCode).contains("pub r#async: bool")
    // Raw identifiers don't need serde rename for keywords
    assertThat(rustCode.contains("#[serde(rename = \"type\")]")).isEqualTo(false)
  }

  @Test
  fun `test suffix template generates correct code`() {
    val rustCode = generateRustCode(keywordTemplate = "{field}_kw")

    assertThat(rustCode).contains("pub type_kw: String")
    assertThat(rustCode).contains("pub match_kw: String")
    assertThat(rustCode).contains("pub async_kw: bool")
    // Suffix templates need serde rename to preserve JSON field names
    assertThat(rustCode).contains("#[serde(rename = \"type\")]")
    assertThat(rustCode).contains("#[serde(rename = \"match\")]")
    assertThat(rustCode).contains("#[serde(rename = \"async\")]")
  }

  @Test
  fun `test prefix template generates correct code`() {
    val rustCode = generateRustCode(keywordTemplate = "kw_{field}")

    assertThat(rustCode).contains("pub kw_type: String")
    assertThat(rustCode).contains("pub kw_match: String")
    assertThat(rustCode).contains("pub kw_async: bool")
    // Prefix templates need serde rename to preserve JSON field names
    assertThat(rustCode).contains("#[serde(rename = \"type\")]")
    assertThat(rustCode).contains("#[serde(rename = \"match\")]")
    assertThat(rustCode).contains("#[serde(rename = \"async\")]")
  }

  @Test
  fun `test prefix and suffix template generates correct code`() {
    val rustCode = generateRustCode(keywordTemplate = "kw_{field}_field")

    assertThat(rustCode).contains("pub kw_type_field: String")
    assertThat(rustCode).contains("pub kw_match_field: String")
    assertThat(rustCode).contains("pub kw_async_field: bool")
    // Prefix+suffix templates need serde rename to preserve JSON field names
    assertThat(rustCode).contains("#[serde(rename = \"type\")]")
    assertThat(rustCode).contains("#[serde(rename = \"match\")]")
    assertThat(rustCode).contains("#[serde(rename = \"async\")]")
  }

  @OptIn(ExperimentalCompilerApi::class)
  private fun generateRustCode(keywordTemplate: String): String {
    val source =
      SourceFile.kotlin(
        "KeywordTest.kt",
        """
                package test

                import kotlinx.serialization.Serializable

                @Serializable
                data class KeywordTest(
                    val type: String,
                    val match: String,
                    val async: Boolean,
                )
            """
          .trimIndent(),
      )

    val compilation =
      KotlinCompilation().apply {
        useKsp2()
        sources = listOf(source)
        symbolProcessorProviders = mutableListOf(RustGeneratorProcessorProvider())
        inheritClassPath = true
        kspProcessorOptions =
          mutableMapOf(
            "rust.output.dir" to tempDir.absolutePath,
            "rust.keywordTemplate" to keywordTemplate,
          )
      }

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

    // Find and read the generated Rust file
    val rustFiles = tempDir.listFiles { file -> file.extension == "rs" }
    assertThat(rustFiles?.isNotEmpty() ?: false).isEqualTo(true)

    return rustFiles!!.first().readText()
  }
}
