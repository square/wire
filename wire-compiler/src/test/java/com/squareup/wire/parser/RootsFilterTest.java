package com.squareup.wire.parser;

import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import java.util.Set;
import org.junit.Test;

import static com.squareup.wire.parser.ProtoQualifier.fullyQualifyProtos;
import static com.squareup.wire.parser.ProtoUtils.collectAllTypes;
import static org.fest.assertions.api.Assertions.assertThat;

public class RootsFilterTest {
  private static Set<String> filter(String proto, String... keep) {
    ProtoFile protoFile = ProtoSchemaParser.parse("test.proto", proto);
    Set<ProtoFile> protoFiles = ImmutableSet.of(protoFile);
    Set<String> allTypes = collectAllTypes(protoFiles);
    protoFiles = fullyQualifyProtos(protoFiles, allTypes);

    Set<String> roots = ImmutableSet.copyOf(keep);
    Set<ProtoFile> filtered = RootsFilter.filter(protoFiles, roots);
    return collectAllTypes(filtered);
  }

  @Test public void transitive() {
    // Newlines needed until this is fixed: https://github.com/square/protoparser/issues/46
    String proto = ""
        + "package wire;\n"
        + "\n"
        + "message A {\n"
        + "  optional B b = 1;\n"
        + "  optional D d = 2;\n"
        + "}\n"
        + "message B {\n"
        + "  optional C c = 1;\n"
        + "}\n"
        + "message C {}\n"
        + "message D {}\n";
    assertThat(filter(proto, "wire.A")) //
        .containsOnly("wire.A", "wire.B", "wire.C", "wire.D");
  }

  @Test public void child() {
    String proto = ""
        + "package wire;"
        + ""
        + "message A {"
        + "  optional B f = 1;"
        + "  optional C g = 2;"
        + ""
        + "  message B {}"
        + "}"
        + "enum C {}";
    assertThat(filter(proto, "wire.A")) //
        .containsOnly("wire.A", "wire.A.B", "wire.C");
  }

  @Test public void parents() {
    // Newlines needed until this is fixed: https://github.com/square/protoparser/issues/46
    String proto = ""
        + "package wire;\n"
        + "\n"
        + "message A {\n"
        + "  message B {}\n"
        + "}\n"
        + "message C {\n"
        + "  optional A.B ab = 1;\n"
        + "}\n";
    assertThat(filter(proto, "wire.C")) //
        .containsOnly("wire.A", "wire.A.B", "wire.C");
  }

  @Test public void none() {
    String proto = ""
        + "package wire;"
        + ""
        + "message A {}";
    assertThat(filter(proto, "wire.A")) //
        .containsOnly("wire.A");
  }
}
