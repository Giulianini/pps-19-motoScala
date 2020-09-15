import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer

plugins {
    java
    scala
    `java-library`
    application
    id(Libs.Plugins.scoverage) version Versions.scoverage
    id(Libs.Plugins.shadow) version Versions.shadow
}

group = Config.Project.project_group
version = Config.Project.project_version

repositories {
    jcenter()
}

dependencies {
    //JAVAFX
    implementation(Libs.JavaFx.jfoenix)
    implementation(Libs.JavaFx.ikonli_core)
    implementation(Libs.JavaFx.ikonli_javafx)
    implementation(Libs.JavaFx.ikonli_devicon)
    implementation(Libs.JavaFx.ikonli_material)
    implementation(Libs.JavaFx.ikonli_ionicons)
    implementation(Libs.JavaFx.enzo)
    //SCALA FX
    implementation(Libs.Scala.scalafx)
    implementation(Libs.Scala.scalafx_extras)
    //SCALA
    implementation(Libs.Scala.scala_library)
    implementation(Libs.Scala.scalatest_wordspec)
    implementation(Libs.Scala.scalatest_funspec)
    implementation(Libs.Scala.scalatest_mustmatchers)
    implementation(Libs.Scala.scalatest_shouldmatchers)
    implementation(Libs.Scala.scalatest_plus)
    implementation(Libs.Scala.scala_xml)
    //SCALA MODULES
    implementation(Libs.Scala.scala_collection_contrib)
    implementation(Libs.Scala.scala_parallel_collections)
    implementation(Libs.Scala.scala_async)
    //AKKA
    implementation(Libs.Akka.akka_actor)
    implementation(Libs.Akka.akka_remote)
    implementation(Libs.Akka.akka_cluster)
    implementation(Libs.Akka.akka_cluster_tools)
    implementation(Libs.Akka.akka_testkit)
    implementation(Libs.Akka.akka_slf4j)
    //LOGGERS
    implementation(Libs.Loggers.logback)
    //SERIALIZERS
    implementation(Libs.Serializers.kryo)
    //PARSERS
    implementation(Libs.Parsers.jackson_core)
    implementation(Libs.Parsers.jackson_annotation)
    implementation(Libs.Parsers.jackson_databind)
    implementation(Libs.Parsers.jackson_yaml)
}

application {
    mainClassName = Config.Project.mainClass
}

tasks.register<Jar>("fatjar") {
    archiveClassifier.set("fat")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.withType<ShadowJar> {
    val newTransformer = AppendingTransformer()
    newTransformer.resource = "reference.conf"
    transformers.add(newTransformer)
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
                "Main-Class" to "it.unibo.pps1920.motoscala.Main"
        ))
    }
}