import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useLaunchParams, useRawInitData } from "@telegram-apps/sdk-react";

import { login } from "../api/auth";
import { ApiError } from "../api/problemDetail";
import type { LoginResponseUser } from "../api/types";
import { defaultLanguageFor } from "../i18n/defaultLanguage";
import { setAuthState } from "./store";

type AuthStatus = "loading" | "ready" | "error";

interface AuthContextValue {
  status: AuthStatus;
  user: LoginResponseUser | null;
  errorDetail: string | null;
}

const AuthContext = createContext<AuthContextValue>({
  status: "loading",
  user: null,
  errorDetail: null,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const rawInitData = useRawInitData();
  const launchParams = useLaunchParams(true);
  const [state, setState] = useState<AuthContextValue>({
    status: "loading",
    user: null,
    errorDetail: null,
  });
  const attempted = useRef(false);

  useEffect(() => {
    // Seed a reasonable Accept-Language before the auth call resolves.
    setAuthState({ language: defaultLanguageFor(launchParams.tgWebAppData?.user?.languageCode) });
  }, [launchParams]);

  useEffect(() => {
    if (attempted.current || !rawInitData) {
      return;
    }
    attempted.current = true;

    login(rawInitData)
      .then((result) => {
        setAuthState({ token: result.accessToken, language: result.user.language });
        setState({ status: "ready", user: result.user, errorDetail: null });
      })
      .catch((err: unknown) => {
        const detail = err instanceof ApiError ? (err.problem.detail ?? null) : null;
        setState({ status: "error", user: null, errorDetail: detail });
      });
  }, [rawInitData]);

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}
