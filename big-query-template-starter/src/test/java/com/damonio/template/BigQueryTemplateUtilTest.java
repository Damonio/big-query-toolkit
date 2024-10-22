package com.damonio.template;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static com.damonio.template.BigQueryTemplateUtil.compressAndEncodeToBase64;
import static com.damonio.template.BigQueryTemplateUtil.decompressBase64Encoded;
import static com.damonio.template.BigQueryTemplateUtil.generateCredentialsFileFromCompressedBase64StringCredentials;
import static com.damonio.template.BigQueryTemplateUtil.lowerCamelToLowerUnderscore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class BigQueryTemplateUtilTest {

    @Test
    void canParseLowerCamelToLowerUnderscore() {
        var expected = "test_word";
        assertEquals(expected, lowerCamelToLowerUnderscore("TestWord"));
        assertEquals(expected, lowerCamelToLowerUnderscore("testWord"));
        assertEquals(expected, lowerCamelToLowerUnderscore("test_word"));
    }

    @Test
    void canCompressThenEncodeBase64AndDecompressThenDecode() {
        var initialString = getExample();

        var compressed = compressAndEncodeToBase64(initialString);
        var decompressed = decompressBase64Encoded(compressed);

        assertEquals(initialString, decompressed);
    }

    @Test
    void canGenerateCredentialsFile() {
        var credentialsFile = generateCredentialsFileFromCompressedBase64StringCredentials(compressAndEncodeToBase64(getExample()));
        log.info("Credentials file created for the test: [{}]", credentialsFile);
        var delete = credentialsFile.toFile().delete();
        log.info("Credentials file deleted: [{}]", delete);
        assertTrue(delete);
    }

    private static @NotNull String getExample() {
        return """
                {
                    "some": "json sample"
                }
                """;
    }
}