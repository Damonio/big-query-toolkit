import com.damonio.template.BigQueryTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class CompressAndEncodeCredentialsTest {

    @Test
    void example() {
        var yourCredentials = doNotCommitThisFileIfYouPutYourCredentialsHere();

        log.info("Your credentials: [{}]", BigQueryTemplateUtil.compressAndEncodeToBase64(yourCredentials));

        assertTrue(true);
    }

    private static @NotNull String doNotCommitThisFileIfYouPutYourCredentialsHere() {
        return """
                {
                    "some": "json sample"
                }
                """;
    }
}
