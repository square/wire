package com.squareup.wire.compiler;

import com.squareup.wire.compiler.parser.WireParser;
import com.squareup.wire.compiler.plugin.WirePlugin;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ServiceLoader;

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
 * <li>{@code file} &ndash; An optional list of proto files to parse.<p>If no proto files are
 * specified, every file in the specified directories will be used.</li>
 * </ul>
 * Each argument can be specified multiple times. For example, to specify multiple roots use:
 * {@code --root=com.example.Foo --root=com.example.Bar foobar.proto}
 * <p>
 * If no proto files are specified, every file in the specified (or default) path will be used.
 * <p>
 * All plugins which are on the classpath will be invoked. By default, the built-in Java plugin is
 * included and other plugins can be added and registered. See {@link WirePlugin the plugin docs}
 * for more info. Plugin arguments can be specified with {@code -Dname=value}. See the individual
 * plugin documentation for a list of its supported arguments.
 */
public final class Main {
  private static final String ARG_PATH = "--path=";
  private static final String ARG_ROOT = "--root=";
  private static final String ARG_ENUM_OPTION = "--enum_option=";
  private static final String ARG_NO_OPTIONS = "--no_options";
  private static final String ARG_PROPERTY = "-D";

  public static void main(String... args) throws IOException {
    FileSystem fs = FileSystems.getDefault();
    WireParser parser = WireParser.createWithFileSystem(fs);

    for (String arg : args) {
      if (arg.startsWith(ARG_PATH)) {
        String pathArg = arg.substring(ARG_PATH.length());
        parser.addDirectory(fs.getPath(pathArg));
      } else if (arg.startsWith(ARG_ROOT)) {
        String rootsArg = arg.substring(ARG_ROOT.length());
        parser.addTypeRoot(rootsArg);
      } else if (arg.startsWith(ARG_ENUM_OPTION)) {
        String enumOptionArg = arg.substring(ARG_ENUM_OPTION.length());
        parser.addEnumOption(enumOptionArg);
      } else if (arg.equals(ARG_NO_OPTIONS)) {
        parser.setNoOptions();
      } else if (arg.startsWith(ARG_PROPERTY)) {
        String[] parts = arg.substring(ARG_PROPERTY.length()).split("=", -1);
        System.setProperty(parts[0], parts[1]);
      } else {
        parser.addProto(fs.getPath(arg));
      }
    }

    System.out.print("Parsing proto files... ");
    WireParser.ParsedInput parsedInput = parser.parse();
    System.out.println("Done.");

    for (WirePlugin plugin : ServiceLoader.load(WirePlugin.class)) {
      System.out.print("Running " + plugin.getClass().getSimpleName() + "... ");
      plugin.generate(fs, parsedInput);
      System.out.println("Done.");
    }
  }

  private Main() {
    throw new AssertionError("No instances.");
  }
}
