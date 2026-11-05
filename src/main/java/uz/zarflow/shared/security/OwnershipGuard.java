package uz.zarflow.shared.security;

import java.util.Optional;

import uz.zarflow.shared.exception.NotFoundException;

/**
 * Accessing another user's resource must look exactly like "not found" (404),
 * never 403 (PRD M2 acceptance criterion) — so every ownership-scoped repository
 * lookup should be routed through here instead of a bespoke check.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    public static <T> T requireFound(Optional<T> result, String resource) {
        return result.orElseThrow(() -> new NotFoundException("common.not-found", resource));
    }

}
