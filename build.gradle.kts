import org.gradle.api.JavaVersion
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.NoSuchFileException

plugins {
  java
  `java-library`
  `maven-publish`
  id("io.papermc.paperweight.userdev") version "1.7.2"
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.1.1"
  id("xyz.jpenilla.run-paper") version "2.3.0"
  id("io.github.goooler.shadow") version "8.1.7"
}

val groupStringSeparator = "."
val kebabcaseStringSeparator = "-"
val snakecaseStringSeparator = "_"

val startPath = "src${File.separator}main${File.separator}java"

fun capitalizeFirstLetter(string: String): String {
  return string.first().uppercase() + string.drop(1)
}

fun kebabcase(normalString: String): String {
  return normalString.lowercase().replace(" ", kebabcaseStringSeparator)
}

fun snakecase(string: String): String {
  return string.lowercase().replace(Regex("$kebabcaseStringSeparator| "), snakecaseStringSeparator)
}

fun pascalcase(string: String): String {
  return string.split(Regex("$kebabcaseStringSeparator| "))
    .joinToString("") { capitalizeFirstLetter(it) }
}

fun replaceStringInFile(filePath: String, stringToReplace: String, replacementString: String) {
  val file = Paths.get(filePath)

  if (!Files.exists(file)) {
    throw NoSuchFileException(filePath)
  }

  val content = String(Files.readAllBytes(file))
  val updatedContent = content.replace(stringToReplace, replacementString)

  Files.write(file, updatedContent.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
}

fun replaceStringInDirectoryFiles(directory: File, stringToReplace: String, replacementString: String) {
  directory.walkTopDown().filter { it.isFile }.forEach { file ->
    replaceStringInFile(file.path, stringToReplace, replacementString)
    println("Replaced $stringToReplace with $replacementString in ${file.name}")
  }
}

fun renamePackageDirectories(oldGroup: String, newGroup: String) {
  val oldPackageDir = File(oldGroup)
  val newPackageDir = File(newGroup)

  if (oldPackageDir.exists()) {
    if (oldPackageDir.renameTo(newPackageDir)) {
      println("Renamed package directory from $oldGroup to $newGroup")
    } else {
      println("Failed to rename package directory from $oldGroup to $newGroup")
    }
  } else {
    println("Package directory $oldGroup does not exist")
  }
}

description = "Test plugin for paperweight-userdev"

val mainProjectAuthor = "Esoteric Slime"
val projectAuthors = listOfNotNull(mainProjectAuthor)

val topLevelDomain = "net"

val projectNameString = rootProject.name

group = "$topLevelDomain$groupStringSeparator${mainProjectAuthor.lowercase().replace(" ", snakecaseStringSeparator)}$groupStringSeparator${snakecase(projectNameString)}"
version = "0.0.4"

val buildDirectoryString = layout.buildDirectory.toString()

val projectGroupString = group.toString()
val projectVersionString = version.toString()

val javaVersion = 21
val javaVersionEnumMember = JavaVersion.valueOf("VERSION_$javaVersion")

val paperApiMinecraftVersion = "1.21"
val paperApiVersion = "$paperApiMinecraftVersion-R0.1-SNAPSHOT"

java {
  sourceCompatibility = javaVersionEnumMember
  targetCompatibility = javaVersionEnumMember
  toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

repositories {
  mavenCentral()
}

dependencies {
  paperweight.paperDevBundle(paperApiVersion)
  implementation("dev.jorel", "commandapi-bukkit-shade-mojang-mapped", "9.5.1")
  implementation("net.lingala.zip4j", "zip4j", "2.11.5")
}

tasks {
  build {
    dependsOn(shadowJar)
  }

  shadowJar {
    archiveFileName.set("$projectNameString-$projectVersionString${File.separator}jar")
  }

  compileJava {
    options.release.set(javaVersion)
  }

  javadoc {
    options.encoding = Charsets.UTF_8.name()
  }
}

tasks.register("renameProject") {
  doLast {
    val newName = project.findProperty("new-name")?.toString() ?: error("Please provide a new project name using -Pnew-name")
    val newAuthorName = project.findProperty("new-author-name")?.toString() ?: error("Please provide a new author name using -Pnew-author-name")
    val newTopLevelDomain = project.findProperty("new-top-level-domain")?.toString() ?: error("Please provide a new top level domain using -Pnew-top-level-domain")

    val newSnakecaseName = snakecase(newName)
    val newSnakecaseAuthorName = snakecase(newAuthorName)
    val newPascalcaseName = pascalcase(newName)

    val newGroupString = "$newTopLevelDomain$groupStringSeparator$newSnakecaseAuthorName$groupStringSeparator$newSnakecaseName"
    val newGroupPath = newGroupString.replace(groupStringSeparator, File.separator)

    val newMainClassName = newPascalcaseName
    val newMainClassFileName = "$newPascalcaseName${File.separator}java"

    val settingsFilePath = projectDir.resolve("settings.gradle.kts").toString()
    val buildFilePath = projectDir.resolve("build.gradle.kts").toString()
    val javaSourcePath = projectDir.resolve(startPath).toPath().toFile()
    val javaSourcePathString = javaSourcePath.toString()

    val currentProjectName = rootProject.name
    val currentGroupString = project.group.toString()
    val currentGroupPath = currentGroupString.replace(groupStringSeparator, File.separator)

    val currentMainClassName = pascalcase(currentProjectName)
    val currentMainClassFileName = "$currentMainClassName.java"

    val oldMainClassFilePath = projectDir.resolve(Paths.get(startPath, currentGroupPath, currentMainClassFileName).toFile())
    val newMainClassFilePath = projectDir.resolve(Paths.get(startPath, currentGroupPath, newMainClassFileName).toFile())

    println("Current main class file path: ${oldMainClassFilePath.absolutePath}")
    println("New main class file path: ${newMainClassFilePath.absolutePath}")

    val destinationDir = newMainClassFilePath.parentFile
    if (!destinationDir.exists()) {
      destinationDir.mkdirs()
    }

    if (oldMainClassFilePath.exists() && oldMainClassFilePath.renameTo(newMainClassFilePath)) {
      println("Successfully renamed main file from ${oldMainClassFilePath.absolutePath} to ${newMainClassFilePath.absolutePath}")
    } else {
      error("Failed to rename main file from ${oldMainClassFilePath.absolutePath} to ${newMainClassFilePath.absolutePath}")
    }

    Files.move(Paths.get(startPath, currentGroupPath, "/."), Paths.get(startPath, newGroupPath, "/."))

    replaceStringInDirectoryFiles(javaSourcePath, currentGroupString, newGroupString)
    replaceStringInDirectoryFiles(javaSourcePath, currentMainClassName, newMainClassName)

    replaceStringInFile(settingsFilePath, currentProjectName, kebabcase(newName))
    replaceStringInFile(buildFilePath, "val mainProjectAuthor = \"$mainProjectAuthor\"", "val mainProjectAuthor = \"$newAuthorName\"")
    replaceStringInFile(buildFilePath, "val topLevelDomain = \"$topLevelDomain\"", "val topLevelDomain = \"$newTopLevelDomain\"")

    val currentMainFilePath = javaSourcePath.resolve(currentGroupPath).resolve(currentMainFileNameWithExtension)
    val newMainFilePath = javaSourcePath.resolve(newGroupPath).resolve(newMainFileNameWithExtension)

    try {
      Files.move(currentMainFilePath.toPath(), newMainFilePath.toPath())
      println("Renamed file successfully")
    } catch (exception: IOException) {
      exception.printStackTrace()
      error("Failed to rename file")
    }

    // Ensure directories are renamed and other replacements are done similarly

    renamePackageDirectories("${javaSourcePathString}/${currentGroupPath}", "${javaSourcePathString}/${newGroupPath}")

    println("Renamed project to '$newName', author to '$newAuthorName', and top-level domain to '$newTopLevelDomain'")
  }
}

bukkitPluginYaml {
  authors = projectAuthors
  main = "$projectGroupString$groupStringSeparator${pascalcase(projectNameString)}"
  apiVersion = paperApiMinecraftVersion
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      groupId = projectGroupString
      artifactId = projectNameString
      version = projectVersionString
    }
  }
}

tasks.named("publishMavenJavaPublicationToMavenLocal") {
  dependsOn(tasks.named("build"))
}
