package com.squareup.wire.compiler.parser;

import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import java.util.Set;
import org.junit.Test;

import static com.squareup.wire.compiler.parser.ProtoQualifier.fullyQualifyProtos;
import static com.squareup.wire.compiler.parser.WireParser.collectAllTypes;
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
    String proto = ""
        + "package wire;"
        + ""
        + "message A {"
        + "  optional B b = 1;"
        + "  optional D d = 2;"
        + "}"
        + "message B {"
        + "  optional C c = 1;"
        + "}"
        + "message C {}"
        + "message D {}";
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
    String proto = ""
        + "package wire;"
        + ""
        + "message A {"
        + "  message B {}"
        + "}"
        + "message C {"
        + "  optional A.B ab = 1;"
        + "}";
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
