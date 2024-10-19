package com.damonio.template;

import com.google.common.base.CaseFormat;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Util {

    public static String toSnakeCase(String s) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s);
    }
}
