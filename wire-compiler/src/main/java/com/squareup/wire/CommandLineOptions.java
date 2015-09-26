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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;

final class CommandLineOptions {
  public static final String PROTO_PATH_FLAG = "--proto_path=";
  public static final String JAVA_OUT_FLAG = "--java_out=";
  public static final String ROOTS_FLAG = "--roots=";
  public static final String REGISTRY_CLASS_FLAG = "--registry_class=";
  public static final String NO_OPTIONS_FLAG = "--no_options";
  public static final String ENUM_OPTIONS_FLAG = "--enum_options=";
  public static final String QUIET_FLAG = "--quiet";
  public static final String DRY_RUN_FLAG = "--dry_run";
  public static final String ANDROID = "--android";

  final List<String> protoPaths;
  final String javaOut;
  final List<String> roots;
  final String registryClass;
  final boolean emitOptions;
  final Set<String> enumOptions;
  final boolean quiet;
  final boolean dryRun;
  final boolean emitAndroid;

  CommandLineOptions(String protoPath, String javaOut, List<String> roots, String registryClass,
      boolean emitOptions, Set<String> enumOptions, boolean quiet, boolean dryRun,
      boolean emitAndroid) {
    this.protoPaths = singletonList(protoPath);
    this.javaOut = javaOut;
    this.roots = roots;
    this.registryClass = registryClass;
    this.emitOptions = emitOptions;
    this.enumOptions = enumOptions;
    this.quiet = quiet;
    this.dryRun = dryRun;
    this.emitAndroid = emitAndroid;
  }

  /**
   * Usage:
   *
   * <pre>
   * java WireCompiler --proto_path=&lt;path&gt; --java_out=&lt;path&gt;
   *     [--roots=&lt;message_name&gt;[,&lt;message_name&gt;...]]
   *     [--registry_class=&lt;class_name&gt;] [--no_options]
   *     [--enum_options=&lt;option_name&gt;[,&lt;option_name&gt;...]]
   *     [--service_factory=&lt;class_name&gt;]
   *     [--service_factory_opt=&lt;value&gt;] [--service_factory_opt=&lt;value&gt;]...]
   *     [--quiet] [--dry_run] [--android]
   * </pre>
   *
   * If the {@code --roots} flag is present, its argument must be a comma-separated list
   * of fully-qualified message or enum names. The output will be limited to those messages
   * and enums that are (transitive) dependencies of the listed names.  If you are using
   * {@code --service_factory} to generate an interface for a Service, your roots can also take the
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
   * <p>
   * If {@code --quiet} is specified, diagnostic messages to stdout are suppressed.
   * <p>
   * The {@code --dry_run} flag causes the compile to just emit the names of the source files that
   * would be generated to stdout.
   * <p>
   * The {@code --android} flag will cause all messages to implement the {@code Parcelable}
   * interface.
   */
  CommandLineOptions(String... args) throws WireException {
    this(Arrays.asList(args));
  }

  CommandLineOptions(List<String> args) throws WireException {
    List<String> roots = new ArrayList<>();
    boolean emitOptions = true;
    List<String> protoPaths = new ArrayList<>();
    String javaOut = null;
    String registryClass = null;
    List<String> enumOptionsList = new ArrayList<>();
    boolean quiet = false;
    boolean dryRun = false;
    boolean emitAndroid = false;

    for (String arg : args) {
      if (arg.startsWith(PROTO_PATH_FLAG)) {
        protoPaths.add(arg.substring(PROTO_PATH_FLAG.length()));
      } else if (arg.startsWith(JAVA_OUT_FLAG)) {
        javaOut = arg.substring(JAVA_OUT_FLAG.length());
      } else if (arg.startsWith(ROOTS_FLAG)) {
        roots.addAll(splitArg(arg, ROOTS_FLAG.length()));
      } else if (arg.startsWith(REGISTRY_CLASS_FLAG)) {
        registryClass = arg.substring(REGISTRY_CLASS_FLAG.length());
      } else if (arg.equals(NO_OPTIONS_FLAG)) {
        emitOptions = false;
      } else if (arg.startsWith(ENUM_OPTIONS_FLAG)) {
        enumOptionsList.addAll(splitArg(arg, ENUM_OPTIONS_FLAG.length()));
      } else if (arg.equals(QUIET_FLAG)) {
        quiet = true;
      } else if (arg.equals(DRY_RUN_FLAG)) {
        dryRun = true;
      } else if (arg.equals(ANDROID)) {
        emitAndroid = true;
      } else {
        throw new IllegalArgumentException("Unknown flag: " + arg);
      }
    }

    this.protoPaths = protoPaths;
    this.javaOut = javaOut;
    this.roots = roots;
    this.registryClass = registryClass;
    this.emitOptions = emitOptions;
    this.enumOptions = new LinkedHashSet<>(enumOptionsList);
    this.quiet = quiet;
    this.dryRun = dryRun;
    this.emitAndroid = emitAndroid;
  }

  private static List<String> splitArg(String arg, int flagLength) {
    return Arrays.asList(arg.substring(flagLength).split(","));
  }
}
