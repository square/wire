package com.squareup.protoparser;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.fest.assertions.data.MapEntry.entry;

public class OptionElementTest {
  @Test public void simpleToString() {
    OptionElement option = new OptionElement("foo", "bar", false);
    String expected = "foo = \"bar\"";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void nestedToString() {
    OptionElement option = new OptionElement("foo.boo", new OptionElement("bar", "baz", true), true);
    String expected = "(foo.boo).bar = \"baz\"";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void listToString() {
    OptionElement option = new OptionElement("foo",
        list(new OptionElement("ping", "pong", true), new OptionElement("kit", "kat", false)), true);
    String expected = ""
        + "(foo) = [\n"
        + "  (ping) = \"pong\",\n"
        + "  kit = \"kat\"\n"
        + "]";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void booleanToString() {
    OptionElement option = new OptionElement("foo", false, false);
    String expected = "foo = false";
    assertThat(option.toString()).isEqualTo(expected);
  }

  @Test public void optionListToMap() {
    List<OptionElement> options = list( //
        new OptionElement("foo", "bar", false), //
        new OptionElement("ping", list( //
            new OptionElement("kit", "kat", false), //
            new OptionElement("tic", "tac", false), //
            new OptionElement("up", "down", false) //
        ), false), //
        new OptionElement("wire", map( //
            "omar", "little", //
            "proposition", "joe" //
        ), false), //
        new OptionElement("nested.option", new OptionElement("one", "two", false), false), //
        new OptionElement("nested.option", new OptionElement("three", "four", false), false) //
    );
    Map<String, Object> optionMap = OptionElement.optionsAsMap(options);
    assertThat(optionMap).contains( //
        entry("foo", "bar"), //
        entry("ping", list( //
            new OptionElement("kit", "kat", false), //
            new OptionElement("tic", "tac", false), //
            new OptionElement("up", "down", false) //
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
    OptionElement one = new OptionElement("one", "1", false);
    OptionElement two = new OptionElement("two", "2", false);
    OptionElement three = new OptionElement("three", "3", false);
    List<OptionElement> options = list(one, two, three);
    assertThat(OptionElement.findByName(options, "one")).isSameAs(one);
    assertThat(OptionElement.findByName(options, "two")).isSameAs(two);
    assertThat(OptionElement.findByName(options, "three")).isSameAs(three);
  }

  @Test public void findInListMissing() {
    OptionElement one = new OptionElement("one", "1", false);
    OptionElement two = new OptionElement("two", "2", false);
    List<OptionElement> options = list(one, two);
    assertThat(OptionElement.findByName(options, "three")).isNull();
  }

  @Test public void findInListDuplicate() {
    OptionElement one = new OptionElement("one", "1", false);
    OptionElement two = new OptionElement("two", "2", false);
    List<OptionElement> options = list(one, two, one);
    try {
      OptionElement.findByName(options, "one");
      fail("Multiple option matches not allowed.");
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
