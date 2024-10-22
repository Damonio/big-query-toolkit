package com.damonio.template;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.base.CaseFormat;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@UtilityClass
public class BigQueryTemplateUtil {

    public static String lowerCamelToLowerUnderscore(String s) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s);
    }

    public static String compressAndEncodeToBase64(String string) {
        var compressedBytes = compress(string);
        return Base64.getEncoder().encodeToString(compressedBytes);
    }

    @SneakyThrows
    private static byte[] compress(String str) {
        try (ByteArrayOutputStream baostream = new ByteArrayOutputStream()) {
            try (OutputStream outStream = new GZIPOutputStream(baostream)) {
                outStream.write(str.getBytes(StandardCharsets.UTF_8));
            }
            return baostream.toByteArray();
        }
    }

    public static ServiceAccountCredentials generateServiceAccountCredentialsFromCompressedBase64StringCredentials(String base64StringCredentials) {
        return getServiceAccountCredentials(generateCredentialsFileFromCompressedBase64StringCredentials(base64StringCredentials));
    }

    @SneakyThrows
    public static ServiceAccountCredentials getServiceAccountCredentials(Path path) {
        try (var serviceAccountStream = new FileInputStream(path.toFile())) {
            return ServiceAccountCredentials.fromStream(serviceAccountStream);
        }
    }

    @SneakyThrows
    public static Path generateCredentialsFileFromCompressedBase64StringCredentials(String base64StringCredentials) {
        var originalString = decompressBase64Encoded(base64StringCredentials);
        var tempFile = Files.createTempFile("temp", ".json");
        return Files.writeString(tempFile, originalString);
    }

    public static String decompressBase64Encoded(String base64StringCredentials) {
        var compressedData = Base64.getDecoder().decode(base64StringCredentials);
        return decompress(compressedData);
    }

    @SneakyThrows
    private static String decompress(byte[] compressedData) {
        try (var inStream = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            try (var baoStream2 = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inStream.read(buffer)) > 0) {
                    baoStream2.write(buffer, 0, len);
                }
                return baoStream2.toString(StandardCharsets.UTF_8);
            }
        }
    }


}

