package uz.zarflow.identity;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
class TelegramAuthController {

    private final TelegramLoginService loginService;

    TelegramAuthController(TelegramLoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/telegram")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        TelegramLoginService.TelegramLoginResult result = loginService.login(request.initData());
        return new LoginResponse(
                result.accessToken(),
                "Bearer",
                result.expiresIn(),
                new UserView(
                        result.user().userId(),
                        result.user().firstName(),
                        result.user().displayName(),
                        result.user().language(),
                        result.user().isNew()));
    }

    record LoginRequest(@NotBlank @Size(max = 4096) String initData) {
    }

    record LoginResponse(String accessToken, String tokenType, long expiresIn, UserView user) {
    }

    record UserView(UUID id, String firstName, String displayName, String language, boolean isNew) {
    }

}
