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

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

private const val DERIVES_FOR_STRUCT =
  "Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize"
private const val DERIVES_FOR_ENUM =
  "Debug, Clone, Copy, PartialEq, Eq, Hash, serde::Serialize, serde::Deserialize"

/** Represents a parsed annotation configuration with qualified name and argument name */
private data class AnnotationConfig(val qualifiedName: String, val argumentName: String)

/**
 * Parses annotation configuration string in format "qualified.name.ArgumentName" Throws
 * IllegalArgumentException if format is invalid
 */
private fun String.parseAnnotationConfig(): AnnotationConfig {
  val lastDotIndex = lastIndexOf('.')

  require(lastDotIndex != -1 && lastDotIndex < length - 1) {
    "Invalid annotation configuration format: '$this'. " +
      "Expected format: 'qualified.annotation.Name.argumentName' " +
      "(e.g., 'kotlinx.serialization.SerialName.value')"
  }

  val qualifiedName = substring(0, lastDotIndex)
  val argumentName = substring(lastDotIndex + 1)

  require(qualifiedName.isNotEmpty()) {
    "Invalid annotation configuration: '$this'. Qualified name cannot be empty."
  }

  require(argumentName.isNotEmpty()) {
    "Invalid annotation configuration: '$this'. Argument name cannot be empty."
  }

  return AnnotationConfig(qualifiedName, argumentName)
}

/** Generates Rust code from Kotlin class declarations */
class RustCodeGenerator(discriminatorAnnotations: Set<String>, serialNameAnnotation: String) {
  // Validate configurations on initialization to fail fast
  private val serialNameConfig: AnnotationConfig = serialNameAnnotation.parseAnnotationConfig()
  private val discriminatorConfigs: Map<String, AnnotationConfig> =
    discriminatorAnnotations
      .associateWith { it.parseAnnotationConfig() }
      .mapKeys { it.value.qualifiedName }

  fun generateStruct(declaration: KSClassDeclaration): String {
    val builder = StringBuilder()

    builder.addDocComments(declaration)
    builder.addDerives(DERIVES_FOR_STRUCT)

    // Add struct definition
    builder.append(
      "pub struct ${TypeMapper.toRustPascalCaseName(declaration.simpleName.asString())} {\n"
    )

    // Add fields
    declaration.getAllProperties().forEach { property ->
      builder.addDocComments(property, indent = "    ")

      val originalFieldName = property.simpleName.asString()
      val fieldName = TypeMapper.toRustSnakeCaseName(originalFieldName)
      val fieldType = property.type.resolve()
      val rustType = TypeMapper.mapType(fieldType)

      // Check for serialization annotations or field name conversion
      val serialName = property.getSerialName()
      val nameToSerialize = serialName ?: originalFieldName

      // Add serde rename attribute if the serialization name differs from the Rust field name
      // This handles both explicit @SerialName annotations and camelCase to snake_case conversions
      if (fieldName != nameToSerialize) {
        builder.append("    #[serde(rename = \"$nameToSerialize\")]\n")
      }

      // Handle optional fields
      if (fieldType.isMarkedNullable) {
        builder.append("    #[serde(skip_serializing_if = \"Option::is_none\")]\n")
      }

      builder.append("    pub $fieldName: $rustType,\n")
    }

    builder.append("}\n")

    return builder.toString()
  }

  fun generateEnum(declaration: KSClassDeclaration): String {
    val builder = StringBuilder()

    builder.addDocComments(declaration)
    builder.addDerives(DERIVES_FOR_ENUM)

    // Add enum definition
    builder.append(
      "pub enum ${TypeMapper.toRustPascalCaseName(declaration.simpleName.asString())} {\n"
    )

    // Add enum variants
    declaration.declarations
      .filterIsInstance<KSClassDeclaration>()
      .filter { it.classKind == ClassKind.ENUM_ENTRY }
      .forEach { entry ->
        builder.addDocComments(entry, indent = "    ")

        val originalEntryName = entry.simpleName.asString()
        val variantName = TypeMapper.toRustEnumVariantName(originalEntryName)
        val serialName = entry.getSerialName()
        val nameToSerialize = serialName ?: originalEntryName

        // Add serde rename attribute if the serialization name differs from the Rust variant name
        // This handles both explicit @SerialName annotations and SCREAMING_SNAKE_CASE to PascalCase
        // conversions
        if (variantName != nameToSerialize) {
          builder.append("    #[serde(rename = \"$nameToSerialize\")]\n")
        }

        builder.append("    $variantName,\n")
      }

    builder.append("}\n")

    return builder.toString()
  }

  fun generateSealedEnum(
    declaration: KSClassDeclaration,
    subclasses: List<KSClassDeclaration>,
  ): String {
    val builder = StringBuilder()

    builder.addDocComments(declaration)
    builder.addDerives(DERIVES_FOR_ENUM)

    // Add tag/type discriminator
    val discriminator = declaration.getTypeDiscriminator()
    builder.append("#[serde(tag = \"$discriminator\")]\n")

    // Add enum definition
    builder.append(
      "pub enum ${TypeMapper.toRustPascalCaseName(declaration.simpleName.asString())} {\n"
    )

    // Add variants for each subclass
    subclasses.forEach { subclass ->
      builder.addDocComments(subclass, indent = "    ")

      val variantName = TypeMapper.toRustPascalCaseName(subclass.simpleName.asString())
      val serialName = subclass.getSerialName()

      if (serialName != null) {
        builder.append("    #[serde(rename = \"$serialName\")]\n")
      }

      builder.append("    $variantName($variantName),\n")
    }

    builder.append("}\n")

    return builder.toString()
  }

  private fun StringBuilder.addDocComments(declaration: KSDeclaration, indent: String = "") {
    val docComment = declaration.extractDocComment()
    if (docComment.isNotEmpty()) {
      append(docComment.prependIndent(indent))
    }
  }

  private fun StringBuilder.addDerives(derives: String) {
    append("#[derive($derives)]\n")
  }

  private fun KSDeclaration.extractDocComment(): String {
    val docString = docString ?: return ""

    return docString.lines().joinToString("\n") { line -> "/// ${line.trim()}" }.plus("\n")
  }

  private fun KSAnnotated.getSerialName(): String? {
    return annotations
      .find { it.annotationType.getQualifiedName() == serialNameConfig.qualifiedName }
      ?.arguments
      ?.find { it.name?.asString() == serialNameConfig.argumentName }
      ?.value as? String
  }

  private fun KSClassDeclaration.getTypeDiscriminator(): String {
    // Look for discriminator annotations
    val polymorphicAnnotation =
      annotations.find { it.annotationType.getQualifiedName() in discriminatorConfigs.keys }

    if (polymorphicAnnotation != null) {
      val qualifiedName = polymorphicAnnotation.annotationType.getQualifiedName()
      val config = qualifiedName?.let { discriminatorConfigs[it] }

      if (config != null) {
        val value =
          polymorphicAnnotation.arguments.find { it.name?.asString() == config.argumentName }?.value
            as? String

        if (value != null) return value
      }
    }

    return "type" // Default discriminator
  }

  private fun KSTypeReference.getQualifiedName() = resolve().declaration.qualifiedName?.asString()

  fun generateFileHeader(moduleName: String): String {
    return """
            // Auto-generated Rust module from Kotlin sources
            // Module: $moduleName

        """
      .trimIndent()
  }
}
