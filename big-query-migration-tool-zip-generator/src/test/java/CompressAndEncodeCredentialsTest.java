import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class CompressAndEncodeCredentialsTest {

    @Test
    void example() {
        var yourCredentials = doNotComitThisFileIfYouPutYourCredentialsHere();

        log.info("Your credentials: [{}]", yourCredentials);

        assertTrue(true);
    }

    private static @NotNull String doNotComitThisFileIfYouPutYourCredentialsHere() {
        return """
                {
                    "some": "json sample"
                }
                """;
    }
}
