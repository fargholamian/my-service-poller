import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

//plugins {
//  java
//  application
//  id("com.github.johnrengelman.shadow") version "7.0.0"
//}

plugins {
  id("io.vertx.vertx-plugin") version "1.1.1"
}

group = "se.kry"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.2.1"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "se.kry.servicepooler.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

//application {
//  mainClass.set(launcherClassName)
//}

vertx {
  mainVerticle = "se.kry.servicepooler.MainVerticle";
}


dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web-client")
  implementation("io.vertx:vertx-web-validation")
  implementation("io.vertx:vertx-config")
  implementation("io.vertx:vertx-sql-client-templates")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-mysql-client")
  implementation("io.vertx:vertx-service-discovery")
  implementation("io.vertx:vertx-web-sstore-infinispan")
  implementation("io.vertx:vertx-auth-sql-client")
  implementation("io.vertx:vertx-rx-java3")
  implementation("io.vertx:vertx-sockjs-service-proxy")
  implementation("org.flywaydb:flyway-core:6.1.3")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
  implementation("mysql:mysql-connector-java:8.0.27")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--launcher-class=$launcherClassName")
}
