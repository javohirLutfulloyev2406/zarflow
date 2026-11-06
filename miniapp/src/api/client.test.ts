import { afterEach, describe, expect, it, vi } from "vitest";

import { apiRequest } from "./client";
import { ApiError } from "./problemDetail";

vi.mock("../auth/store", () => ({
  getAuthState: vi.fn(() => ({ token: "stale-token", language: "uz" })),
  reauthenticate: vi.fn(),
}));

import { reauthenticate } from "../auth/store";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.clearAllMocks();
  vi.unstubAllGlobals();
});

describe("apiRequest 401 handling", () => {
  it("retries exactly once with a fresh token after a successful re-authentication", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(401, { status: 401, title: "Unauthorized" }))
      .mockResolvedValueOnce(jsonResponse(200, { ok: true }));
    vi.stubGlobal("fetch", fetchMock);
    vi.mocked(reauthenticate).mockResolvedValueOnce("fresh-token");

    const result = await apiRequest<{ ok: boolean }>("/users/me");

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(reauthenticate).toHaveBeenCalledTimes(1);
    const retryInit = fetchMock.mock.calls[1]?.[1] as RequestInit;
    const retryHeaders = retryInit.headers as Headers;
    expect(retryHeaders.get("Authorization")).toBe("Bearer fresh-token");
  });

  it("propagates the real re-authentication failure instead of the stale 401", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(401, { status: 401, title: "Unauthorized" }));
    vi.stubGlobal("fetch", fetchMock);
    const reauthFailure = new Error("No initData available for re-authentication");
    vi.mocked(reauthenticate).mockRejectedValueOnce(reauthFailure);

    await expect(apiRequest("/users/me")).rejects.toBe(reauthFailure);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not retry a second time if the retried request is also a 401", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(401, { status: 401, title: "Unauthorized" }))
      .mockResolvedValueOnce(jsonResponse(401, { status: 401, title: "Unauthorized" }));
    vi.stubGlobal("fetch", fetchMock);
    vi.mocked(reauthenticate).mockResolvedValueOnce("fresh-token");

    await expect(apiRequest("/users/me")).rejects.toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(reauthenticate).toHaveBeenCalledTimes(1);
  });
});
