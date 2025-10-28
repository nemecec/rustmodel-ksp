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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Serializer for java.util.UUID */
object UUIDSerializer : KSerializer<UUID> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: UUID) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): UUID {
    return UUID.fromString(decoder.decodeString())
  }
}

/** Serializer for java.time.Instant */
object InstantSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Instant) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): Instant {
    return Instant.parse(decoder.decodeString())
  }
}

/** Serializer for java.time.LocalDate */
object LocalDateSerializer : KSerializer<LocalDate> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalDate) {
    encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
  }

  override fun deserialize(decoder: Decoder): LocalDate {
    return LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE)
  }
}

/** Serializer for java.time.LocalDateTime */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalDateTime) {
    encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  }

  override fun deserialize(decoder: Decoder): LocalDateTime {
    return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }
}

/** Serializer for java.time.OffsetDateTime */
object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: OffsetDateTime) {
    encoder.encodeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }

  override fun deserialize(decoder: Decoder): OffsetDateTime {
    return OffsetDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }
}

/** Serializer for java.time.ZonedDateTime */
object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: ZonedDateTime) {
    encoder.encodeString(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
  }

  override fun deserialize(decoder: Decoder): ZonedDateTime {
    return ZonedDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_ZONED_DATE_TIME)
  }
}
