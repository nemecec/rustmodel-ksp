## Test Structure

The test suite is organized into the following test classes:

### 1. `RustGeneratorProcessorTest`

End-to-end integration tests that verify the complete processor functionality using `kotlin-compile-testing-ksp`. These
tests compile Kotlin source code and verify the generated Rust code.

**Test Coverage:**

- Simple data class generation
- Nullable fields and Optional types
- Collection types (List, Set, Map)
- Enum generation
- Sealed class generation
- SerialName annotation handling
- Multiple classes in the same file
- Numeric type mappings
- Nested generic types
- Custom type references
- File header generation
- CamelCase to snake_case conversion
- Package and file filtering
- Boolean type mapping

### 2. `TypeMapperTest`

Unit tests for the `TypeMapper` utility class that handles type conversions between Kotlin and Rust.

**Test Coverage:**

- PascalCase preservation for type names
- camelCase to snake_case conversion for field names
- SCREAMING_SNAKE_CASE to PascalCase for enum variants
- Edge cases (single words, already snake_case, multiple capitals)

### 3. `ProcessorConfigTest`

Unit tests for the `ProcessorConfig` class that manages processor configuration and filtering.

**Test Coverage:**

- `processAll` flag behavior
- Package filtering (prefix matching)
- File filtering (exact matching)
- Combined package and file filters
- Empty filter behavior
- Null package and file handling
- Multiple filters

### 4. `RustCodeGeneratorTest`

Unit tests for the `RustCodeGenerator` class that generates Rust code from Kotlin declarations.

**Test Coverage:**

- File header generation with module names
- Required imports inclusion
- Proper comment format

### 5. `RustCompilationTest` (Optional)

End-to-end integration tests that validate the generated Rust code by compiling it with Cargo and verifying JSON serialization/deserialization compatibility between Kotlin and Rust.

**Test Coverage:**

- Simple data class JSON round-trip
- Nullable fields JSON handling
- Collections (List, Set, Map) JSON handling
- Enum JSON serialization
- CamelCase to snake_case field name conversion

**Requirements:**

- Rust toolchain (cargo) installed
- C compiler (for linking Rust dependencies)
- On macOS: Xcode command-line tools with license agreement accepted

**Note:** These tests are disabled by default and must be explicitly enabled with the system property `-Drust.compilation.tests.enabled=true`

## Running Tests

Run all working tests:

```bash
./gradlew :processor:test
```

This will run:

- `TypeMapperTest` - Unit tests for type mapping functionality
- `ProcessorConfigTest` - Tests for configuration and filtering
- `RustCodeGeneratorTest` - Tests for Rust code generation
- `RustGeneratorProcessorTest` - End-to-end integration tests
- `RustCompilationTest` - Rust compilation and JSON validation tests (skipped by default)

Run specific test class:

```bash
./gradlew :processor:test --tests TypeMapperTest
./gradlew :processor:test --tests ProcessorConfigTest
./gradlew :processor:test --tests RustCodeGeneratorTest
./gradlew :processor:test --tests RustGeneratorProcessorTest
```

Run with verbose output:

```bash
./gradlew :processor:test --info
```

### Running Rust Compilation Tests

The `RustCompilationTest` suite validates that the generated Rust code compiles and handles JSON the same way as Kotlin. These tests are optional and require additional setup:

**Prerequisites:**

1. Install Rust toolchain:

   ```bash
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
   ```

2. On macOS, accept Xcode license:
   ```bash
   sudo xcodebuild -license
   ```

**Run with Rust compilation tests enabled:**

```bash
./gradlew :processor:test -Drust.compilation.tests.enabled=true
```

These tests will:

1. Generate Rust code from Kotlin classes
2. Create a Cargo project with the generated code
3. Compile the Rust code with `cargo test`
4. Verify that JSON serialization/deserialization works identically in both languages
