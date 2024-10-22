import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static com.damonio.template.BigQueryTemplateUtil.compressAndEncodeToBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompressAndEncodeCredentialsTest {

    @Test
    void example() {
        var yourCredentials = getYourCredentials();

        assertEquals("H4sIAAAAAAAA/6vmUgACpeL83FQlKwWlrOL8PIXixNyCnFQlrlouAH5l89weAAAA", compressAndEncodeToBase64(yourCredentials));
    }

    private static @NotNull String getYourCredentials() {
        return """
                {
                    "some": "json sample"
                }
                """;
    }
}
