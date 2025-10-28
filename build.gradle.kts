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
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.net.URI

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.maven.publish)
}

allprojects {
  repositories {
    mavenCentral()
  }
}

allprojects {
  group = "dev.nemecec.rustmodel.ksp"
  version = "1.0.0"
}

dokka {
  moduleName.set("Rust object model generator KSP plugin")
}

subprojects {
  apply(plugin = "org.jetbrains.dokka")

  pluginManager.withPlugin("org.jetbrains.dokka") {
    configure<org.jetbrains.dokka.gradle.DokkaExtension> {
      dokkaSourceSets.configureEach {
        documentedVisibilities.set(
          setOf(
            VisibilityModifier.Public,
            VisibilityModifier.Protected
          )
        )
        reportUndocumented.set(false)
        jdkVersion.set(17)

        sourceLink {
          localDirectory.set(rootProject.projectDir)
          remoteUrl.set(URI("https://github.com/nemecec/rustmodel-ksp/tree/main/"))
          remoteLineSuffix.set("#L")
        }
      }
    }
  }

  // Don't attempt to sign anything if we don't have an in-memory key. Otherwise, the 'build' task
  // triggers 'signJsPublication' even when we aren't publishing (and so don't have signing keys).
  tasks.withType<Sign>().configureEach {
    enabled = project.findProperty("signingInMemoryKey") != null
  }

  val javaVersion = JavaVersion.VERSION_17

  tasks.withType(JavaCompile::class.java).configureEach {
    sourceCompatibility = javaVersion.toString()
    targetCompatibility = javaVersion.toString()
  }

  tasks.withType(KotlinJvmCompile::class.java).configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
    }
  }

  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
      repositories {
        maven {
          name = "testMaven"
          url = rootProject.layout.buildDirectory.dir("testMaven").get().asFile.toURI()
        }
      }
    }
    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
      coordinates(group.toString(), name, version.toString())
      pom {
        description.set("Rust object model generator KSP plugin")
        name.set(project.name)
        url.set("https://github.com/nemecec/rustmodel-ksp")
        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }
        developers {
          developer {
            id.set("nemecec")
            name.set("Neeme Praks")
            url.set("https://github.com/nemecec/")
          }
        }
        scm {
          url.set("https://github.com/nemecec/rustmodel-ksp")
          connection.set("scm:git:https://github.com/nemecec/rustmodel-ksp.git")
          developerConnection.set("scm:git:ssh://git@github.com/nemecec/rustmodel-ksp.git")
        }
      }
    }
  }
}

tasks {
  wrapper {
    distributionType = Wrapper.DistributionType.ALL
  }
}