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

package dev.nemecec.rustmodel.ksp.fixtures

import kotlinx.serialization.Serializable

/** Test fixture with fields that are Rust keywords */
@Serializable
data class KeywordFields(
  // Fields that are both Kotlin and Rust keywords need backticks in Kotlin
  val `as`: String,
  val `break`: Boolean,
  val `continue`: Boolean,
  val `do`: String,
  val `else`: String,
  val `false`: Boolean,
  val `for`: String,
  val `if`: String,
  val `in`: Boolean,
  val `return`: String,
  val `true`: Boolean,
  val `typeof`: String,
  val `while`: String,

  // Fields that are only Rust keywords (not Kotlin keywords)
  val type: String,
  val match: String,
  val async: Boolean,
  val const: Int,
  val static: String,
  val trait: String,
  val struct: String,
  val enum: String,
  val impl: String,
  val loop: Int,
  val mod: String,
  val pub: String,
  val use: String,
  val extern: String,
  val fn: String,
  val let: String,
  val mut: String,
  val ref: String,
  val move: String,
  val where: String,
  val unsafe: String,
  val await: String,
  val dyn: String,
  val abstract: String,
  val become: String,
  val box: String,
  val final: String,
  val macro: String,
  val override: String,
  val priv: String,
  val unsized: String,
  val virtual: String,
  val yield: String,
  val union: String,
)

/** Test fixture with camelCase keyword fields */
@Serializable
data class CamelCaseKeywords(val myType: String, val matchValue: String, val asyncTask: Boolean)
