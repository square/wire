package com.squareup.wire.parser;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A {@link Filesystem} representation which exists only in memory. */
final class InMemoryFilesystem implements Filesystem {
  private static final String ROOT = "/";

  private final Folder root;

  public InMemoryFilesystem() {
    this.root = new Folder("", null);
  }

  /** Dump a hierarchical representation of this filesystem to {@link System#out}. */
  public void dump() {
    System.out.println(ROOT);
    root.dump("");
  }

  /** Add a file at the specified path. This will also add its parent dirs, if missing. */
  public void addFile(String path, String content) {
    File file = new File(path);
    addDirectory(file.getParent()).addFile(file.getName(), content);
  }

  /** Add a directory at the specified path. This will also add its parent dirs, if missing. */
  private Folder addDirectory(String path) {
    if (ROOT.equals(path)) {
      return root;
    }
    File file = new File(path);
    return addDirectory(file.getParent()).addDirectory(file.getName());
  }

  @Override public boolean exists(File file) {
    return isFile(file) || isDirectory(file);
  }

  @Override public boolean isFile(File file) {
    return fileOf(file) != null;
  }

  @Override public boolean isDirectory(File file) {
    return folderOf(file) != null;
  }

  @Override public File[] listFiles(File file) {
    if (!isDirectory(file)) {
      throw new IllegalArgumentException("File \"" + file + "\" is not a directory.");
    }
    Set<String> fileNames = folderOf(file).getFiles();
    File[] files = new File[fileNames.size()];
    int i = 0;
    for (String fileName : fileNames) {
      files[i++] = new File(file, fileName);
    }
    return files;
  }

  @Override public String contentsUtf8(File file) throws IOException {
    return fileOf(file);
  }

  private String fileOf(File file) {
    Folder folder = folderOf(file.getParentFile());
    return folder == null ? null : folder.getFile(file.getName());
  }

  private Folder folderOf(File file) {
    if (ROOT.equals(file.getAbsolutePath())) {
      return root;
    }
    Folder parent = folderOf(file.getParentFile());
    return parent != null ? parent.getDirectory(file.getName()) : null;
  }

  private static class Folder {
    private final Folder parent;
    private final String name;
    private final Map<String, String> files = new LinkedHashMap<String, String>();
    private final Map<String, Folder> directories = new LinkedHashMap<String, Folder>();

    private Folder(String name, Folder parent) {
      this.name = name;
      this.parent = parent;
    }

    void addFile(String name, String contents) {
      Preconditions.checkArgument(!files.containsKey(name),
          "File \"" + name + "\" already exists in " + getPath());
      Preconditions.checkArgument(!directories.containsKey(name),
          "Directory exists with same name \"" + name + "\" in " + getPath());

      files.put(name, contents);
    }

    String getFile(String name) {
      return files.get(name);
    }

    Set<String> getFiles() {
      return Sets.union(files.keySet(), directories.keySet());
    }

    Folder addDirectory(String name) {
      if (directories.containsKey(name)) {
        return directories.get(name);
      }
      Preconditions.checkArgument(!files.containsKey(name),
          "File exists with same name \"" + name + "\" in " + getPath());

      Folder directory = new Folder(name, this);
      directories.put(name, directory);
      return directory;
    }

    Folder getDirectory(String name) {
      return directories.get(name);
    }

    String getPath() {
      String path = "";
      for (Folder current = this; current != null; current = current.parent) {
        path = current.name + "/" + path;
      }
      return path;
    }

    public void dump(String indent) {
      int things = directories.size() + files.size();

      List<String> directoryNames = new ArrayList<String>(directories.keySet());
      Collections.sort(directoryNames);
      for (int i = 0, count = directoryNames.size(); i < count; i++) {
        String directory = directoryNames.get(i);
        System.out.print(indent);
        System.out.print(i < things - 1 ? '+' : '`');
        System.out.print("-");
        System.out.print(directory);
        System.out.println("/");
        Folder folder = directories.get(directory);
        folder.dump(indent + (i < things - 1 ? "|" : " ") + "  ");
      }

      List<String> fileNames = new ArrayList<String>(files.keySet());
      Collections.sort(fileNames);
      for (int i = 0, count = fileNames.size(); i < count; i++) {
        System.out.print(indent);
        System.out.print(i < things - 1 ? '|' : '`');
        System.out.print("-");
        System.out.println(fileNames.get(i));
      }
    }
  }
}
