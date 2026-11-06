import { ApiError, parseProblemDetail } from "./problemDetail";
import type { LoginResponse } from "./types";

/**
 * Self-contained on purpose: does not go through client.ts's authorized/retry
 * wrapper, since login is what the 401-retry path itself calls.
 */
export async function login(initData: string): Promise<LoginResponse> {
  const response = await fetch("/api/v1/auth/telegram", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Request-Id": crypto.randomUUID(),
    },
    body: JSON.stringify({ initData }),
  });

  if (!response.ok) {
    throw new ApiError(await parseProblemDetail(response), response.status);
  }
  return (await response.json()) as LoginResponse;
}
