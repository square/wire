package com.squareup.wire.gradle

import com.android.build.gradle.api.BaseVariant
import com.google.common.collect.Lists
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin which adds a Wire proto generation step to a project.
 * <p>
 * For the simplest use case of a single folder of protos, just apply the plugin and place the
 * protos in src/main/proto. If multiple folders or other compiler flags need to be specified, use
 * the "proto" extension on any sourceSet in the project. See {@link WireSourceSetExtension} for
 * supported configuration settings.
 */
class WirePlugin implements Plugin<Project> {
    void apply(Project project) {
        if (project.plugins.hasPlugin("com.android.application")) {
            applyAndroid(project,
                    (DomainObjectCollection<BaseVariant>) project.android.applicationVariants)
        } else if (project.plugins.hasPlugin("com.android.library")) {
            applyAndroid(project,
                    (DomainObjectCollection<BaseVariant>) project.android.libraryVariants)
        } else {
            throw new IllegalArgumentException(
                    "Wire plugin requires the Android plugin to be configured")
        }
    }

    private static void applyAndroid(Project project,
            DomainObjectCollection<BaseVariant> variants) {
        // Create a 'wire' extension on every source set.
        project.android.sourceSets.all { sourceSet ->
            sourceSet.extensions.create('wire', WireSourceSetExtension, project, sourceSet.name)
        }

        // Add Java proto generator tasks which compile all the protos in each variant.
        variants.all { variant ->
            List<WireSourceSetExtension> configurations =
                    Lists.newArrayListWithCapacity(variant.sourceSets.size())
            variant.sourceSets.each { sourceSet ->
                configurations.add((WireSourceSetExtension) sourceSet.extensions['wire'])
            }

            WireGeneratorTask task = project.tasks.create(
                    "generate${variant.name.capitalize()}WireProtos", WireGeneratorTask)
            task.configurations = configurations
            task.outputDir = project.file("${project.buildDir}/generated/proto/${variant.dirName}")
            variant.registerJavaGeneratingTask(task, task.outputDir)
        }
    }
}
