package uz.zarflow.shared.security;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
class JwtCurrentUser implements CurrentUser {

    @Override
    public UUID requireUserId() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("No authenticated JWT in the security context");
        }
        return UUID.fromString(jwtAuth.getToken().getSubject());
    }

}
