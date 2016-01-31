package com.squareup.wire.gradle

import com.android.build.gradle.api.BaseVariant
import com.google.common.collect.Lists
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Plugin which adds a Wire proto generation step to a project.
 * <p>
 * For the simplest use case of a single folder of protos, just apply the plugin and place the
 * protos in src/main/proto. If multiple folders or other compiler flags need to be specified, use
 * the "proto" extension on any sourceSet in the project. See {@link WireSourceSetExtension} for
 * supported configuration settings.
 */
class WirePlugin implements Plugin<Project> {
  @Override public void apply(Project project) {
    if (project.plugins.hasPlugin("com.android.application")) {
      applyAndroid(project,
          (DomainObjectCollection<BaseVariant>) project.android.applicationVariants)
    } else if (project.plugins.hasPlugin("com.android.library")) {
      applyAndroid(project,
          (DomainObjectCollection<BaseVariant>) project.android.libraryVariants)
    } else if (project.plugins.hasPlugin("org.gradle.java")) {
      applyJava(project)
    } else {
      throw new IllegalArgumentException(
          "Wire plugin requires the Android or Java plugin to be configured")
    }
  }

  private static void applyAndroid(Project project,
      DomainObjectCollection<BaseVariant> variants) {
    // Create a 'wire' extension on every source set.
    project.android.sourceSets.all { sourceSet ->
      addExtensionToSourceSet(project, sourceSet)
    }

    // Add Java proto generator tasks which compile all the protos in each variant.
    variants.all { variant ->
      WireGeneratorTask task =
          createGeneratorTask(project, variant.name, variant.dirName, variant.sourceSets)
      variant.registerJavaGeneratingTask(task, task.outputDir)
    }
  }

  private static void applyJava(Project project) {
    project.sourceSets.all { sourceSet ->
      // Create a 'wire' extension on this source set.
      addExtensionToSourceSet(project, sourceSet)

      // Add Java proto generator task which compiles all the protos in this source set.
      String sourceSetName = (String) sourceSet.name
      String taskName = "main".equals(sourceSetName) ? "" : sourceSetName
      WireGeneratorTask task =
          createGeneratorTask(project, taskName, sourceSetName, [sourceSet])
      Task classesTask = project.tasks.getByName(taskName.isEmpty() ? "classes" : "${taskName}Classes")
      classesTask.mustRunAfter task
      JavaCompile compileTask =
          (JavaCompile) project.tasks.getByName("compile${taskName.capitalize()}Java")
      compileTask.source += project.fileTree(task.outputDir)
      compileTask.dependsOn(task)
    }
  }

  private static void addExtensionToSourceSet(Project project, def sourceSet) {
    sourceSet.extensions.create('wire', WireSourceSetExtension, project, sourceSet.name)
  }

  private static WireGeneratorTask createGeneratorTask(Project project, String name,
      String dirName, Collection<?> sourceSets) {
    List<WireSourceSetExtension> configurations =
        Lists.newArrayListWithCapacity(sourceSets.size())
    sourceSets.each { sourceSet ->
      configurations.add((WireSourceSetExtension) sourceSet.extensions['wire'])
    }

    WireGeneratorTask task = project.tasks.create(
        "generate${name.capitalize()}WireProtos", WireGeneratorTask)
    task.configurations = configurations
    task.outputDir = project.file("${project.buildDir}/generated/source/proto/${dirName}")
    return task
  }
}
