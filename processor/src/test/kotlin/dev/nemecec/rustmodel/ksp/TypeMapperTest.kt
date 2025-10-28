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

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TypeMapperTest {

  @ParameterizedTest
  @CsvSource(
    "User, User",
    "UserProfile, UserProfile",
    "APIResponse, APIResponse",
    "XMLParser, XMLParser",
  )
  fun `test toRustPascalCaseName preserves PascalCase`(input: String, expected: String) {
    assertThat(TypeMapper.toRustPascalCaseName(input)).isEqualTo(expected)
  }

  @ParameterizedTest
  @CsvSource(
    "userName, user_name",
    "firstName, first_name",
    "lastName, last_name",
    "emailAddress, email_address",
    "isActive, is_active",
    "userId, user_id",
    "createdAt, created_at",
    "updatedAt, updated_at",
    "phoneNumber, phone_number",
    "streetAddress, street_address",
    "firstName, first_name",
    "MyFile, my_file",
    "UserModel, user_model",
  )
  fun `test toRustSnakeCaseName converts camelCase to snake_case`(input: String, expected: String) {
    assertThat(TypeMapper.toRustSnakeCaseName(input)).isEqualTo(expected)
  }

  @ParameterizedTest
  @CsvSource(
    "PENDING, Pending",
    "APPROVED, Approved",
    "IN_PROGRESS, InProgress",
    "WAITING_FOR_APPROVAL, WaitingForApproval",
  )
  fun `test toRustEnumVariantName converts SCREAMING_SNAKE_CASE to PascalCase`(
    input: String,
    expected: String,
  ) {
    assertThat(TypeMapper.toRustEnumVariantName(input)).isEqualTo(expected)
  }

  @Test
  fun `test toRustSnakeCaseName handles single word lowercase`() {
    assertThat(TypeMapper.toRustSnakeCaseName("name")).isEqualTo("name")
    assertThat(TypeMapper.toRustSnakeCaseName("id")).isEqualTo("id")
    assertThat(TypeMapper.toRustSnakeCaseName("value")).isEqualTo("value")
  }

  @Test
  fun `test toRustSnakeCaseName handles already snake_case`() {
    assertThat(TypeMapper.toRustSnakeCaseName("user_name")).isEqualTo("user_name")
    assertThat(TypeMapper.toRustSnakeCaseName("first_name")).isEqualTo("first_name")
  }

  @Test
  fun `test toRustSnakeCaseName handles multiple consecutive capitals`() {
    // Handles consecutive capitals: last capital before lowercase starts new word
    assertThat(TypeMapper.toRustSnakeCaseName("URL")).isEqualTo("url")
    assertThat(TypeMapper.toRustSnakeCaseName("HTTPClient")).isEqualTo("http_client")
    assertThat(TypeMapper.toRustSnakeCaseName("XMLParser")).isEqualTo("xml_parser")
    assertThat(TypeMapper.toRustSnakeCaseName("parseHTMLString")).isEqualTo("parse_html_string")
    assertThat(TypeMapper.toRustSnakeCaseName("APIResponse")).isEqualTo("api_response")
    assertThat(TypeMapper.toRustSnakeCaseName("getHTTPSConnection"))
      .isEqualTo("get_https_connection")
    assertThat(TypeMapper.toRustSnakeCaseName("HTTPSProxy")).isEqualTo("https_proxy")
  }

  @Test
  fun `test toRustPascalCaseName identity for simple names`() {
    assertThat(TypeMapper.toRustPascalCaseName("User")).isEqualTo("User")
    assertThat(TypeMapper.toRustPascalCaseName("Profile")).isEqualTo("Profile")
    assertThat(TypeMapper.toRustPascalCaseName("Model")).isEqualTo("Model")
  }

  @Test
  fun `test toRustEnumVariantName handles single word`() {
    assertThat(TypeMapper.toRustEnumVariantName("ACTIVE")).isEqualTo("Active")
    assertThat(TypeMapper.toRustEnumVariantName("PENDING")).isEqualTo("Pending")
  }

  @Test
  fun `test toRustEnumVariantName handles lowercase input`() {
    assertThat(TypeMapper.toRustEnumVariantName("active")).isEqualTo("Active")
    assertThat(TypeMapper.toRustEnumVariantName("pending")).isEqualTo("Pending")
  }
}
