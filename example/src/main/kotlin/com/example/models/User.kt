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

package com.example.models

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents a user in the system */
@Serializable
data class User(
  @Serializable(with = UUIDSerializer::class) val id: UUID,
  @SerialName("user_name") val userName: String,
  val email: String,
  val age: Int?,
  val isActive: Boolean,
  @Serializable(with = InstantSerializer::class) val createdAt: Instant,
  val tags: List<String>,
  val metadata: Map<String, String>,
)

/** User preferences */
@Serializable
data class UserPreferences(val theme: Theme, val notifications: Boolean, val language: String)
