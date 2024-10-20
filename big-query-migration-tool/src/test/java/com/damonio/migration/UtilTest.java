package com.damonio.migration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTest {

    @Test
    void canOrderScripts() {
        var scripts = List.of(
                "R22_script_name.sql",
                "R11_script_name.sql",
                "V22_script_name.sql",
                "V11_script_name.sql"
        );

        var ordered = Util.order('V', scripts);

        assertEquals(List.of(
                "V11_script_name.sql",
                "V22_script_name.sql",
                "R11_script_name.sql",
                "R22_script_name.sql"
        ), ordered);
    }
}