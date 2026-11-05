package uz.zarflow.shared.security;

import java.util.UUID;

/** Who's making the request. JWT-backed implementation arrives with issue #4. */
public interface CurrentUser {

    UUID requireUserId();

}
