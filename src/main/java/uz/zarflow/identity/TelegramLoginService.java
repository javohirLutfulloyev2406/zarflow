package uz.zarflow.identity;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import uz.zarflow.shared.exception.UnauthorizedException;
import uz.zarflow.user.UserAccounts;

@Service
class TelegramLoginService {

    private static final Logger log = LoggerFactory.getLogger(TelegramLoginService.class);
    private static final String ISSUER = "zarflow";

    private final InitDataValidator validator;
    private final UserAccounts userAccounts;
    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;
    private final Clock clock;

    TelegramLoginService(
            InitDataValidator validator,
            UserAccounts userAccounts,
            JwtEncoder jwtEncoder,
            AuthProperties authProperties,
            Clock clock) {
        this.validator = validator;
        this.userAccounts = userAccounts;
        this.jwtEncoder = jwtEncoder;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    TelegramLoginResult login(String rawInitData) {
        InitDataValidation validation = validator.validate(rawInitData);
        if (validation instanceof InitDataValidation.Failure failure) {
            log.warn("Telegram initData validation failed: {}", failure.reason());
            throw new UnauthorizedException("auth.invalid_init_data");
        }

        VerifiedTelegramUser verified = ((InitDataValidation.Success) validation).user();
        UserAccounts.LoginResult userResult = userAccounts.registerOrUpdate(new UserAccounts.TelegramProfile(
                verified.telegramId(),
                verified.firstName(),
                verified.lastName(),
                verified.username(),
                verified.languageCode()));

        String accessToken = issueToken(userResult, verified.telegramId());
        return new TelegramLoginResult(accessToken, authProperties.jwtTtl().toSeconds(), userResult);
    }

    private String issueToken(UserAccounts.LoginResult userResult, long telegramId) {
        Instant now = Instant.now(clock);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userResult.userId().toString())
                .claim("tid", telegramId)
                .claim("lang", userResult.language())
                .issuedAt(now)
                .expiresAt(now.plus(authProperties.jwtTtl()))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    record TelegramLoginResult(String accessToken, long expiresIn, UserAccounts.LoginResult user) {
    }

}
