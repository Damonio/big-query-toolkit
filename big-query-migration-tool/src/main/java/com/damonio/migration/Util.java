package com.damonio.migration;

import autovalue.shaded.com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class Util {

    @SneakyThrows
    public static String readFile(String path) {
        var resource = new ClassPathResource(path);
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }

    public static List<String> order(char singleRunScripPrefix, List<String> list) {
        var toOrder =new ArrayList<>(list);
        toOrder.sort((a, b) -> {
            char prefixA = a.charAt(0);
            char prefixB = b.charAt(0);
            int versionA = Integer.parseInt(a.substring(1, a.indexOf("_")));
            int versionB = Integer.parseInt(b.substring(1, b.indexOf("_")));

            if (prefixA != prefixB) {
                if (prefixA == singleRunScripPrefix) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return Integer.compare(versionA, versionB);
            }
        });
        return toOrder;
    }

    @SneakyThrows
    public static String readFileInsideJar(String path) {
        var resource = new ClassPathResource(path);
        var inputStream = resource.getInputStream();
        return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
    }

    public static String substituteValues(String string, Map<String, String> replacements) {
        var stringSubstitutor = new StringSubstitutor(replacements);
        return stringSubstitutor.replace(string);
    }

    @SneakyThrows
    public static String generateChecksum(String script) {
        var md = MessageDigest.getInstance("SHA-256");
        var bytes = md.digest(script.getBytes());
        var checksum = new StringBuilder();
        for (byte b : bytes) {
            checksum.append(String.format("%02x", b));
        }
        return checksum.toString();
    }

}
