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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import java.io.File

/** KSP processor that generates Rust code from Kotlin models */
class RustGeneratorProcessor(private val logger: KSPLogger, private val config: ProcessorConfig) :
  SymbolProcessor {

  private val generator =
    RustCodeGenerator(config.discriminatorAnnotations, config.serialNameAnnotation)
  private val fileClasses = mutableMapOf<String, MutableList<KSClassDeclaration>>()
  private val sealedHierarchies =
    mutableMapOf<KSClassDeclaration, MutableList<KSClassDeclaration>>()

  override fun process(resolver: Resolver): List<KSAnnotated> {
    // Find all classes with marker annotations
    val annotatedClasses =
      config.markerAnnotations
        .flatMap { annotation ->
          resolver.getSymbolsWithAnnotation(annotation).filterIsInstance<KSClassDeclaration>()
        }
        .distinct()

    annotatedClasses.forEach { classDecl ->
      val containingFile = classDecl.containingFile
      val fileName = containingFile?.fileName
      val packageName = classDecl.packageName.asString()

      // Check if we should process this class
      if (!config.shouldProcess(packageName, fileName)) {
        return@forEach
      }

      // Group classes by source file
      val sourceFileName = fileName ?: "${classDecl.simpleName.asString()}.kt"
      fileClasses.getOrPut(sourceFileName) { mutableListOf() }.add(classDecl)

      // Track sealed class hierarchies
      if (classDecl.modifiers.contains(Modifier.SEALED)) {
        sealedHierarchies[classDecl] = mutableListOf()
      }

      // Check if this class extends a sealed class
      classDecl.superTypes.forEach { superType ->
        val superDecl = superType.resolve().declaration as? KSClassDeclaration
        if (superDecl?.modifiers?.contains(Modifier.SEALED) == true) {
          sealedHierarchies.getOrPut(superDecl) { mutableListOf() }.add(classDecl)
        }
      }
    }

    // Generate Rust files
    generateRustFiles()

    return emptyList()
  }

  private fun generateRustFiles() {
    fileClasses.forEach { (sourceFileName, classes: List<KSClassDeclaration>) ->
      val rustFileName =
        sourceFileName.removeSuffix(".kt").let { TypeMapper.toRustSnakeCaseName(it) }.plus(".rs")

      val content = buildString {
        // Add file header
        val moduleName = sourceFileName.removeSuffix(".kt")
        append(generator.generateFileHeader(moduleName))
        append("\n")

        // Generate code for each class
        classes.forEach { classDecl ->
          when {
            classDecl.modifiers.contains(Modifier.SEALED) -> {
              generateSealedClassAsEnum(classDecl)
            }
            classDecl.classKind == ClassKind.ENUM_CLASS -> {
              // Generate enum
              append(generator.generateEnum(classDecl))
              append("\n\n")
            }
            classDecl.classKind == ClassKind.CLASS || classDecl.classKind == ClassKind.OBJECT -> {
              // Skip if this is a subclass of a sealed class (already generated)
              val isSealed =
                classDecl.superTypes.any { superType ->
                  val superDecl = superType.resolve().declaration as? KSClassDeclaration
                  superDecl?.modifiers?.contains(Modifier.SEALED) == true
                }

              if (!isSealed) {
                // Generate regular struct
                append(generator.generateStruct(classDecl))
                append("\n\n")
              }
            }
          }
        }
      }

      writeRustFile(rustFileName, content)
    }
  }

  private fun StringBuilder.generateSealedClassAsEnum(classDecl: KSClassDeclaration) {
    val subclasses = sealedHierarchies[classDecl] ?: emptyList()
    if (subclasses.isNotEmpty()) {
      append(generator.generateSealedEnum(classDecl, subclasses))
      append("\n\n")

      // Generate structs for each subclass
      subclasses.forEach { subclass ->
        if (subclass.classKind == ClassKind.CLASS) {
          append(generator.generateStruct(subclass))
          append("\n\n")
        }
      }
    }
  }

  private fun writeRustFile(fileName: String, content: String) {
    val outputFile = File(config.outputDir, fileName)
    outputFile.parentFile?.mkdirs()
    outputFile.writeText(content)

    logger.info("Generated Rust file: ${outputFile.absolutePath}")
  }
}
