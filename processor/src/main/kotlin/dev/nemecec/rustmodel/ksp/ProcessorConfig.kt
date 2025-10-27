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

import java.io.File
import kotlin.text.startsWith

private const val DEFAULT_MARKER_ANNOTATION = "kotlinx.serialization.Serializable"
private val DEFAULT_DISCRIMINATOR_ANNOTATIONS = setOf(
  "kotlinx.serialization.json.JsonClassDiscriminator.discriminator",
  "kotlinx.serialization.Polymorphic.value"
)
private const val DEFAULT_SERIAL_NAME_ANNOTATION = "kotlinx.serialization.SerialName.value"

fun Map<String, String>.toConfig() =
  ProcessorConfig(
    outputDir = File(this["rust.output.dir"] ?: "build/generated/rust"),
    filterPackages = this["rust.filter.packages"]?.parseSet() ?: emptySet(),
    filterFiles = this["rust.filter.files"]?.parseSet() ?: emptySet(),
    markerAnnotations = this["rust.markerAnnotations"]?.parseSet() ?: setOf(DEFAULT_MARKER_ANNOTATION),
    discriminatorAnnotations = this["rust.discriminatorAnnotations"]?.parseSet() ?: DEFAULT_DISCRIMINATOR_ANNOTATIONS,
    serialNameAnnotation = this["rust.serialNameAnnotation"] ?: DEFAULT_SERIAL_NAME_ANNOTATION
  )

fun String.parseSet() = split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

data class ProcessorConfig(
  val outputDir: File,
  val filterPackages: Set<String>,
  val filterFiles: Set<String>,
  val markerAnnotations: Set<String>,
  val discriminatorAnnotations: Set<String>,
  val serialNameAnnotation: String,
) {

  fun shouldProcess(packageName: String?, fileName: String?): Boolean {
    if (filterPackages.isEmpty() && filterFiles.isEmpty()) {
      return true
    }
    if (filterPackages.anyMatches(packageName)) {
      return true
    }
    if (filterFiles.anyContains(fileName)) {
      return true
    }
    return false
  }

}

private fun Set<String>.anyContains(fileName: String?) =
  isNotEmpty() && fileName != null && contains(fileName)

private fun Set<String>.anyMatches(packageName: String?) =
  isNotEmpty() && packageName != null && any { filter -> packageName == filter || packageName.startsWith("$filter.") }
