package com.damonio.migration.migration;

import com.damonio.migration.Util;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTest {

    @Test
    void canOrderScripts() {
        var scripts = List.of(
                "22_script_name.sql",
                "11_script_name.sql",
                "O22_script_name.sql",
                "O11_script_name.sql"
        );

        var ordered = Util.order('O', scripts);

        assertEquals(List.of(
                "O11_script_name.sql",
                "O22_script_name.sql",
                "R11_script_name.sql",
                "R22_script_name.sql"
        ), ordered);
    }
}