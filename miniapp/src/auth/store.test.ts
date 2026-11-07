import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("@telegram-apps/sdk-react", () => ({
  retrieveRawInitData: vi.fn(() => "raw-init-data"),
}));

vi.mock("../api/auth", () => ({
  login: vi.fn(),
}));

import { retrieveRawInitData } from "@telegram-apps/sdk-react";
import { login } from "../api/auth";
import type { LoginResponse } from "../api/types";
import { getAuthState, reauthenticate } from "./store";

function loginResponse(accessToken: string): LoginResponse {
  return {
    accessToken,
    tokenType: "Bearer",
    expiresIn: 3600,
    user: { id: "u1", firstName: "Ali", displayName: null, language: "uz", isNew: false },
  };
}

afterEach(() => {
  vi.clearAllMocks();
});

describe("reauthenticate", () => {
  it("shares one in-flight attempt across concurrent callers instead of racing two logins", async () => {
    let resolveLogin: (value: LoginResponse) => void;
    vi.mocked(login).mockReturnValueOnce(
      new Promise((resolve) => {
        resolveLogin = resolve;
      }),
    );

    const first = reauthenticate();
    const second = reauthenticate();

    resolveLogin!(loginResponse("token-a"));
    const [firstToken, secondToken] = await Promise.all([first, second]);

    expect(login).toHaveBeenCalledTimes(1);
    expect(firstToken).toBe("token-a");
    expect(secondToken).toBe("token-a");
    expect(getAuthState().token).toBe("token-a");
  });

  it("allows a fresh attempt after a previous one finished", async () => {
    vi.mocked(login)
      .mockResolvedValueOnce(loginResponse("token-a"))
      .mockResolvedValueOnce(loginResponse("token-b"));

    await reauthenticate();
    await reauthenticate();

    expect(login).toHaveBeenCalledTimes(2);
    expect(getAuthState().token).toBe("token-b");
  });

  it("throws when no initData is available, without caching the failure forever", async () => {
    vi.mocked(retrieveRawInitData).mockReturnValueOnce(undefined);

    await expect(reauthenticate()).rejects.toThrow("No initData available");

    vi.mocked(retrieveRawInitData).mockReturnValueOnce("raw-init-data");
    vi.mocked(login).mockResolvedValueOnce(loginResponse("token-c"));
    await expect(reauthenticate()).resolves.toBe("token-c");
  });
});
