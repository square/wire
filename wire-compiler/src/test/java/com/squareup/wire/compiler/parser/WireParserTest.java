package com.squareup.wire.compiler.parser;

import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class WireParserTest {
  @Rule public FileSystemRule fileSystemRule = new FileSystemRule();

  private FileSystem fs;
  private WireParser parser;

  @Before public void setUp() {
    fs = fileSystemRule.get();
    parser = WireParser.createWithFileSystem(fs);
  }

  @Test public void fileIsNotValidDirectory() throws IOException {
    addFile(fs.getPath("/foo/bar"), "baz");
    parser.addDirectory(fs.getPath("/foo/bar"));

    try {
      parser.validateInputFiles();
      fail("File is not valid directory.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("\"/foo/bar\" is not a directory.");
    }
  }

  @Test public void directoryMustExist() {
    parser.addDirectory(fs.getPath("/foo/bar/"));

    try {
      parser.validateInputFiles();
      fail("Root directory must exist.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Directory \"/foo/bar\" does not exist.");
    }
  }

  @Test public void directoryIsNotValidProto() throws IOException {
    addFile(fs.getPath("/foo/bar/baz.txt"), "");
    parser.addProto(fs.getPath("/foo/bar"));

    try {
      parser.validateInputFiles();
      fail("Directory is not valid proto file.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Proto \"/foo/bar\" is not a file.");
    }
  }

  @Test public void protoFileMustExist() {
    parser.addProto(fs.getPath("/foo/bar"));

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
    parser.addDirectory(fs.getPath("/foo/bar"));
    parser.addDirectory(fs.getPath("/ping/pong"));

    Set<Path> directories = parser.getOrFindDirectories();
    assertThat(directories).containsOnly(fs.getPath("/foo/bar"), fs.getPath("/ping/pong"));
  }

  @Test public void noSpecifiedRootsReturnsWorkingDir() {
    Set<Path> directories = parser.getOrFindDirectories();
    assertThat(directories).containsOnly(fs.getPath("."));
  }

  @Test public void specifiedProtosAreReturned() throws IOException {
    parser.addProto(fs.getPath("/foo/bar.proto"));
    parser.addProto(fs.getPath("/foo/baz.proto"));

    Set<Path> directories = ImmutableSet.of(fs.getPath("/foo"));
    Set<Path> protos = parser.getOrFindProtos(directories);
    assertThat(protos).containsOnly(fs.getPath("/foo/bar.proto"), fs.getPath("/foo/baz.proto"));
  }

  @Test public void noSpecifiedProtosSearchesRoots() throws IOException {
    addFile(fs.getPath("/foo/bar/one.proto"), "one");
    addFile(fs.getPath("/foo/bar/two.proto"), "two");
    addFile(fs.getPath("/ping/pong/three.proto"), "three");

    Set<Path> directories = ImmutableSet.of(fs.getPath("/"));
    Set<Path> protos = parser.getOrFindProtos(directories);
    assertThat(protos).containsOnly( //
        fs.getPath("/foo/bar/one.proto"), //
        fs.getPath("/foo/bar/two.proto"), //
        fs.getPath("/ping/pong/three.proto"));
  }

  @Test public void directorySearchIgnoresNonProtos() throws IOException {
    addFile(fs.getPath("/foo/bar", "baz.txt"), "one");
    addFile(fs.getPath("/ping/pong"), "ball");
    addFile(fs.getPath("/kit/kat.proto.txt"), "nom");

    Set<Path> directories = ImmutableSet.of(fs.getPath("/"));
    Set<Path> protos = parser.getOrFindProtos(directories);
    assertThat(protos).isEmpty();
  }

  @Test public void parseFindsDependencies() throws IOException {
    addFile(fs.getPath("/foo/bar/one.proto"), "one");
    addFile(fs.getPath("/foo/bar/two.proto"), "two");
    addFile(fs.getPath("/kit/kat/three.proto"), "three");

    Path proto = fs.getPath("/foo/bar/one.proto");
    Set<Path> directories = ImmutableSet.of(fs.getPath("/foo/bar/"), fs.getPath("/kit/kat/"));

    Path dependency1 = parser.resolveDependency(proto, directories, "two.proto");
    assertThat((Comparable<Path>) dependency1).isEqualTo(fs.getPath("/foo/bar/two.proto"));
    Path dependency2 = parser.resolveDependency(proto, directories, "three.proto");
    assertThat((Comparable<Path>) dependency2).isEqualTo(fs.getPath("/kit/kat/three.proto"));
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

  private void addFile(Path path, String content) throws IOException {
    Files.createDirectories(path.getParent());
    Files.write(path, content.getBytes(UTF_8));
  }
}
