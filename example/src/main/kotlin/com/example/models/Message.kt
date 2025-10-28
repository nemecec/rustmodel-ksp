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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/** Base sealed class for different message types */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("message_type")
sealed class Message {
  abstract val id: String
  abstract val timestamp: Long
}

/** A text message */
@Serializable
@SerialName("text")
data class TextMessage(
  override val id: String,
  override val timestamp: Long,
  val content: String,
  val sender: String,
) : Message()

/** An image message */
@Serializable
@SerialName("image")
data class ImageMessage(
  override val id: String,
  override val timestamp: Long,
  val imageUrl: String,
  val thumbnailUrl: String?,
  val caption: String?,
) : Message()

/** A system notification message */
@Serializable
@SerialName("system")
data class SystemMessage(
  override val id: String,
  override val timestamp: Long,
  val level: NotificationLevel,
  val text: String,
) : Message()

/** Notification severity levels */
@Serializable
enum class NotificationLevel {
  @SerialName("info") INFO,
  @SerialName("warning") WARNING,
  @SerialName("error") ERROR
}
