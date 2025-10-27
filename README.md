# Rust Source Generator KSP

A Kotlin Symbol Processing (KSP) library that generates Rust struct and enum definitions from Kotlin data models
annotated with kotlinx.serialization.

## Features

- **File-based organization**: Classes in the same source file generate a single Rust output file
- **Flexible filtering**: Filter by package, source files, or process everything
- **Configurable marker annotations**: Process classes with custom annotations (defaults to `@Serializable`)
- **Custom output directory**: Configure where schemas are generated (default: `build/generated/rust`)
- **Rich type support**:
    - Basic types (String, Int, Double, Boolean, etc.)
    - Collections (List → Vec, Set → HashSet, Map → HashMap)
    - Advanced types: Java Time API, UUID
    - Nullable types as `Option<T>`
    - Enums as Rust enums
    - Sealed classes/interfaces as Rust enums with variants
    - Nested objects
    - KDoc comments as Rust doc comments (`///`)
    - Support for closed polymorphism with type discriminator property

## Setup

### 1. Add version catalog

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.2.20"
ksp = "2.2.20-2.0.2"
kotlinx-serialization = "1.9.0"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 2. Add to your project

In your `build.gradle.kts`:

```kotlin
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

dependencies {
  implementation(libs.kotlinx.serialization.json)
  ksp(project(":processor"))
}

kotlin {
  jvmToolchain(17)
}
```

### 3. Configure the processor

Add KSP arguments to configure the generator:

```kotlin
ksp {
  // Output directory (default: build/generated/rust)
  arg("rust.output.dir", "${project.layout.buildDirectory.get()}/generated/rust")

  // Filter by package prefix (optional - processes all by default)
  arg("rust.filter.packages", "com.example.models,com.example.dto")

  // Filter by specific source files (optional - processes all by default)
  arg("rust.filter.files", "User.kt,Product.kt")

  // Specify marker annotations to process (default: kotlinx.serialization.Serializable)
  arg("rust.markerAnnotations", "kotlinx.serialization.Serializable,com.example.CustomAnnotation")

  // Specify discriminator annotations for sealed classes with argument names
  // Format: "qualified.annotation.Name.argumentName"
  // Default: JsonClassDiscriminator.discriminator, Polymorphic.value
  arg("rust.discriminatorAnnotations", "kotlinx.serialization.json.JsonClassDiscriminator.discriminator,kotlinx.serialization.Polymorphic.value")

  // Specify serial name annotation with argument name for field/enum renaming
  // Format: "qualified.annotation.Name.argumentName"
  // Default: SerialName.value
  arg("rust.serialNameAnnotation", "kotlinx.serialization.SerialName.value")
}
```

## Configuration Options

| Option                          | Description                                                                                      | Default                                                                                                      |
|---------------------------------|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `rust.output.dir`               | Output directory for generated Rust files                                                        | `build/generated/rust`                                                                                       |
| `rust.filter.packages`          | Comma-separated package prefixes to process                                                      | Empty (processes all packages)                                                                               |
| `rust.filter.files`             | Comma-separated source file names to process                                                     | Empty (processes all files)                                                                                  |
| `rust.markerAnnotations`        | Comma-separated annotations to look for                                                          | `kotlinx.serialization.Serializable`                                                                         |
| `rust.discriminatorAnnotations` | Comma-separated discriminator annotations for sealed classes (format: `annotation.Name.argName`) | `kotlinx.serialization.json.JsonClassDiscriminator.discriminator`, `kotlinx.serialization.Polymorphic.value` |
| `rust.serialNameAnnotation`     | Serial name annotation for field/enum renaming (format: `annotation.Name.argName`)               | `kotlinx.serialization.SerialName.value`                                                                     |

**Note:** By default, all files are processed. Use filters to restrict processing to specific packages or files.

**Annotation Format:** For `discriminatorAnnotations` and `serialNameAnnotation`, specify both the annotation qualified name and the argument name separated by a dot (e.g., `com.example.MyAnnotation.myArgument`). This allows you to use custom annotations with different argument names.

## Usage Examples

### Basic Data Class

**Kotlin:**

```kotlin
@Serializable
data class User(
  val id: UUID,
  val userName: String,
  val email: String,
  val age: Int?,
  val isActive: Boolean
)
```

**Generated Rust:**

```rust
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct User {
    pub id: String,
    pub user_name: String,
    pub email: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub age: Option<i32>,
    pub is_active: bool,
}
```

### Enums

**Kotlin:**

```kotlin
@Serializable
enum class Theme {
  @SerialName("light")
  LIGHT,

  @SerialName("dark")
  DARK,

  @SerialName("auto")
  AUTO
}
```

**Generated Rust:**

```rust
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, serde::Serialize, serde::Deserialize)]
pub enum Theme {
    #[serde(rename = "light")]
    Light,
    #[serde(rename = "dark")]
    Dark,
    #[serde(rename = "auto")]
    Auto,
}
```

### Sealed Classes (Polymorphism)

**Kotlin:**

```kotlin
@Serializable
@JsonClassDiscriminator("message_type")
sealed class Message {
  abstract val id: String
  abstract val timestamp: Long
}

@Serializable
@SerialName("text")
data class TextMessage(
  override val id: String,
  override val timestamp: Long,
  val content: String
) : Message()

@Serializable
@SerialName("image")
data class ImageMessage(
  override val id: String,
  override val timestamp: Long,
  val imageUrl: String
) : Message()
```

**Generated Rust:**

```rust
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(tag = "message_type")]
pub enum Message {
    #[serde(rename = "text")]
    TextMessage(TextMessage),
    #[serde(rename = "image")]
    ImageMessage(ImageMessage),
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct TextMessage {
    pub id: String,
    pub timestamp: i64,
    pub content: String,
}

#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct ImageMessage {
    pub id: String,
    pub timestamp: i64,
    pub image_url: String,
}
```

### Collections and Complex Types

**Kotlin:**

```kotlin
@Serializable
data class Product(
  val id: UUID,
  val name: String,
  val price: Double,
  val tags: List<String>,
  val categories: Set<String>,
  val metadata: Map<String, String>
)
```

**Generated Rust:**

```rust
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Product {
    pub id: String,
    pub name: String,
    pub price: f64,
    pub tags: Vec<String>,
    pub categories: std::collections::HashSet<String>,
    pub metadata: std::collections::HashMap<String, String>,
}
```

### KDoc Comments

**Kotlin:**

```kotlin
/**
 * Represents a user in the system
 */
@Serializable
data class User(
  /** Unique user identifier */
  val id: UUID,
  /** User's display name */
  val userName: String
)
```

**Generated Rust:**

```rust
/// Represents a user in the system
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct User {
    /// Unique user identifier
    pub id: String,
    /// User's display name
    pub user_name: String,
}
```

## Type Mappings

| Kotlin Type     | Rust Type                        |
|-----------------|----------------------------------|
| `String`        | `String`                         |
| `Int`           | `i32`                            |
| `Long`          | `i64`                            |
| `Short`         | `i16`                            |
| `Byte`          | `i8`                             |
| `Double`        | `f64`                            |
| `Float`         | `f32`                            |
| `Boolean`       | `bool`                           |
| `UInt`          | `u32`                            |
| `ULong`         | `u64`                            |
| `List<T>`       | `Vec<T>`                         |
| `Set<T>`        | `std::collections::HashSet<T>`   |
| `Map<K,V>`      | `std::collections::HashMap<K,V>` |
| `T?`            | `Option<T>`                      |
| `UUID`          | `String`                         |
| `Instant`       | `String` (ISO-8601)              |
| `LocalDate`     | `String` (ISO date)              |
| `LocalDateTime` | `String` (ISO date-time)         |

## Building and Running

```bash
# Build the processor
./gradlew :processor:build

# Run KSP on the example project
./gradlew :example:kspKotlin

# View generated Rust files
ls -la example/build/generated/rust/
```

See [TESTS.md](TESTS.md) for detailed information about tests.

## Generated File Organization

The generator creates one Rust file per Kotlin source file:

- `User.kt` → `user.rs`
- `Product.kt` → `product.rs`
- `Message.kt` → `message.rs`

All classes from the same Kotlin file are grouped together in the corresponding Rust file.

## Requirements

- **Gradle**: 9.1.0+
- **Java**: 17+ (required for Gradle 9)
- **Kotlin**: 2.2.20+
- **KSP**: 2.2.20-2.0.2+ (KSP2)
- **kotlinx.serialization**: 1.9.0+

## License

[Apache Public License 2.0](LICENSE)
