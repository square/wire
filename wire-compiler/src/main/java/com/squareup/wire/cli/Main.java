package com.squareup.wire.cli;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.parser.WireParser;
import com.squareup.wire.plugin.WireJavaPlugin;
import com.squareup.wire.plugin.WirePlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Runs the parser and plugins.
 * <p>
 * Usage:
 * <pre>
 * java com.squareup.wire.cli.Main [argument ..] [file ..]
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
 * plugin has a {@link WirePlugin#getArgumentPrefix() prefix} with which you can pass additional
 * arguments. See the individual plugin documentation for a list of its supported arguments.<p>If
 * no plugin is specified, the {@link WireJavaPlugin built-in Wire plugin for Java} will be
 * used.</li>
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
  private static final Pattern PLUGIN_PREFIX = Pattern.compile("^[a-z]+$");

  public static void main(String... args) throws IOException {
    WireParser parser = new WireParser();
    Set<String> pluginClassNames = new LinkedHashSet<String>();
    List<String> unusedArgs = new ArrayList<String>();

    for (String arg : args) {
      if (arg.startsWith(ARG_PLUGIN)) {
        String pluginArg = arg.substring(ARG_PLUGIN.length());
        pluginClassNames.add(pluginArg);
      } else if (arg.startsWith(ARG_PATH)) {
        String pathArg = arg.substring(ARG_PATH.length());
        parser.addDirectory(new File(pathArg));
      } else if (arg.startsWith(ARG_ROOT)) {
        String rootsArg = arg.substring(ARG_ROOT.length());
        parser.addTypeRoot(rootsArg);
      } else if (!arg.startsWith("--")) {
        parser.addProto(new File(arg));
      } else {
        unusedArgs.add(arg);
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
      plugin.generate(argsForPlugin(unusedArgs, plugin), data);
    }
  }

  /** TODO tests. */
  private static Set<WirePlugin> loadPlugins(Set<String> pluginClassNames) {
    Set<WirePlugin> plugins = new LinkedHashSet<WirePlugin>();
    Set<String> pluginPrefixes = new LinkedHashSet<String>();
    Set<String> pluginErrors = new LinkedHashSet<String>();
    for (String pluginClassName : pluginClassNames) {
      try {
        Class<?> pluginClass = Class.forName(pluginClassName);
        WirePlugin plugin = (WirePlugin) pluginClass.newInstance();

        String pluginPrefix = plugin.getArgumentPrefix();
        if (!PLUGIN_PREFIX.matcher(pluginPrefix).matches()) {
          pluginErrors.add("Invalid argument prefix \"" + pluginPrefix + "\": " + pluginClassName);
          continue;
        }
        if (pluginPrefixes.contains(pluginPrefix)) {
          pluginErrors.add("Argument prefix collision: " + pluginPrefix);
          continue;
        }
        pluginPrefixes.add(pluginPrefix);

        plugins.add(plugin);
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

  /** TODO tests. */
  static List<String> argsForPlugin(List<String> args, WirePlugin plugin) {
    final String argPrefix = "--" + plugin.getArgumentPrefix() + "-";
    return FluentIterable.from(args).filter(new Predicate<String>() {
      @Override public boolean apply(String arg) {
        return arg.startsWith(argPrefix);
      }
    }).transform(new Function<String, String>() {
      @Override public String apply(String arg) {
        return arg.substring(argPrefix.length());
      }
    }).toList();
  }

  private Main() {
    throw new AssertionError("No instances.");
  }
}
