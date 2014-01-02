package com.squareup.wire.parser;

import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import java.util.Set;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class ProtoUtilsTest {
  @Test public void collectAllTypesRecursesToNestedTypes() {
    // Newlines needed until this is fixed: https://github.com/square/protoparser/issues/46
    String proto = ""
        + "package wire;\n"
        + "\n"
        + "message Person {\n"
        + "  enum PhoneType {}\n"
        + "  message PhoneNumber {}\n"
        + "}\n";

    ProtoFile protoFile = ProtoSchemaParser.parse("test.proto", proto);
    Set<ProtoFile> protos = ImmutableSet.of(protoFile);
    Set<String> types = ProtoUtils.collectAllTypes(protos);
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
      ProtoUtils.collectAllTypes(files);
      fail("Duplicate types are not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Duplicate type wire.Message defined in test1.proto, test2.proto");
    }
  }
}
