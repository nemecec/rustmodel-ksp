# Quick Start Guide

## Project Structure

```
rust-source-generator-ksp/
├── processor/                      # KSP processor implementation
│   └── src/main/kotlin/dev/nemecec/rustmodel/ksp/
│       ├── TypeMapper.kt          # Kotlin → Rust type mapping
│       ├── ProcessorConfig.kt     # Configuration handling
│       ├── RustCodeGenerator.kt   # Rust code generation logic
│       ├── RustGeneratorProcessor.kt       # Main KSP processor
│       └── RustGeneratorProcessorProvider.kt # KSP provider
├── example/                        # Example usage
│   └── src/main/kotlin/com/example/models/
│       ├── User.kt                # Basic data classes
│       ├── Theme.kt               # Enum example
│       ├── Message.kt             # Sealed class example
│       └── Product.kt             # Collections example
└── README.md                      # Full documentation

```

## Quick Build & Test

```bash
# 1. Build the processor
./gradlew :processor:build

# 2. Generate Rust files from example models
./gradlew :example:kspKotlin

# 3. View generated Rust files
ls -la example/build/generated/rust/
cat example/build/generated/rust/user.rs
```

## Configuration Examples

### Process specific packages

```kotlin
ksp {
    arg("rust.output.dir", "${project.buildDir}/rust")
    arg("rust.filter.packages", "com.example.models")
}
```

### Process specific files

```kotlin
ksp {
    arg("rust.filter.files", "User.kt,Product.kt")
}
```

### Process everything

```kotlin
ksp {
    arg("rust.process.all", "true")
}
```

## Key Features Demonstrated

### 1. Basic Types & Collections (User.kt, Product.kt)

- UUID, String, Int, Double, Boolean
- List → Vec
- Set → HashSet
- Map → HashMap
- Nullable types → Option

### 2. Enums (Theme.kt)

- Simple enums with @SerialName
- Rust enum with serde rename

### 3. Sealed Classes (Message.kt)

- Polymorphic types
- Type discriminator support
- Multiple subclass variants

### 4. Naming Conventions

- camelCase → snake_case for fields
- PascalCase preserved for types
- @SerialName annotation support

## Expected Output

After running `./gradlew :example:kspKotlin`, you should see:

```
example/build/generated/rust/
├── user.rs          # User + UserPreferences
├── theme.rs         # Theme enum
├── message.rs       # Message sealed class + variants
└── product.rs       # Product + ShoppingCart + CartItem
```

## Integration in Your Project

1. Copy the `processor/` module to your project
2. Add KSP plugin to your `build.gradle.kts`
3. Add processor as ksp dependency
4. Annotate models with `@Serializable`
5. Configure output directory and filters
6. Run `./gradlew kspKotlin`

## Common Issues

### Issue: No files generated

- Check that classes are annotated with `@Serializable`
- Verify package/file filters match your models
- Check KSP logs for errors

### Issue: Wrong output directory

- Verify `rust.output.dir` setting
- Check project build directory path

### Issue: Type mapping incorrect

- Review TypeMapper.kt for custom type mappings
- Add custom mappings as needed