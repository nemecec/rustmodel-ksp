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

import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

/** Product in the catalog */
@Serializable
data class Product(
  @Serializable(with = UUIDSerializer::class) val id: UUID,
  val name: String,
  val description: String?,
  val price: Double,
  val currency: String,
  val stock: Int,
  val categories: Set<String>,
  @Serializable(with = LocalDateSerializer::class) val availableFrom: LocalDate?,
  val specifications: Map<String, String>?,
)

/** Shopping cart with products */
@Serializable
data class ShoppingCart(
  @Serializable(with = UUIDSerializer::class) val userId: UUID,
  val items: List<CartItem>,
  val totalPrice: Double,
)

/** Individual cart item */
@Serializable
data class CartItem(
  @Serializable(with = UUIDSerializer::class) val productId: UUID,
  val quantity: Int,
  val pricePerUnit: Double,
)
