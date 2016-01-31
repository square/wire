package com.squareup.wire.gradle

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternSet

/** An extension defining a set of proto sources and Wire compiler options. */
class WireSourceSetExtension {
  private final Project project

  /** List of source folders containing protos. */
  private List<String> folders = Lists.newArrayList()

  /** Cached map of .proto files found, keyed by the containing source folder. */
  private Multimap<String, String> cachedFiles

  // Properties passed to the Wire compiler.
  private boolean noOptions
  private List<String> enumOptions = Lists.newArrayList()
  private List<String> protoPaths = Lists.newArrayList()
  private List<String> roots = Lists.newArrayList()
  private String serviceWriter
  private List<String> serviceWriterOpts = Lists.newArrayList()
  private String registryClass

  public WireSourceSetExtension(Project project, String sourceSetName) {
    this.project = project

    // Set a default srcDir matching the standard directory structure. Can always be overridden.
    srcDir("src/$sourceSetName/proto")
  }

  void srcDir(String dir) {
    folders.add(dir)
  }

  void srcDirs(String... dirs) {
    Collections.addAll(folders, dirs)
  }

  void setSrcDirs(Iterable<String> dirs) {
    folders = Lists.newArrayList(dirs)
  }

  void setProtoPaths(Iterable<String> paths) {
    protoPaths = Lists.newArrayList(paths)
  }

  Iterable<String> getProtoPaths() {
    return protoPaths
  }

  /** Return a map of .proto files found, keyed by the containing source folder. */
  public Multimap<String, String> getFiles() {
    if (cachedFiles != null) {
      return cachedFiles
    }

    PatternSet patternSet = new PatternSet().include("**/*.proto")
    def files = ArrayListMultimap.create()
    for (String folder : folders) {
      project.files(folder).getAsFileTree().matching(patternSet).visit { element ->
        if (!element.directory) {
          files.put(folder, element.relativePath.pathString)
        }
      }
    }
    cachedFiles = files
    return cachedFiles
  }

  void setNoOptions(boolean noOptions) {
    this.noOptions = noOptions
  }

  public boolean getNoOptions() {
    return noOptions
  }

  void setEnumOptions(Iterable<String> enumOptions) {
    this.enumOptions = Lists.newArrayList(enumOptions)
  }

  void enumOptions(String... enumOptions) {
    Collections.addAll(this.enumOptions, enumOptions)
  }

  public Collection<String> getEnumOptions() {
    return enumOptions
  }

  void setRoots(Iterable<String> roots) {
    this.roots = Lists.newArrayList(roots)
  }

  void roots(String... roots) {
    Collections.addAll(this.roots, roots)
  }

  public Collection<String> getRoots() {
    return roots
  }

  void setServiceWriter(String serviceWriter) {
    this.serviceWriter = serviceWriter
  }

  public String getServiceWriter() {
    return serviceWriter
  }

  void setRegistryClass(String registryClass) {
    this.registryClass = registryClass
  }

  public String getRegistryClass() {
    return registryClass
  }

  void setServiceWriterOpts(Iterable<String> serviceWriterOpts) {
    this.serviceWriterOpts = Lists.newArrayList(serviceWriterOpts)
  }

  void serviceWriterOpts(String... serviceWriterOpts) {
    Collections.addAll(this.serviceWriterOpts, serviceWriterOpts)
  }

  public Collection<String> getServiceWriterOpts() {
    return serviceWriterOpts
  }
}
