package uz.zarflow.shared.security;

import java.util.UUID;

/** Test fixture for {@link CurrentUser} until issue #4 wires up real JWT auth. */
public class StubCurrentUser implements CurrentUser {

    private final UUID userId;

    public StubCurrentUser(UUID userId) {
        this.userId = userId;
    }

    @Override
    public UUID requireUserId() {
        return userId;
    }

}
