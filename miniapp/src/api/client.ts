import { getAuthState, reauthenticate } from "../auth/store";
import { ApiError, parseProblemDetail } from "./problemDetail";

const BASE_URL = "/api/v1";

async function rawFetch(path: string, init: RequestInit, token: string | null): Promise<Response> {
  const { language } = getAuthState();
  const headers = new Headers(init.headers);
  headers.set("Accept-Language", language);
  headers.set("X-Request-Id", crypto.randomUUID());
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  return fetch(`${BASE_URL}${path}`, { ...init, headers });
}

/**
 * On a 401, re-authenticates once with a fresh initData value and retries the
 * same request once. A second 401 is a real failure, not retried again.
 */
export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const { token } = getAuthState();
  let response = await rawFetch(path, init, token);

  if (response.status === 401) {
    // Re-authentication itself can fail for a different reason than the
    // original 401 (offline, stale initData) -- propagate that real error
    // instead of masking it behind the stale original response.
    const freshToken = await reauthenticate();
    response = await rawFetch(path, init, freshToken);
  }

  if (!response.ok) {
    throw new ApiError(await parseProblemDetail(response), response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
