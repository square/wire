package com.squareup.wire.compiler;

import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.compiler.parser.WireParser;
import com.squareup.wire.compiler.plugin.WirePlugin;
import com.squareup.wire.compiler.plugin.java.WireJavaPlugin;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runs the parser and plugins.
 * <p>
 * Usage:
 * <pre>
 * java com.squareup.wire.compiler.Main [argument ..] [file ..]
 * </pre>
 * Optional arguments:
 * <ul>
 * <li>{@code --path} &ndash; Directory under which proto files reside. {@code include}
 * declarations will be resolved from these directories.<p>If no directories are specified, the
 * current working directory will be used.</li>
 * <li>{@code --root} &ndash; Type to include in the parsed data. If specified, only these types
 * and their dependencies will be included. This allows for filtering message-heavy proto files
 * such that only desired message types are generated.<p>If no types are specified,
 * every type in the specified proto files will be used.</li>
 * <li>{@code --plugin} &ndash; Fully-qualified class name of a plugin for code generation.<p>Each
 * plugin has arguments which you can specify with {@code -Dname=value}. See the individual plugin
 * documentation for a list of its supported arguments.<p>If no plugin is specified, the
 * {@link WireJavaPlugin built-in Wire plugin for Java} will be used.</li>
 * <li>{@code file} &ndash; An optional list of proto files to parse.<p>If no proto files are
 * specified, every file in the specified directories will be used.</li>
 * </ul>
 * Each argument can be specified multiple times. For example, to specify multiple roots use:
 * {@code --root=com.example.Foo --root=com.example.Bar foobar.proto}
 * <p>
 * If no proto files are specified, every file in the specified (or default) path will be used.
 */
public final class Main {
  private static final String ARG_PATH = "--path=";
  private static final String ARG_ROOT = "--root=";
  private static final String ARG_PLUGIN = "--plugin=";
  private static final String ARG_PROPERTY = "-D";

  public static void main(String... args) throws IOException {
    FileSystem fs = FileSystems.getDefault();
    WireParser parser = WireParser.createWithFileSystem(fs);
    Set<String> pluginClassNames = new LinkedHashSet<>();

    for (String arg : args) {
      if (arg.startsWith(ARG_PLUGIN)) {
        String pluginArg = arg.substring(ARG_PLUGIN.length());
        pluginClassNames.add(pluginArg);
      } else if (arg.startsWith(ARG_PATH)) {
        String pathArg = arg.substring(ARG_PATH.length());
        parser.addDirectory(fs.getPath(pathArg));
      } else if (arg.startsWith(ARG_ROOT)) {
        String rootsArg = arg.substring(ARG_ROOT.length());
        parser.addTypeRoot(rootsArg);
      } else if (arg.startsWith(ARG_PROPERTY)) {
        String[] parts = arg.substring(ARG_PROPERTY.length()).split("=", -1);
        System.setProperty(parts[0], parts[1]);
      } else {
        parser.addProto(fs.getPath(arg));
      }
    }

    Set<WirePlugin> plugins;
    if (pluginClassNames.isEmpty()) {
      plugins = ImmutableSet.<WirePlugin>of(new WireJavaPlugin());
    } else {
      plugins = loadPlugins(pluginClassNames);
    }

    Set<ProtoFile> data = parser.parse();

    for (WirePlugin plugin : plugins) {
      plugin.generate(fs, data);
    }
  }

  /** TODO tests. */
  private static Set<WirePlugin> loadPlugins(Set<String> pluginClassNames) {
    Set<WirePlugin> plugins = new LinkedHashSet<>();
    Set<String> pluginErrors = new LinkedHashSet<>();
    for (String pluginClassName : pluginClassNames) {
      try {
        Class<?> pluginClass = Class.forName(pluginClassName);
        WirePlugin plugin = (WirePlugin) pluginClass.newInstance();
        plugins.add(plugin);
      } catch (ClassNotFoundException e) {
        pluginErrors.add("Could not load " + pluginClassName + ": " + e.getMessage());
      } catch (InstantiationException | IllegalAccessException e) {
        pluginErrors.add("Could not instantiate " + pluginClassName + ": " + e.getMessage());
      }
    }
    if (!pluginErrors.isEmpty()) {
      StringBuilder builder = new StringBuilder("Problem loading plugins:\n\n");
      for (String pluginError : pluginErrors) {
        builder.append(" * ").append(pluginError).append('\n');
      }
      throw new IllegalStateException(builder.toString());
    }
    return plugins;
  }

  private Main() {
    throw new AssertionError("No instances.");
  }
}
