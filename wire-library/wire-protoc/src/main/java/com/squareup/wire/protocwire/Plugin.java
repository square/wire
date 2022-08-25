package com.squareup.wire.protocwire;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Front-end for protoc code generator plugins written in Java.
 * <p>
 * To implement a protoc plugin in Java, simply write an implementation of
 * {@link CodeGenerator}, then create a main() method like:
 * <pre><code>
 *   public static void main(String[] args) {
 *     Plugin.run(new MyCodeGenerator());
 *   }
 * </code></pre>
 * To get protoc to use the plugin, you'll have to create a launcher script:
 * <pre><code>
 *   #!/bin/sh
 *   cd `dirname $0`
 *   exec java -jar myplugin.jar
 * </code></pre>
 * On Windows, if it lives in the same folder as the plugin's JAR, it will
 * probably look like:
 * <pre><code>
 *   {@literal @}echo off
 *   java -jar "%~dp0\myplugin.jar"
 *   exit %errorlevel%
 * </code></pre>
 * You'll then have to do one of the following:
 * <ul>
 *   <li>Place the plugin binary somewhere in the {@code PATH} and give it the
 *       name {@code protoc-gen-NAME} (replacing {@code NAME} with the name of
 *       your plugin). If you then invoke protoc with the parameter
 *       {@code --NAME_out=OUT_DIR} (again, replace {@code NAME} with your
 *       plugin's name), protoc will invoke your plugin to generate the output,
 *       which will be placed in {@code OUT_DIR}.
 *   <li>Place the plugin binary anywhere, with any name, and pass the
 *       {@code --plugin} parameter to protoc to direct it to your plugin like
 *       so:
 *       <pre>
 *   protoc --plugin=protoc-gen-NAME=path/to/myscript --NAME_out=OUT_DIR
 *       </pre>
 *       On Windows, make sure to include the {@code .bat} suffix:
 *       <pre>
 *   protoc --plugin=protoc-gen-NAME=path/to/myscript.bat --NAME_out=OUT_DIR
 *       </pre>
 * </ul>
 *
 * @author t.broyer@ltgt.net Thomas Broyer
 * <br>Based on the initial work of:
 * @author kenton@google.com Kenton Varda
 */
public final class Plugin {
  /**
   * Thrown when something went wrong in the plugin infrastructure.
   * <p>
   * This is an unrecoverable error. You shouldn't handle it.
   */
  public static class PluginException extends RuntimeException {
    private static final long serialVersionUID = 4028115971354639383L;

    PluginException(String message) {
      super(message);
    }

    PluginException(String message, Throwable cause) {
      super(message, cause);
    }

    PluginException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Provides access to the input and output streams used to communicate with
   * protoc.
   *
   * @see DefaultEnvironment
   */
  public interface Environment {
    /**
     * Returns the input stream to read the protoc code generation request
     * from.
     */
    InputStream getInputStream();

    /**
     * Returns the output stream to write the code generation response to.
     */
    OutputStream getOutputStream();
  }

  /**
   * An {@link Environment} giving access to the "standard" input and output
   * streams.
   */
  public static class DefaultEnvironment implements Environment {

    @Override
    public InputStream getInputStream() {
      return System.in;
    }

    @Override
    public OutputStream getOutputStream() {
      return System.out;
    }
  }

  public static class DescriptorSource {
    private final Map<String, FileDescriptor> files;
    private DescriptorSource(Map<String, FileDescriptor> files) {
      this.files = files;
    }

    public Descriptors.Descriptor findMessageTypeByName(String fullName) {
      for (FileDescriptor fd : files.values()) {
        Descriptors.Descriptor ret = fd.findMessageTypeByName(fullName);
        if (ret != null) {
          return ret;
        }
      }
      return null;
    }
  }

  /**
   * Streams generated files from a {@link CodeGenerator} to a {@link CodedOutputStream}.
   */
  public static class Response {

    private final CodedOutputStream output;

    Response(CodedOutputStream output) {
      this.output = output;
    }

    public void addFile(String filename, String content) {
      CodeGeneratorResponse.File.Builder file =
        CodeGeneratorResponse.File.newBuilder();
      file.setName(filename);
      file.setContent(content);
      try {
        // Protocol format guarantees that concatenated messages are parsed as
        // if they had been merged in a single message prior to being serialized.
        CodeGeneratorResponse.newBuilder().addFile(file).build().writeTo(output);
        output.flush();
      } catch (IOException e) {
        throw new PluginException("Error writing to stdout.", e);
      }
    }
  }

  private Plugin() {
  }

  /**
   * Runs the given code generator, reading the request from {@link System#in}
   * and writing the response to {@link System#out}.
   * <p>
   * This is equivalent to {@code run(generator, new DefaultEnvironment())}.
   *
   * @see #run(CodeGenerator, Environment)
   */
  public static void run(CodeGenerator generator) throws PluginException {
    run(generator, new DefaultEnvironment());
  }

  /**
   * Runs the given code generator in the given environment.
   */
  public static void run(CodeGenerator generator, Environment environment) {
    CodeGeneratorRequest request;
    ByteString rawRequest;
    try {
      rawRequest = ByteString.readFrom(environment.getInputStream());
      request = CodeGeneratorRequest.parseFrom(rawRequest);
    } catch (IOException e) {
      throw new PluginException("protoc sent unparseable request to plugin.", e);
    }

    Map<String, FileDescriptor> files = asDescriptors(request.getProtoFileList());
    ExtensionRegistry reg = createExtensionRegistry(files.values());

    // now we must *re-parse* the request, but this time we can properly parse any
    // custom options therein
    try {
      request = CodeGeneratorRequest.parseFrom(rawRequest, reg);
    } catch (IOException e) {
      throw new PluginException("protoc sent unparseable request to plugin.", e);
    }
    files = asDescriptors(request.getProtoFileList());

    CodedOutputStream output = CodedOutputStream.newInstance(
      environment.getOutputStream());
    try {
      // go ahead and write response preamble
      CodeGeneratorResponse.newBuilder()
        // add more here as more features are introduced and then supported
        .setSupportedFeatures(
          toFeatureBitmask(CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL))
        .build()
        .writeTo(output);
    } catch (IOException e) {
      throw new PluginException("protoc sent unparseable request to plugin.", e);
    }
    generator.generate(request, new DescriptorSource(files), new Response(output));
  }

  private static long toFeatureBitmask(CodeGeneratorResponse.Feature... features) {
    long result = 0;
    for (CodeGeneratorResponse.Feature f : features) {
      result |= f.getNumber();
    }
    return result;
  }

  private static ExtensionRegistry createExtensionRegistry(Collection<FileDescriptor> files) {
    ExtensionRegistry reg = ExtensionRegistry.newInstance();
    for (FileDescriptor fd : files) {
      addAllExtensionsFromFile(reg, fd);
    }
    return reg;
  }

  private static void addAllExtensionsFromFile(ExtensionRegistry reg, FileDescriptor fd) {
    for (Descriptors.FieldDescriptor ext : fd.getExtensions()) {
      if (ext.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
        reg.add(ext, DynamicMessage.newBuilder(ext.getMessageType()).build());
      } else {
        reg.add(ext);
      }
    }
    for (Descriptors.Descriptor msg : fd.getMessageTypes()) {
      addAllExtensionsFromMessage(reg, msg);
    }
  }

  private static void addAllExtensionsFromMessage(ExtensionRegistry reg,
                                                  Descriptors.Descriptor msg) {
    for (Descriptors.FieldDescriptor ext : msg.getExtensions()) {
      if (ext.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
        reg.add(ext, DynamicMessage.newBuilder(ext.getMessageType()).build());
      } else {
        reg.add(ext);
      }
    }
    for (Descriptors.Descriptor nested : msg.getNestedTypes()) {
      addAllExtensionsFromMessage(reg, nested);
    }
  }

  /**
   * Parse the request's proto files and returns the list of parsed descriptors
   * corresponding only to the files to generate (i.e. dependencies not listed
   * explicitly are not included in the returned list).
   */
  private static Map<String, FileDescriptor> asDescriptors(List<FileDescriptorProto> protoFiles)
    throws PluginException {
    Map<String, FileDescriptor> filesByName = new HashMap<>(protoFiles.size());
    for (FileDescriptorProto protoFile : protoFiles) {
      FileDescriptor[] dependencies =
        new FileDescriptor[protoFile.getDependencyCount()];
      for (int i = 0, l = protoFile.getDependencyCount(); i < l; i++) {
        FileDescriptor dependency = filesByName.get(
          protoFile.getDependency(i));
        if (dependency == null) {
          throw new PluginException("protoc asked plugin to generate a file "
             + "but did not provide a descriptor for a dependency (or "
             + "provided it after the file that depends on it): "
            + protoFile.getDependency(i));
        }
        dependencies[i] = dependency;
      }
      try {
        filesByName.put(protoFile.getName(), FileDescriptor.buildFrom(
          protoFile, dependencies));
      } catch (DescriptorValidationException e) {
        throw new PluginException(e);
      }
    }

    return filesByName;
  }
}
