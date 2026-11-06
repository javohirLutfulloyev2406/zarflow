import { useSyncExternalStore } from "react";

import { getAuthState, subscribeAuthState } from "../auth/store";
import type { Language } from "../api/types";

/**
 * Single source of truth for the UI's current language — the same value used
 * as the Accept-Language header for every API request (see auth/store.ts).
 * Subscribes via useSyncExternalStore so changing it (e.g. from the Profile
 * screen) re-renders every consumer immediately.
 */
export function useLanguage(): Language {
  return useSyncExternalStore(subscribeAuthState, () => getAuthState().language);
}
