package com.squareup.wire.cli;

import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.parser.WireParser;
import com.squareup.wire.plugin.WirePlugin;
import com.squareup.wire.plugin.java.WireJavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the parser and plugins.
 * <p>
 * Usage:
 * <pre>
 * java com.squareup.wire.cli.Main [argument ..] [--plugin=<plugin> [flags ..]] [file ..]
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
 * <li>{@code --plugin} &ndash; Fully-qualified class name of a plugin for code
 * generation.<p>Optional flags for the plugin can be specified after the argument. See the
 * individual plugin documentation for a list of its supported flags.<p>If no plugin is specified,
 * the {@link WireJavaPlugin built-in Wire plugin for Java} will be used.</li>
 * <li>{@code file} &ndash; An optional list of proto files to parse.<p>If no proto files are
 * specified, every file in the specified directories will be used.</li>
 * <li>All other "{@code --}"-prefixed arguments will be handed off to the corresponding
 * plugin.</li>
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

  public static void main(String... args) throws IOException {
    WireParser parser = new WireParser();
    Map<String, List<String>> plugins = new LinkedHashMap<String, List<String>>();
    List<String> pluginFlags = null;

    for (String arg : args) {
      if (arg.startsWith(ARG_PLUGIN)) {
        String pluginArg = arg.substring(ARG_PLUGIN.length());
        if (plugins.containsKey(pluginArg)) {
          throw new IllegalArgumentException("Duplicate plugin: " + pluginArg);
        }
        pluginFlags = new ArrayList<String>();
        plugins.put(pluginArg, pluginFlags);
      } else if (arg.startsWith(ARG_PATH)) {
        String pathArg = arg.substring(ARG_PATH.length());
        parser.addDirectory(new File(pathArg));
      } else if (arg.startsWith(ARG_ROOT)) {
        String rootsArg = arg.substring(ARG_ROOT.length());
        parser.addTypeRoot(rootsArg);
      } else if (!arg.startsWith("--")) {
        parser.addProto(new File(arg));
      } else if (pluginFlags == null) {
        throw new IllegalArgumentException("Unknown argument: " + arg);
      } else {
        pluginFlags.add(arg);
      }
    }

    Set<ProtoFile> data = parser.parse();

    if (plugins.isEmpty()) {
      new WireJavaPlugin().run(data);
      return;
    }

    Map<String, WirePlugin> pluginMap = loadPlugins(plugins.keySet());
    for (Map.Entry<String, WirePlugin> entry : pluginMap.entrySet()) {
      WirePlugin plugin = entry.getValue();
      plugin.parseArgs(plugins.get(entry.getKey()));
      plugin.run(data);
    }
  }

  /** TODO tests. */
  private static Map<String, WirePlugin> loadPlugins(Set<String> pluginClassNames) {
    Map<String, WirePlugin> plugins = new LinkedHashMap<String, WirePlugin>();
    Set<String> pluginErrors = new LinkedHashSet<String>();
    for (String pluginClassName : pluginClassNames) {
      try {
        Class<?> pluginClass = Class.forName(pluginClassName);
        WirePlugin plugin = (WirePlugin) pluginClass.newInstance();
        plugins.put(pluginClassName, plugin);
      } catch (ClassNotFoundException e) {
        pluginErrors.add("Could not load " + pluginClassName + ": " + e.getMessage());
      } catch (InstantiationException e) {
        pluginErrors.add("Could not instantiate " + pluginClassName + ": " + e.getMessage());
      } catch (IllegalAccessException e) {
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
