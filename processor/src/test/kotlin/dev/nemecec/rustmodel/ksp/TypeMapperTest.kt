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

  @ParameterizedTest
  @CsvSource(
    "type, true",
    "match, true",
    "async, true",
    "await, true",
    "const, true",
    "static, true",
    "struct, true",
    "enum, true",
    "trait, true",
    "impl, true",
    "self, true",
    "Self, true",
    "super, true",
    "loop, true",
    "break, true",
    "continue, true",
    "return, true",
    "if, true",
    "else, true",
    "while, true",
    "for, true",
    "in, true",
    "as, true",
    "mod, true",
    "pub, true",
    "use, true",
    "extern, true",
    "crate, true",
    "fn, true",
    "let, true",
    "mut, true",
    "ref, true",
    "move, true",
    "where, true",
    "unsafe, true",
    "true, true",
    "false, true",
    "dyn, true",
    "abstract, true",
    "become, true",
    "box, true",
    "do, true",
    "final, true",
    "macro, true",
    "override, true",
    "priv, true",
    "typeof, true",
    "unsized, true",
    "virtual, true",
    "yield, true",
    "union, true",
    "my_type, false",
    "value, false",
    "field_name, false",
    "user_id, false",
  )
  fun `test isRustKeyword identifies keywords correctly`(name: String, expected: Boolean) {
    assertThat(TypeMapper.isRustKeyword(name)).isEqualTo(expected)
  }

  @Test
  fun `test escapeFieldNameIfNeeded with raw identifier template`() {
    // With r#{field} template (raw identifiers), keywords should be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("type", "r#{field}")).isEqualTo("r#type")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("match", "r#{field}")).isEqualTo("r#match")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("async", "r#{field}")).isEqualTo("r#async")

    // Non-keywords should not be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("my_type", "r#{field}")).isEqualTo("my_type")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("value", "r#{field}")).isEqualTo("value")
  }

  @Test
  fun `test escapeFieldNameIfNeeded with prefix template`() {
    // With custom prefix template, keywords should be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("type", "kw_{field}")).isEqualTo("kw_type")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("match", "field_{field}"))
      .isEqualTo("field_match")

    // Non-keywords should not be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("my_type", "kw_{field}")).isEqualTo("my_type")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("value", "kw_{field}")).isEqualTo("value")
  }

  @Test
  fun `test escapeFieldNameIfNeeded with suffix template`() {
    // With suffix template, keywords should be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("type", "{field}_kw")).isEqualTo("type_kw")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("match", "{field}_field"))
      .isEqualTo("match_field")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("async", "{field}_")).isEqualTo("async_")

    // Non-keywords should not be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("my_type", "{field}_kw")).isEqualTo("my_type")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("value", "{field}_kw")).isEqualTo("value")
  }

  @Test
  fun `test escapeFieldNameIfNeeded with prefix and suffix template`() {
    // With prefix and suffix template, keywords should be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("type", "kw_{field}_field"))
      .isEqualTo("kw_type_field")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("match", "prefix_{field}_suffix"))
      .isEqualTo("prefix_match_suffix")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("async", "_{field}_")).isEqualTo("_async_")

    // Non-keywords should not be escaped
    assertThat(TypeMapper.escapeFieldNameIfNeeded("my_type", "kw_{field}_field"))
      .isEqualTo("my_type")
    assertThat(TypeMapper.escapeFieldNameIfNeeded("value", "kw_{field}_field")).isEqualTo("value")
  }

  @Test
  fun `test escapeFieldNameIfNeeded with all strict keywords`() {
    val strictKeywords =
      listOf(
        "as",
        "break",
        "const",
        "continue",
        "crate",
        "else",
        "enum",
        "extern",
        "false",
        "fn",
        "for",
        "if",
        "impl",
        "in",
        "let",
        "loop",
        "match",
        "mod",
        "move",
        "mut",
        "pub",
        "ref",
        "return",
        "self",
        "Self",
        "static",
        "struct",
        "super",
        "trait",
        "true",
        "type",
        "unsafe",
        "use",
        "where",
        "while",
        "async",
        "await",
        "dyn",
      )

    strictKeywords.forEach { keyword ->
      assertThat(TypeMapper.escapeFieldNameIfNeeded(keyword, "r#{field}")).isEqualTo("r#$keyword")
    }
  }

  @Test
  fun `test escapeFieldNameIfNeeded with reserved keywords`() {
    val reservedKeywords =
      listOf(
        "abstract",
        "become",
        "box",
        "do",
        "final",
        "macro",
        "override",
        "priv",
        "typeof",
        "unsized",
        "virtual",
        "yield",
      )

    reservedKeywords.forEach { keyword ->
      assertThat(TypeMapper.escapeFieldNameIfNeeded(keyword, "r#{field}")).isEqualTo("r#$keyword")
    }
  }

  @Test
  fun `test isRawIdentifierTemplate recognizes raw identifier template`() {
    // Should recognize the standard raw identifier template
    assertThat(TypeMapper.isRawIdentifierTemplate("r#{field}")).isEqualTo(true)

    // Should NOT recognize other templates
    assertThat(TypeMapper.isRawIdentifierTemplate("kw_{field}")).isEqualTo(false)
    assertThat(TypeMapper.isRawIdentifierTemplate("{field}_kw")).isEqualTo(false)
    assertThat(TypeMapper.isRawIdentifierTemplate("kw_{field}_field")).isEqualTo(false)
    assertThat(TypeMapper.isRawIdentifierTemplate("r#{field}_extra")).isEqualTo(false)
    assertThat(TypeMapper.isRawIdentifierTemplate("prefix_r#{field}")).isEqualTo(false)
  }
}
