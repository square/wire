package com.squareup.protoparser;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.protoparser.OptionElement.Kind.BOOLEAN;
import static com.squareup.protoparser.OptionElement.Kind.LIST;
import static com.squareup.protoparser.OptionElement.Kind.MAP;
import static com.squareup.protoparser.OptionElement.Kind.OPTION;
import static com.squareup.protoparser.OptionElement.Kind.STRING;
import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;

public class OptionElementTest {
  @Test public void nullNameThrows() {
    try {
      OptionElement.create(null, STRING, "Test");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void nullValueThrows() {
    try {
      OptionElement.create("test", STRING, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("value == null");
    }
  }

  @Test public void simpleToSchema() {
    OptionElement option = OptionElement.create("foo", STRING, "bar");
    String expected = "foo = \"bar\"";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void nestedToSchema() {
    OptionElement option =
        OptionElement.create("foo.boo", OPTION, OptionElement.create("bar", STRING, "baz"), true);
    String expected = "(foo.boo).bar = \"baz\"";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void listToSchema() {
    OptionElement option = OptionElement.create("foo", LIST,
        list(OptionElement.create("ping", STRING, "pong", true),
            OptionElement.create("kit", STRING, "kat")), true);
    String expected = ""
        + "(foo) = [\n"
        + "  (ping) = \"pong\",\n"
        + "  kit = \"kat\"\n"
        + "]";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void mapToSchema() {
    OptionElement option =
        OptionElement.create("foo", MAP, map("ping", "pong", "kit", list("kat", "kot")));
    String expected = ""
        + "foo = {\n"
        + "  ping: \"pong\",\n"
        + "  kit: [\n"
        + "    \"kat\",\n"
        + "    \"kot\"\n"
        + "  ]\n"
        + "}";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void booleanToSchema() {
    OptionElement option = OptionElement.create("foo", BOOLEAN, "false");
    String expected = "foo = false";
    assertThat(option.toSchema()).isEqualTo(expected);
  }

  @Test public void optionListToMap() {
    List<OptionElement> options = list( //
        OptionElement.create("foo", STRING, "bar"), //
        OptionElement.create("ping", LIST, list( //
            OptionElement.create("kit", STRING, "kat"), //
            OptionElement.create("tic", STRING, "tac"), //
            OptionElement.create("up", STRING, "down") //
        )), //
        OptionElement.create("wire", MAP, map( //
            "omar", "little", //
            "proposition", "joe" //
        )), //
        OptionElement.create("nested.option", OPTION, OptionElement.create("one", STRING, "two")), //
        OptionElement.create("nested.option", OPTION, OptionElement.create("three", STRING, "four")) //
    );
    Map<String, Object> optionMap = OptionElement.optionsAsMap(options);
    assertThat(optionMap).contains( //
        entry("foo", "bar"), //
        entry("ping", list( //
            OptionElement.create("kit", STRING, "kat"), //
            OptionElement.create("tic", STRING, "tac"), //
            OptionElement.create("up", STRING, "down") //
        )), //
        entry("wire", map( //
            "omar", "little", //
            "proposition", "joe" //
        )), //
        entry("nested.option", map( //
            "one", "two", //
            "three", "four" //
        )) //
    );
  }

  @Test public void findInList() {
    OptionElement one = OptionElement.create("one", STRING, "1");
    OptionElement two = OptionElement.create("two", STRING, "2");
    OptionElement three = OptionElement.create("three", STRING, "3");
    List<OptionElement> options = list(one, two, three);
    assertThat(OptionElement.findByName(options, "one")).isSameAs(one);
    assertThat(OptionElement.findByName(options, "two")).isSameAs(two);
    assertThat(OptionElement.findByName(options, "three")).isSameAs(three);
  }

  @Test public void findInListMissing() {
    OptionElement one = OptionElement.create("one", STRING, "1");
    OptionElement two = OptionElement.create("two", STRING, "2");
    List<OptionElement> options = list(one, two);
    assertThat(OptionElement.findByName(options, "three")).isNull();
  }

  @Test public void findInListDuplicate() {
    OptionElement one = OptionElement.create("one", STRING, "1");
    OptionElement two = OptionElement.create("two", STRING, "2");
    List<OptionElement> options = list(one, two, one);
    try {
      OptionElement.findByName(options, "one");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Multiple options match name: one");
    }
  }
}
