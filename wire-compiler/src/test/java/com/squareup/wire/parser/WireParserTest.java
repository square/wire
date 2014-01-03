package com.squareup.wire.parser;

import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class WireParserTest {
  private InMemoryFilesystem fs;
  private WireParser parser;

  @Before public void setUp() {
    fs = new InMemoryFilesystem();
    parser = new WireParser(fs);
  }

  @Test public void fileIsNotValidDirectory() {
    fs.addFile("/foo/bar", "baz");
    parser.addDirectory(new File("/foo/bar"));

    try {
      parser.validateInputFiles();
      fail("File is not valid directory.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("\"/foo/bar\" is not a directory.");
    }
  }

  @Test public void directoryMustExist() {
    parser.addDirectory(new File("/foo/bar"));

    try {
      parser.validateInputFiles();
      fail("Root directory must exist.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Directory \"/foo/bar\" does not exist.");
    }
  }

  @Test public void directoryIsNotValidProto() {
    fs.addFile("/foo/bar/baz.txt", "");
    parser.addProto(new File("/foo/bar"));

    try {
      parser.validateInputFiles();
      fail("Directory is not valid proto file.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Proto \"/foo/bar\" is not a file.");
    }
  }

  @Test public void protoFileMustExist() {
    parser.addProto(new File("/foo/bar"));

    try {
      parser.validateInputFiles();
      fail("Proto file must exist.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Proto \"/foo/bar\" does not exist.");
    }
  }

  @Test public void typeMustBeValid() {
    try {
      parser.addTypeRoot(null);
      fail("Null is not a valid type.");
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("Type must not be null.");
    }
    try {
      parser.addTypeRoot("");
      fail("Empty string is not a valid type.");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Type must not be blank.");
    }
    try {
      parser.addTypeRoot("      ");
      fail("Blank string is not a valid type.");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Type must not be blank.");
    }
  }

  @Test public void specifiedRootsAreReturned() {
    parser.addDirectory(new File("/foo/bar"));
    parser.addDirectory(new File("/ping/pong"));

    Set<File> directories = parser.getOrFindDirectories();
    assertThat(directories).containsOnly(new File("/foo/bar"), new File("/ping/pong"));
  }

  @Test public void noSpecifiedRootsReturnsWorkingDir() {
    Set<File> directories = parser.getOrFindDirectories();
    assertThat(directories).containsOnly(new File(System.getProperty("user.dir")));
  }

  @Test public void specifiedProtosAreReturned() {
    parser.addProto(new File("/foo/bar.proto"));
    parser.addProto(new File("/foo/baz.proto"));

    Set<File> directories = ImmutableSet.of(new File("/foo"));
    Set<File> protos = parser.getOrFindProtos(directories);
    assertThat(protos).containsOnly(new File("/foo/bar.proto"), new File("/foo/baz.proto"));
  }

  @Test public void noSpecifiedProtosSearchesRoots() {
    fs.addFile("/foo/bar/one.proto", "one");
    fs.addFile("/foo/bar/two.proto", "two");
    fs.addFile("/ping/pong/three.proto", "three");

    Set<File> directories = ImmutableSet.of(new File("/"));
    Set<File> protos = parser.getOrFindProtos(directories);
    assertThat(protos).containsOnly( //
        new File("/foo/bar/one.proto"), //
        new File("/foo/bar/two.proto"), //
        new File("/ping/pong/three.proto"));
  }

  @Test public void directorySearchIgnoresNonProtos() {
    fs.addFile("/foo/bar/baz.txt", "one");
    fs.addFile("/ping/pong", "ball");
    fs.addFile("/kit/kat.proto.txt", "nom");

    Set<File> directories = ImmutableSet.of(new File("/"));
    Set<File> protos = parser.getOrFindProtos(directories);
    assertThat(protos).isEmpty();
  }

  @Test public void parseFindsDependencies() throws IOException {
    fs.addFile("/foo/bar/one.proto", "one");
    fs.addFile("/foo/bar/two.proto", "two");
    fs.addFile("/kit/kat/three.proto", "three");

    File proto = new File("/foo/bar/one.proto");
    Set<File> directories = ImmutableSet.of(new File("/foo/bar"), new File("/kit/kat"));

    File dependency1 = parser.resolveDependency(proto, directories, "two.proto");
    assertThat(dependency1).isEqualTo(new File("/foo/bar/two.proto"));
    File dependency2 = parser.resolveDependency(proto, directories, "three.proto");
    assertThat(dependency2).isEqualTo(new File("/kit/kat/three.proto"));
  }

  @Test public void collectAllTypesRecursesToNestedTypes() {
    String proto = ""
        + "package wire;"
        + ""
        + "message Person {"
        + "  enum PhoneType {}"
        + "  message PhoneNumber {}"
        + "}";

    ProtoFile protoFile = ProtoSchemaParser.parse("test.proto", proto);
    Set<ProtoFile> protos = ImmutableSet.of(protoFile);
    Set<String> types = WireParser.collectAllTypes(protos);
    assertThat(types).containsOnly( //
        "wire.Person", //
        "wire.Person.PhoneType", //
        "wire.Person.PhoneNumber");
  }

  @Test public void collectAllTypesFailsOnDuplicates() {
    String proto = ""
        + "package wire;"
        + ""
        + "message Message {}";
    ProtoFile file1 = ProtoSchemaParser.parse("test1.proto", proto);
    ProtoFile file2 = ProtoSchemaParser.parse("test2.proto", proto);
    Set<ProtoFile> files = ImmutableSet.of(file1, file2);

    try {
      WireParser.collectAllTypes(files);
      fail("Duplicate types are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate type wire.Message defined in test1.proto, test2.proto");
    }
  }
}
