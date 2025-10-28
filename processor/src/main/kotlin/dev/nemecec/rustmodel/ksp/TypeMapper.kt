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

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

/** Maps Kotlin types to their Rust equivalents */
object TypeMapper {

  /**
   * Special Rust path keywords that cannot be escaped with raw identifiers (r#). These keywords
   * have special meaning in Rust's module system and path resolution:
   * - `super`: refers to the parent module
   * - `self`: refers to the current module
   * - `crate`: refers to the crate root
   *
   * When these appear as field names, they must use a non-raw-identifier escape strategy (e.g.,
   * suffix like "{field}_kw" or prefix like "kw_{field}").
   */
  private val RUST_PATH_KEYWORDS = setOf("super", "self", "crate")

  /**
   * Complete set of Rust keywords that need to be escaped when used as field names. Includes strict
   * keywords, reserved keywords, and weak keywords.
   */
  private val RUST_KEYWORDS =
    setOf(
      // Strict keywords (always reserved)
      "as",
      "break",
      "const",
      "continue",
      "crate",
      "else",
      "enum",
      "extern",
      "false",
      "fn",
      "for",
      "if",
      "impl",
      "in",
      "let",
      "loop",
      "match",
      "mod",
      "move",
      "mut",
      "pub",
      "ref",
      "return",
      "self",
      "Self",
      "static",
      "struct",
      "super",
      "trait",
      "true",
      "type",
      "unsafe",
      "use",
      "where",
      "while",

      // Keywords added in Rust 2018+
      "async",
      "await",
      "dyn",

      // Reserved keywords (not currently used but reserved for future use)
      "abstract",
      "become",
      "box",
      "do",
      "final",
      "macro",
      "override",
      "priv",
      "typeof",
      "unsized",
      "virtual",
      "yield",

      // Weak keywords (context-dependent, but safer to escape)
      "union",
    )

  /** Checks if a field name is a Rust keyword. */
  fun isRustKeyword(name: String): Boolean = name in RUST_KEYWORDS

  /**
   * Escapes a field name if it's a Rust keyword by applying the specified template.
   *
   * Template format uses {field} as placeholder:
   * - "r#{field}" - prefix only (raw identifier)
   * - "{field}_kw" - suffix only
   * - "kw_{field}_field" - prefix and suffix
   *
   * @param name The field name to potentially escape
   * @param keywordTemplate The template to apply (e.g., "r#{field}" for raw identifiers)
   * @return The escaped field name if it's a keyword, otherwise the original name
   */
  fun escapeFieldNameIfNeeded(name: String, keywordTemplate: String): String {
    return if (isRustKeyword(name)) {
      keywordTemplate.replace("{field}", name)
    } else {
      name
    }
  }

  /**
   * Checks if the template uses raw identifier syntax (r# prefix). Raw identifiers don't require
   * serde rename attributes.
   */
  fun isRawIdentifierTemplate(template: String): Boolean {
    return template == "r#{field}"
  }

  /**
   * Validates that a field name can be escaped using the given template.
   *
   * @param name The field name to validate
   * @param keywordTemplate The keyword escape template to use
   * @throws IllegalArgumentException if the field name is a path keyword and the template uses raw
   *   identifiers
   */
  fun validateKeywordEscaping(name: String, keywordTemplate: String) {
    require(!(name in RUST_PATH_KEYWORDS && isRawIdentifierTemplate(keywordTemplate))) {
      "Field name '$name' cannot be escaped with raw identifiers (r#) in Rust. " +
        "The keywords 'super', 'self', and 'crate' have special meaning in Rust's path resolution " +
        "and cannot be used as raw identifiers. " +
        "Please configure a different keyword template using suffix or prefix patterns. " +
        "Examples: '{field}_kw', 'kw_{field}', or 'kw_{field}_kw'. " +
        "Set this in your KSP options with: rust.keywordTemplate={field}_kw"
    }
  }

  fun mapType(type: KSType, nullable: Boolean = type.isMarkedNullable): String {
    val baseType =
      when (val declaration = type.declaration) {
        is KSClassDeclaration -> mapClassType(declaration, type)
        else -> "String" // Default fallback
      }

    return if (nullable) "Option<$baseType>" else baseType
  }

  private fun mapClassType(declaration: KSClassDeclaration, type: KSType): String {
    val qualifiedName = declaration.qualifiedName?.asString() ?: return "String"

    return when (qualifiedName) {
      // Basic types
      "kotlin.String" -> "String"
      "kotlin.Int" -> "i32"
      "kotlin.Long" -> "i64"
      "kotlin.Short" -> "i16"
      "kotlin.Byte" -> "i8"
      "kotlin.Double" -> "f64"
      "kotlin.Float" -> "f32"
      "kotlin.Boolean" -> "bool"
      "kotlin.Char" -> "char"
      "kotlin.UInt" -> "u32"
      "kotlin.ULong" -> "u64"
      "kotlin.UShort" -> "u16"
      "kotlin.UByte" -> "u8"

      // Collections
      "kotlin.collections.List",
      "kotlin.collections.MutableList" -> {
        val elementType = type.arguments.firstOrNull()?.type?.resolve()
        val mapped = elementType?.let { mapType(it) } ?: "String"
        "Vec<$mapped>"
      }
      "kotlin.collections.Set",
      "kotlin.collections.MutableSet" -> {
        val elementType = type.arguments.firstOrNull()?.type?.resolve()
        val mapped = elementType?.let { mapType(it) } ?: "String"
        "std::collections::HashSet<$mapped>"
      }
      "kotlin.collections.Map",
      "kotlin.collections.MutableMap" -> {
        val keyType = type.arguments.getOrNull(0)?.type?.resolve()
        val valueType = type.arguments.getOrNull(1)?.type?.resolve()
        val mappedKey = keyType?.let { mapType(it) } ?: "String"
        val mappedValue = valueType?.let { mapType(it) } ?: "String"
        "std::collections::HashMap<$mappedKey, $mappedValue>"
      }

      // Java Time API
      "java.time.Instant" -> "String" // ISO-8601 format
      "java.time.LocalDate" -> "String" // ISO date format
      "java.time.LocalDateTime" -> "String" // ISO date-time format
      "java.time.LocalTime" -> "String" // ISO time format
      "java.time.OffsetDateTime" -> "String" // ISO offset date-time
      "java.time.ZonedDateTime" -> "String" // ISO zoned date-time
      "java.time.Duration" -> "String" // ISO-8601 duration
      "java.time.Period" -> "String" // ISO-8601 period

      // UUID
      "java.util.UUID" -> "String" // UUID string format

      // Custom types - use the simple name converted to snake_case
      else -> toRustPascalCaseName(declaration.simpleName.asString())
    }
  }

  fun toRustPascalCaseName(kotlinName: String): String {
    // Convert PascalCase to PascalCase (Rust struct names are PascalCase)
    return kotlinName
  }

  fun toRustSnakeCaseName(kotlinName: String): String {
    // Convert camelCase to snake_case
    // Handle multiple consecutive capitals: HTTPClient -> http_client
    // The last capital letter before a lowercase letter starts a new word
    return kotlinName
      // Insert underscore between lowercase and uppercase: camelCase -> camel_Case
      .replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
      // Insert underscore between consecutive capitals and lowercase: HTTPClient -> HTTP_Client
      .replace(Regex("([A-Z]+)([A-Z][a-z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
      .lowercase()
  }

  fun toRustEnumVariantName(kotlinName: String): String {
    // Convert to PascalCase for enum variants
    return kotlinName.split("_").joinToString("") {
      it.lowercase().replaceFirstChar { c -> c.uppercase() }
    }
  }
}
