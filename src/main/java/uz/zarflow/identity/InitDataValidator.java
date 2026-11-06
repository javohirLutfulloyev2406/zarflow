package uz.zarflow.identity;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Validates Telegram Mini App {@code initData} per the official algorithm
 * ("Validating data received via the Mini App"). See {@code docs} / the plan
 * for the step-by-step procedure in plain language.
 */
@Component
class InitDataValidator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String WEB_APP_DATA_KEY = "WebAppData";
    private static final long FUTURE_SKEW_SECONDS = 60;

    private static final JsonMapper USER_JSON_MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private final AuthProperties authProperties;
    private final Clock clock;

    InitDataValidator(AuthProperties authProperties, Clock clock) {
        this.authProperties = authProperties;
        this.clock = clock;
    }

    InitDataValidation validate(String rawInitData) {
        Map<String, String> fields = parse(rawInitData);
        if (fields == null) {
            return new InitDataValidation.Failure(FailureReason.MALFORMED);
        }

        String hash = fields.remove("hash");
        if (hash == null) {
            return new InitDataValidation.Failure(FailureReason.MISSING_HASH);
        }

        String expected = computeHash(buildCheckString(fields));
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                hash.getBytes(StandardCharsets.US_ASCII))) {
            return new InitDataValidation.Failure(FailureReason.BAD_SIGNATURE);
        }

        Instant authDate = parseAuthDate(fields.get("auth_date"));
        if (authDate == null) {
            return new InitDataValidation.Failure(FailureReason.MALFORMED);
        }

        Instant now = Instant.now(clock);
        boolean tooOld = authDate.isBefore(now.minus(authProperties.initDataMaxAge()));
        boolean tooFarInFuture = authDate.isAfter(now.plusSeconds(FUTURE_SKEW_SECONDS));
        if (tooOld || tooFarInFuture) {
            return new InitDataValidation.Failure(FailureReason.EXPIRED);
        }

        VerifiedTelegramUser user = parseUser(fields.get("user"), authDate);
        if (user == null) {
            return new InitDataValidation.Failure(FailureReason.MALFORMED);
        }

        return new InitDataValidation.Success(user);
    }

    /** Null means "reject as malformed" — duplicate key or undecodable percent-encoding. */
    private Map<String, String> parse(String rawInitData) {
        Map<String, String> result = new LinkedHashMap<>();
        if (rawInitData == null || rawInitData.isEmpty()) {
            return result;
        }
        for (String pair : rawInitData.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawKey = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawValue = eq >= 0 ? pair.substring(eq + 1) : "";
            String key;
            String value;
            try {
                key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                return null;
            }
            if (result.containsKey(key)) {
                return null;
            }
            result.put(key, value);
        }
        return result;
    }

    private String buildCheckString(Map<String, String> fields) {
        return fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private String computeHash(String checkString) {
        try {
            Mac secretKeyMac = Mac.getInstance(HMAC_ALGORITHM);
            secretKeyMac.init(new SecretKeySpec(
                    WEB_APP_DATA_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] secretKey = secretKeyMac.doFinal(
                    authProperties.botToken().getBytes(StandardCharsets.UTF_8));

            Mac dataMac = Mac.getInstance(HMAC_ALGORITHM);
            dataMac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] hashBytes = dataMac.doFinal(checkString.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 is not available", ex);
        }
    }

    private Instant parseAuthDate(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private VerifiedTelegramUser parseUser(String userJson, Instant authDate) {
        if (userJson == null) {
            return null;
        }
        try {
            TelegramUserJson parsed = USER_JSON_MAPPER.readValue(userJson, TelegramUserJson.class);
            if (parsed.id() == null) {
                return null;
            }
            return new VerifiedTelegramUser(
                    parsed.id(), parsed.firstName(), parsed.lastName(), parsed.username(),
                    parsed.languageCode(), authDate);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private record TelegramUserJson(
            Long id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String username,
            @JsonProperty("language_code") String languageCode) {
    }

}
