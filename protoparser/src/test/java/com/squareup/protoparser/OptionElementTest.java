package com.squareup.protoparser;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;

public class OptionElementTest {
  @Test public void nullNameThrows() {
    try {
      OptionElement.create(null, "Test");
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name == null");
    }
  }

  @Test public void nullValueThrows() {
    try {
      OptionElement.create("test", null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("value == null");
    }
  }

  @Test public void simpleToString() {
    OptionElement option = OptionElement.create("foo", "bar");
    String expected = "foo = \"bar\"";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void nestedToString() {
    OptionElement option =
        OptionElement.create("foo.boo", OptionElement.create("bar", "baz"), true);
    String expected = "(foo.boo).bar = \"baz\"";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void listToString() {
    OptionElement option = OptionElement.create("foo",
        list(OptionElement.create("ping", "pong", true), OptionElement.create("kit", "kat")), true);
    String expected = ""
        + "(foo) = [\n"
        + "  (ping) = \"pong\",\n"
        + "  kit = \"kat\"\n"
        + "]";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void booleanToString() {
    OptionElement option = OptionElement.create("foo", false);
    String expected = "foo = false";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void optionListToMap() {
    List<OptionElement> options = list( //
        OptionElement.create("foo", "bar"), //
        OptionElement.create("ping", list( //
            OptionElement.create("kit", "kat"), //
            OptionElement.create("tic", "tac"), //
            OptionElement.create("up", "down") //
        )), //
        OptionElement.create("wire", map( //
            "omar", "little", //
            "proposition", "joe" //
        )), //
        OptionElement.create("nested.option", OptionElement.create("one", "two")), //
        OptionElement.create("nested.option", OptionElement.create("three", "four")) //
    );
    Map<String, Object> optionMap = OptionElement.optionsAsMap(options);
    assertThat(optionMap).contains( //
        entry("foo", "bar"), //
        entry("ping", list( //
            OptionElement.create("kit", "kat"), //
            OptionElement.create("tic", "tac"), //
            OptionElement.create("up", "down") //
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
    OptionElement one = OptionElement.create("one", "1");
    OptionElement two = OptionElement.create("two", "2");
    OptionElement three = OptionElement.create("three", "3");
    List<OptionElement> options = list(one, two, three);
    assertThat(OptionElement.findByName(options, "one")).isSameAs(one);
    assertThat(OptionElement.findByName(options, "two")).isSameAs(two);
    assertThat(OptionElement.findByName(options, "three")).isSameAs(three);
  }

  @Test public void findInListMissing() {
    OptionElement one = OptionElement.create("one", "1");
    OptionElement two = OptionElement.create("two", "2");
    List<OptionElement> options = list(one, two);
    assertThat(OptionElement.findByName(options, "three")).isNull();
  }

  @Test public void findInListDuplicate() {
    OptionElement one = OptionElement.create("one", "1");
    OptionElement two = OptionElement.create("two", "2");
    List<OptionElement> options = list(one, two, one);
    try {
      OptionElement.findByName(options, "one");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Multiple options match name: one");
    }
  }

  @Test public void escape() {
    assertThat(OptionElement.escape("h\"i")).isEqualTo("h\\\"i");
    assertThat(OptionElement.escape("h\ti")).isEqualTo("h\\ti");
    assertThat(OptionElement.escape("h\ri")).isEqualTo("h\\ri");
    assertThat(OptionElement.escape("h\\i")).isEqualTo("h\\\\i");
    assertThat(OptionElement.escape("h\ni")).isEqualTo("h\\ni");
  }
}
