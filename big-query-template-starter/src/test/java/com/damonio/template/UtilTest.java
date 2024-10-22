package com.damonio.template;

import com.damonio.template.Util;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTest {

    @Test
    void canParseToSnakeCase() {
        var expected = "test_word";
        assertEquals(expected, Util.toSnakeCase("TestWord"));
        assertEquals(expected, Util.toSnakeCase("testWord"));
        assertEquals(expected, Util.toSnakeCase("test_word"));
    }
}