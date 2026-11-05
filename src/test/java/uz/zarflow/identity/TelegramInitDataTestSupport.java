package uz.zarflow.identity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Independently re-implements the Telegram initData signing algorithm for tests —
 * deliberately NOT reusing {@link InitDataValidator}'s internals, so a bug shared
 * between "build a fixture" and "validate it" code wouldn't be invisible to tests.
 */
final class TelegramInitDataTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private TelegramInitDataTestSupport() {
    }

    /** Builds a correctly-signed raw initData query string from the given fields (excluding hash). */
    static String sign(Map<String, String> fields, String botToken) {
        Map<String, String> ordered = new LinkedHashMap<>(fields);

        String checkString = ordered.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));
        byte[] hashBytes = hmac(secretKey, checkString.getBytes(StandardCharsets.UTF_8));
        String hash = HexFormat.of().formatHex(hashBytes);

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : ordered.entrySet()) {
            appendPair(query, entry.getKey(), entry.getValue());
        }
        appendPair(query, "hash", hash);
        return query.toString();
    }

    static String userJson(long id, String firstName, String lastName, String username, String languageCode) {
        try {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", id);
            user.put("first_name", firstName);
            user.put("last_name", lastName);
            user.put("username", username);
            user.put("language_code", languageCode);
            return JSON.writeValueAsString(user);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void appendPair(StringBuilder query, String key, String value) {
        if (!query.isEmpty()) {
            query.append('&');
        }
        query.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private static byte[] hmac(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

}
