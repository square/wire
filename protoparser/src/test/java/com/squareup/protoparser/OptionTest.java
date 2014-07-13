package com.squareup.protoparser;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.fest.assertions.data.MapEntry.entry;

public class OptionTest {
  @Test public void simpleToString() {
    Option option = new Option("foo", "bar", false);
    String expected = "foo = \"bar\"";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void nestedToString() {
    Option option = new Option("foo.boo", new Option("bar", "baz", true), true);
    String expected = "(foo.boo).bar = \"baz\"";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void listToString() {
    Option option = new Option("foo",
        list(new Option("ping", "pong", true), new Option("kit", "kat", false)), true);
    String expected = ""
        + "(foo) = [\n"
        + "  (ping) = \"pong\",\n"
        + "  kit = \"kat\"\n"
        + "]";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void booleanToString() {
    Option option = new Option("foo", false, false);
    String expected = "foo = false";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void optionListToMap() {
    List<Option> options = list( //
        new Option("foo", "bar", false), //
        new Option("ping", list( //
            new Option("kit", "kat", false), //
            new Option("tic", "tac", false), //
            new Option("up", "down", false) //
        ), false), //
        new Option("wire", map( //
            "omar", "little", //
            "proposition", "joe" //
        ), false), //
        new Option("nested.option", new Option("one", "two", false), false), //
        new Option("nested.option", new Option("three", "four", false), false) //
    );
    Map<String, Object> optionMap = Option.optionsAsMap(options);
    assertThat(optionMap).contains( //
        entry("foo", "bar"), //
        entry("ping", list( //
            new Option("kit", "kat", false), //
            new Option("tic", "tac", false), //
            new Option("up", "down", false) //
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
    Option one = new Option("one", "1", false);
    Option two = new Option("two", "2", false);
    Option three = new Option("three", "3", false);
    List<Option> options = list(one, two, three);
    assertThat(Option.findByName(options, "one")).isSameAs(one);
    assertThat(Option.findByName(options, "two")).isSameAs(two);
    assertThat(Option.findByName(options, "three")).isSameAs(three);
  }

  @Test public void findInListMissing() {
    Option one = new Option("one", "1", false);
    Option two = new Option("two", "2", false);
    List<Option> options = list(one, two);
    assertThat(Option.findByName(options, "three")).isNull();
  }

  @Test public void findInListDuplicate() {
    Option one = new Option("one", "1", false);
    Option two = new Option("two", "2", false);
    List<Option> options = list(one, two, one);
    try {
      Option.findByName(options, "one");
      fail("Multiple option matches not allowed.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Multiple options match name: one");
    }
  }

  @Test public void escape() {
    assertThat(Option.escape("h\"i")).isEqualTo("h\\\"i");
    assertThat(Option.escape("h\ti")).isEqualTo("h\\ti");
    assertThat(Option.escape("h\ri")).isEqualTo("h\\ri");
    assertThat(Option.escape("h\\i")).isEqualTo("h\\\\i");
    assertThat(Option.escape("h\ni")).isEqualTo("h\\ni");
  }
}
