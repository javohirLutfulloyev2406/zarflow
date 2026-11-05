package uz.zarflow.shared.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint protected by the real security chain (everything not explicitly permitted). */
@RestController
class SecuredProbeController {

    private final CurrentUser currentUser;

    SecuredProbeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/probe/secured")
    SecuredResponse secured() {
        return new SecuredResponse(currentUser.requireUserId().toString());
    }

    record SecuredResponse(String userId) {
    }

}
