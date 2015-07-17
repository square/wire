/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import com.squareup.wire.java.ServiceFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

final class CommandLineOptions {
  public static final String PROTO_PATH_FLAG = "--proto_path=";
  public static final String JAVA_OUT_FLAG = "--java_out=";
  public static final String FILES_FLAG = "--files=";
  public static final String ROOTS_FLAG = "--roots=";
  public static final String REGISTRY_CLASS_FLAG = "--registry_class=";
  public static final String NO_OPTIONS_FLAG = "--no_options";
  public static final String ENUM_OPTIONS_FLAG = "--enum_options=";
  public static final String SERVICE_FACTORY_FLAG = "--service_factory=";
  public static final String SERVICE_FACTORY_OPT_FLAG = "--service_factory_opt=";
  public static final String QUIET_FLAG = "--quiet";
  public static final String DRY_RUN_FLAG = "--dry_run";

  final String protoPath;
  final File javaOut;
  final List<String> sourceFileNames;
  final List<String> roots;
  final String registryClass;
  final boolean emitOptions;
  final Set<String> enumOptions;
  final ServiceFactory serviceFactory;
  final List<String> serviceFactoryOptions;
  final boolean quiet;
  final boolean dryRun;

  CommandLineOptions(String protoPath, File javaOut,
      List<String> sourceFileNames, List<String> roots,
      String registryClass, boolean emitOptions,
      Set<String> enumOptions,
      ServiceFactory serviceFactory,
      List<String> serviceFactoryOptions,
      boolean quiet,
      boolean dryRun) {
    this.protoPath = protoPath;
    this.javaOut = javaOut;
    this.sourceFileNames = sourceFileNames;
    this.roots = roots;
    this.registryClass = registryClass;
    this.emitOptions = emitOptions;
    this.enumOptions = enumOptions;
    this.serviceFactory = serviceFactory;
    this.serviceFactoryOptions = serviceFactoryOptions;
    this.quiet = quiet;
    this.dryRun = dryRun;
  }

  /**
   * Usage:
   *
   * <pre>
   * java WireCompiler --proto_path=&lt;path&gt; --java_out=&lt;path&gt;
   *     [--files=&lt;protos.include&gt;] [--roots=&lt;message_name&gt;[,&lt;message_name&gt;...]]
   *     [--registry_class=&lt;class_name&gt;] [--no_options]
   *     [--enum_options=&lt;option_name&gt;[,&lt;option_name&gt;...]]
   *     [--service_factory=&lt;class_name&gt;]
   *     [--service_factory_opt=&lt;value&gt;] [--service_factory_opt=&lt;value&gt;]...]
   *     [--quiet] [--dry_run]
   *     [file [file...]]
   * </pre>
   *
   * If the {@code --roots} flag is present, its argument must be a comma-separated list
   * of fully-qualified message or enum names. The output will be limited to those messages
   * and enums that are (transitive) dependencies of the listed names.  If you are using
   * {@code --service_writer} to generate an interface for a Service, your roots can also take the
   * form 'fully.qualified.Service#MethodName` to limit what endpoints are generated.
   * <p>
   * If the {@code --registry_class} flag is present, its argument must be a Java class name. A
   * class with the given name will be generated, containing a constant list of all extension
   * classes generated during the compile. This list is suitable for passing to Wire's constructor
   * at runtime for constructing its internal extension registry.
   * <p>
   * Unless the {@code --no_options} flag is supplied, code will be emitted for options on messages
   * and fields.  The presence of options on a message will result in a static member named
   * "MESSAGE_OPTIONS", initialized with the options and their values.   The presence of options on
   * a field (other than the standard options "default", "deprecated", and "packed") will result in
   * a static member named "FIELD_OPTIONS_&lt;field name&gt;" in the generated code, initialized
   * with the field option values.
   * <p>
   * Regardless of the value of the {@code --no_options} flag, code will be emitted for all
   * enum value options listed in the {@code --enum_options} flag. The resulting code will contain
   * a public static field for each option used within a particular enum type.
   * </p>
   * <p>
   * If {@code --quiet} is specified, diagnostic messages to stdout are suppressed.
   * </p>
   * <p>
   * The {@code --dry_run} flag causes the compile to just emit the names of the source files that
   * would be generated to stdout.
   */
  CommandLineOptions(String... args) throws WireException {
    int index = 0;

    List<String> sourceFileNames = new ArrayList<>();
    List<String> serviceFactoryOptions = new ArrayList<>();
    List<String> roots = new ArrayList<>();
    boolean emitOptions = true;
    String protoPath = null;
    File javaOut = null;
    String registryClass = null;
    List<String> enumOptionsList = new ArrayList<>();
    ServiceFactory serviceFactory = null;
    boolean quiet = false;
    boolean dryRun = false;

    while (index < args.length) {
      if (args[index].startsWith(PROTO_PATH_FLAG)) {
        protoPath = args[index].substring(PROTO_PATH_FLAG.length());
      } else if (args[index].startsWith(JAVA_OUT_FLAG)) {
        javaOut = new File(args[index].substring(JAVA_OUT_FLAG.length()));
      } else if (args[index].startsWith(FILES_FLAG)) {
        File files = new File(args[index].substring(FILES_FLAG.length()));
        String[] fileNames;
        try {
          fileNames = new Scanner(files, "UTF-8").useDelimiter("\\A").next().split("\n");
        } catch (FileNotFoundException ex) {
          throw new WireException("Error processing argument " + args[index], ex);
        }
        sourceFileNames.addAll(Arrays.asList(fileNames));
      } else if (args[index].startsWith(ROOTS_FLAG)) {
        roots.addAll(splitArg(args[index], ROOTS_FLAG.length()));
      } else if (args[index].startsWith(REGISTRY_CLASS_FLAG)) {
        registryClass = args[index].substring(REGISTRY_CLASS_FLAG.length());
      } else if (args[index].equals(NO_OPTIONS_FLAG)) {
        emitOptions = false;
      } else if (args[index].startsWith(ENUM_OPTIONS_FLAG)) {
        enumOptionsList.addAll(splitArg(args[index], ENUM_OPTIONS_FLAG.length()));
      } else if (args[index].startsWith(SERVICE_FACTORY_FLAG)) {
        String serviceFactoryClassName = args[index].substring(SERVICE_FACTORY_FLAG.length());
        serviceFactory = loadServiceFactory(serviceFactoryClassName);
      } else if (args[index].startsWith(SERVICE_FACTORY_OPT_FLAG)) {
        serviceFactoryOptions.add(args[index].substring(SERVICE_FACTORY_OPT_FLAG.length()));
      } else if (args[index].startsWith(QUIET_FLAG)) {
        quiet = true;
      } else if (args[index].startsWith(DRY_RUN_FLAG)) {
        dryRun = true;
      } else {
        sourceFileNames.add(args[index]);
      }
      index++;
    }

    this.protoPath = protoPath;
    this.javaOut = javaOut;
    this.sourceFileNames = sourceFileNames;
    this.roots = roots;
    this.registryClass = registryClass;
    this.emitOptions = emitOptions;
    this.enumOptions = new LinkedHashSet<>(enumOptionsList);
    this.serviceFactory = serviceFactory;
    this.serviceFactoryOptions = serviceFactoryOptions;
    this.quiet = quiet;
    this.dryRun = dryRun;
  }

  private ServiceFactory loadServiceFactory(String className) throws WireException {
    try {
      Class<?> serviceFactoryClass = Class.forName(className);
      return (ServiceFactory) serviceFactoryClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new WireException(
          "Failed to load ServiceFactory: " + className, e);
    } catch (ClassCastException e) {
      throw new WireException(
          "Class " + className + " does not implement ServiceFactory interface.");
    } catch (InstantiationException e) {
      throw new WireException(
          "Failed to instantiate ServiceFactory: " + className, e);
    } catch (IllegalAccessException e) {
      throw new WireException(
          "Failed to access ServiceFactory: " + className, e);
    }
  }

  private static List<String> splitArg(String arg, int flagLength) {
    return Arrays.asList(arg.substring(flagLength).split(","));
  }

  public String protoPath() {
    String result = protoPath;
    if (result == null) {
      result = System.getProperty("user.dir");
      System.err.println(CommandLineOptions.PROTO_PATH_FLAG + " flag not specified, "
          + "using current dir " + result);
    }
    return result;
  }
}
