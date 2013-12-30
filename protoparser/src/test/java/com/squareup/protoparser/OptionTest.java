package com.squareup.protoparser;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.squareup.protoparser.TestUtils.list;
import static com.squareup.protoparser.TestUtils.map;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.data.MapEntry.entry;

public class OptionTest {
  @Test public void optionListToMap() {
    List<Option> options = list( //
        new Option("foo", "bar"), //
        new Option("ping", list( //
            new Option("kit", "kat"), //
            new Option("tic", "tac"), //
            new Option("up", "down") //
        )), //
        new Option("wire", map( //
            "omar", "little", //
            "proposition", "joe" //
        )), //
        new Option("nested.option", new Option("one", "two")), //
        new Option("nested.option", new Option("three", "four")) //
    );
    Map<String, Object> optionMap = Option.optionsAsMap(options);
    assertThat(optionMap).contains( //
        entry("foo", "bar"), //
        entry("ping", list( //
            new Option("kit", "kat"), //
            new Option("tic", "tac"), //
            new Option("up", "down") //
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
}
