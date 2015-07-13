package com.squareup.protoparser;

import com.squareup.protoparser.DataType.MapType;
import com.squareup.protoparser.DataType.NamedType;
import org.junit.Test;

import static com.squareup.protoparser.DataType.ScalarType.ANY;
import static com.squareup.protoparser.DataType.ScalarType.BOOL;
import static com.squareup.protoparser.DataType.ScalarType.BYTES;
import static com.squareup.protoparser.DataType.ScalarType.DOUBLE;
import static com.squareup.protoparser.DataType.ScalarType.FIXED32;
import static com.squareup.protoparser.DataType.ScalarType.FIXED64;
import static com.squareup.protoparser.DataType.ScalarType.FLOAT;
import static com.squareup.protoparser.DataType.ScalarType.INT32;
import static com.squareup.protoparser.DataType.ScalarType.INT64;
import static com.squareup.protoparser.DataType.ScalarType.SFIXED32;
import static com.squareup.protoparser.DataType.ScalarType.SFIXED64;
import static com.squareup.protoparser.DataType.ScalarType.SINT32;
import static com.squareup.protoparser.DataType.ScalarType.SINT64;
import static com.squareup.protoparser.DataType.ScalarType.STRING;
import static com.squareup.protoparser.DataType.ScalarType.UINT32;
import static com.squareup.protoparser.DataType.ScalarType.UINT64;
import static org.assertj.core.api.Assertions.assertThat;

public final class DataTypeTest {
  @Test public void scalarToString() {
    assertThat(ANY.toString()).isEqualTo("any");
    assertThat(BOOL.toString()).isEqualTo("bool");
    assertThat(BYTES.toString()).isEqualTo("bytes");
    assertThat(DOUBLE.toString()).isEqualTo("double");
    assertThat(FLOAT.toString()).isEqualTo("float");
    assertThat(FIXED32.toString()).isEqualTo("fixed32");
    assertThat(FIXED64.toString()).isEqualTo("fixed64");
    assertThat(INT32.toString()).isEqualTo("int32");
    assertThat(INT64.toString()).isEqualTo("int64");
    assertThat(SFIXED32.toString()).isEqualTo("sfixed32");
    assertThat(SFIXED64.toString()).isEqualTo("sfixed64");
    assertThat(SINT32.toString()).isEqualTo("sint32");
    assertThat(SINT64.toString()).isEqualTo("sint64");
    assertThat(STRING.toString()).isEqualTo("string");
    assertThat(UINT32.toString()).isEqualTo("uint32");
    assertThat(UINT64.toString()).isEqualTo("uint64");
  }

  @Test public void mapToString() {
    assertThat(MapType.create(STRING, STRING).toString()).isEqualTo("map<string, string>");
  }

  @Test public void namedToString() {
    assertThat(NamedType.create("test").toString()).isEqualTo("test");
    assertThat(NamedType.create("nested.nested").toString()).isEqualTo("nested.nested");
  }
}
