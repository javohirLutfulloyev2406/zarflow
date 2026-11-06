import { retrieveRawInitData } from "@telegram-apps/sdk-react";

import { login } from "../api/auth";
import type { Language } from "../api/types";

/**
 * The JWT lives only here, as a module-scope variable — never localStorage,
 * sessionStorage, or a cookie. A reload means re-authenticating, by design:
 * Telegram re-supplies fresh initData on every Mini App open anyway.
 */
interface AuthStoreState {
  token: string | null;
  language: Language;
}

let state: AuthStoreState = { token: null, language: "uz" };
const listeners = new Set<() => void>();

export function getAuthState(): AuthStoreState {
  return state;
}

export function setAuthState(partial: Partial<AuthStoreState>): void {
  state = { ...state, ...partial };
  for (const listener of listeners) {
    listener();
  }
}

export function subscribeAuthState(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

let inFlightReauth: Promise<string> | null = null;

/**
 * Used by api/client.ts when a request comes back 401: fetch a fresh raw
 * initData value (never the one captured at app boot) and re-authenticate.
 * Throws if no initData is available (e.g. called outside Telegram).
 *
 * Concurrent callers (several requests 401-ing around the same time) share
 * one in-flight attempt instead of racing independent login() calls that
 * could overwrite each other's token in the shared auth state.
 */
export async function reauthenticate(): Promise<string> {
  if (inFlightReauth) {
    return inFlightReauth;
  }
  inFlightReauth = (async () => {
    const raw = retrieveRawInitData();
    if (!raw) {
      throw new Error("No initData available for re-authentication");
    }
    const result = await login(raw);
    setAuthState({ token: result.accessToken, language: result.user.language });
    return result.accessToken;
  })();
  try {
    return await inFlightReauth;
  } finally {
    inFlightReauth = null;
  }
}
