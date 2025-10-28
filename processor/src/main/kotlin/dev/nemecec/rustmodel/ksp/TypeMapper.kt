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
