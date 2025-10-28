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
import assertk.assertions.isTrue
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.symbolProcessorProviders
import dev.nemecec.rustmodel.ksp.fixtures.Container
import dev.nemecec.rustmodel.ksp.fixtures.Person
import dev.nemecec.rustmodel.ksp.fixtures.Profile
import dev.nemecec.rustmodel.ksp.fixtures.Status
import dev.nemecec.rustmodel.ksp.fixtures.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Integration tests that validate the generated Rust code by:
 * 1. Generating Rust code from Kotlin classes
 * 2. Compiling the Rust code with Cargo
 * 3. Comparing JSON serialization/deserialization between Kotlin and Rust
 *
 * These tests require:
 * - Rust toolchain (cargo) to be installed
 * - C compiler (for linking Rust dependencies)
 * - On macOS: Xcode command-line tools with license agreement accepted
 *   Use `xcode-select --install` to install and then `sudo xcodebuild -license accept`
 *
 * To run these tests, set the system property: -Drust.compilation.tests.enabled=true
 * Example: ./gradlew :processor:test -Drust.compilation.tests.enabled=true
 */
class RustCompilationTest {

  @BeforeEach
  fun checkRustEnvironment() {
    val enabled = System.getProperty("rust.compilation.tests.enabled", "false").toBoolean()
    Assumptions.assumeTrue(
      enabled,
      "Rust compilation tests are disabled. Enable with -Drust.compilation.tests.enabled=true"
    )

    // Check if cargo is available
    val cargoAvailable = try {
      val process = ProcessBuilder("cargo", "--version")
        .redirectErrorStream(true)
        .start()
      val output = process.inputStream.bufferedReader().readText()
      println("Using $output")
      process.waitFor() == 0
    } catch (e: Exception) {
      println("Error while running Cargo: $e")
      false
    }
    Assumptions.assumeTrue(cargoAvailable, "Cargo is not available. Please install Rust toolchain.")
  }

  @TempDir
  lateinit var tempDir: File

  private val json = Json { prettyPrint = false }

  @Test
  fun `test simple data class JSON round-trip`() {
    val kotlinSource = readFixtureSource("User.kt")

    // Create Kotlin object and serialize to JSON
    val userInstance = User(
      id = 123,
      name = "John Doe",
      email = "john@example.com"
    )
    val testInstance = json.encodeToString(userInstance)

    val rustCode = generateRustCode("User.kt", kotlinSource)
    val cargoOutput = compileAndRunRustTest(
      rustCode = rustCode,
      testCode = """
                #[test]
                fn test_deserialize_and_serialize() {
                    let json_input = r#"$testInstance"#;
                    let user: User = serde_json::from_str(json_input).unwrap();
                    assert_eq!(user.id, 123);
                    assert_eq!(user.name, "John Doe");
                    assert_eq!(user.email, "john@example.com");

                    let json_output = serde_json::to_string(&user).unwrap();
                    assert_eq!(json_output, json_input);
                }
            """.trimIndent()
    )

    // Check that the test passed - look for either the "ok" or "passed" message
    val testPassed = cargoOutput.contains("test test_deserialize_and_serialize ... ok") ||
        cargoOutput.contains("test result: ok")
    assertThat(testPassed).isTrue()
  }

  @Test
  fun `test nullable fields JSON handling`() {
    val kotlinSource = readFixtureSource("Profile.kt")

    // Create Kotlin objects and serialize to JSON
    val profileWithNulls = Profile(
      userId = 42,
      bio = null,
      avatar = null
    )
    val testWithNulls = json.encodeToString(profileWithNulls)

    val profileWithValues = Profile(
      userId = 42,
      bio = "Software developer",
      avatar = "https://example.com/avatar.jpg"
    )
    val testWithValues = json.encodeToString(profileWithValues)

    val rustCode = generateRustCode("Profile.kt", kotlinSource)
    val cargoOutput = compileAndRunRustTest(
      rustCode = rustCode,
      testCode = """
                #[test]
                fn test_nullable_fields() {
                    // Test with null values
                    let json_nulls = r#"$testWithNulls"#;
                    let profile: Profile = serde_json::from_str(json_nulls).unwrap();
                    assert_eq!(profile.user_id, 42);
                    assert_eq!(profile.bio, None);
                    assert_eq!(profile.avatar, None);

                    // Test with actual values
                    let json_values = r#"$testWithValues"#;
                    let profile2: Profile = serde_json::from_str(json_values).unwrap();
                    assert_eq!(profile2.user_id, 42);
                    assert_eq!(profile2.bio, Some("Software developer".to_string()));
                    assert_eq!(profile2.avatar, Some("https://example.com/avatar.jpg".to_string()));
                }
            """.trimIndent()
    )

    val testPassed = cargoOutput.contains("test test_nullable_fields ... ok") ||
        cargoOutput.contains("test result: ok")
    assertThat(testPassed).isTrue()
  }

  @Test
  fun `test collections JSON handling`() {
    val kotlinSource = readFixtureSource("Container.kt")

    // Create Kotlin object and serialize to JSON
    val containerInstance = Container(
      items = listOf("item1", "item2", "item3"),
      tags = setOf("tag1", "tag2"),
      metadata = mapOf("key1" to "value1", "key2" to "value2")
    )
    val testJson = json.encodeToString(containerInstance)

    val rustCode = generateRustCode("Container.kt", kotlinSource)
    val cargoOutput = compileAndRunRustTest(
      rustCode = rustCode,
      testCode = """
                #[test]
                fn test_collections() {
                    let json_input = r#"$testJson"#;
                    let container: Container = serde_json::from_str(json_input).unwrap();

                    assert_eq!(container.items.len(), 3);
                    assert_eq!(container.items[0], "item1");
                    assert_eq!(container.items[1], "item2");
                    assert_eq!(container.items[2], "item3");

                    assert_eq!(container.tags.len(), 2);
                    assert!(container.tags.contains("tag1"));
                    assert!(container.tags.contains("tag2"));

                    assert_eq!(container.metadata.len(), 2);
                    assert_eq!(container.metadata.get("key1"), Some(&"value1".to_string()));
                    assert_eq!(container.metadata.get("key2"), Some(&"value2".to_string()));
                }
            """.trimIndent()
    )

    val testPassed = cargoOutput.contains("test test_collections ... ok") ||
        cargoOutput.contains("test result: ok")
    assertThat(testPassed).isTrue()
  }

  @Test
  fun `test enum JSON handling`() {
    val kotlinSource = readFixtureSource("Status.kt")

    // Serialize enum values to JSON
    val pendingJson = json.encodeToString(Status.PENDING)
    val approvedJson = json.encodeToString(Status.APPROVED)
    val rejectedJson = json.encodeToString(Status.REJECTED)

    val rustCode = generateRustCode("Status.kt", kotlinSource)
    val cargoOutput = compileAndRunRustTest(
      rustCode = rustCode,
      testCode = """
                #[test]
                fn test_enum_serialization() {
                    let pending = Status::Pending;
                    let json = serde_json::to_string(&pending).unwrap();
                    assert_eq!(json, r#"$pendingJson"#);

                    let approved: Status = serde_json::from_str(r#"$approvedJson"#).unwrap();
                    assert!(matches!(approved, Status::Approved));

                    let rejected: Status = serde_json::from_str(r#"$rejectedJson"#).unwrap();
                    assert!(matches!(rejected, Status::Rejected));
                }
            """.trimIndent()
    )

    val testPassed = cargoOutput.contains("test test_enum_serialization ... ok") ||
        cargoOutput.contains("test result: ok")
    assertThat(testPassed).isTrue()
  }

  @Test
  fun `test camelCase to snake_case field names`() {
    val kotlinSource = readFixtureSource("Person.kt")

    // Create Kotlin object and serialize to JSON
    val personInstance = Person(
      firstName = "John",
      lastName = "Doe",
      emailAddress = "john@example.com"
    )
    val testJson = json.encodeToString(personInstance)

    val rustCode = generateRustCode("Person.kt", kotlinSource)
    val cargoOutput = compileAndRunRustTest(
      rustCode = rustCode,
      testCode = """
                #[test]
                fn test_field_name_conversion() {
                    let json_input = r#"$testJson"#;
                    let person: Person = serde_json::from_str(json_input).unwrap();

                    // Rust uses snake_case fields internally
                    assert_eq!(person.first_name, "John");
                    assert_eq!(person.last_name, "Doe");
                    assert_eq!(person.email_address, "john@example.com");

                    // But serializes to camelCase JSON
                    let json_output = serde_json::to_string(&person).unwrap();
                    assert_eq!(json_output, json_input);
                }
            """.trimIndent()
    )

    val testPassed = cargoOutput.contains("test test_field_name_conversion ... ok") ||
        cargoOutput.contains("test result: ok")
    assertThat(testPassed).isTrue()
  }

  private fun generateRustCode(fileName: String, kotlinSource: String): String {
    val source = SourceFile.kotlin(fileName, kotlinSource)

    val compilation = KotlinCompilation().apply {
      sources = listOf(source)
      symbolProcessorProviders = listOf(RustGeneratorProcessorProvider())
      inheritClassPath = true
      kspArgs = mutableMapOf(
        "rust.output.dir" to tempDir.absolutePath,
        "rust.process.all" to "true"
      )
    }

    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

    // Find and read the generated Rust file
    val rustFiles = tempDir.listFiles { file -> file.extension == "rs" }
    assertThat(rustFiles?.isNotEmpty() ?: false).isTrue()

    return rustFiles!!.first().readText()
  }

  private fun compileAndRunRustTest(
    rustCode: String,
    testCode: String
  ): String {
    // Create a temporary Cargo project
    val cargoDir = File(tempDir, "cargo_project")
    cargoDir.mkdirs()

    // Create Cargo.toml
    File(cargoDir, "Cargo.toml").writeText(
      """
            [package]
            name = "rust_validation_test"
            version = "0.1.0"
            edition = "2021"

            [dependencies]
            serde = { version = "1.0", features = ["derive"] }
            serde_json = "1.0"
        """.trimIndent()
    )

    // Create src directory
    val srcDir = File(cargoDir, "src")
    srcDir.mkdirs()

    // Create lib.rs with the generated Rust code and test
    File(srcDir, "lib.rs").writeText(
      """
            $rustCode

            #[cfg(test)]
            mod tests {
                use super::*;

                $testCode
            }
        """.trimIndent()
    )

    // Run cargo test
    val processBuilder = ProcessBuilder()
      .command("cargo", "test", "--quiet", "--color", "never")
      .directory(cargoDir)
      .redirectErrorStream(true)
    processBuilder.environment()["RUSTFLAGS"] = "-D warnings"
    val process = processBuilder.start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()

    if (exitCode != 0) {
      throw AssertionError("Cargo test failed with exit code $exitCode:\n$output")
    }

    return output
  }

  private fun readFixtureSource(fileName: String): String {
    val fixtureDir = File("src/test/kotlin/dev/nemecec/rustmodel/ksp/fixtures")
    val file = File(fixtureDir, fileName)
    return file.readText()
  }
}
