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
  retry: () => void;
  setDisplayName: (displayName: string) => void;
}

const AuthContext = createContext<AuthContextValue>({
  status: "loading",
  user: null,
  errorDetail: null,
  retry: () => {},
  setDisplayName: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const rawInitData = useRawInitData();
  const launchParams = useLaunchParams(true);
  const [state, setState] = useState<Omit<AuthContextValue, "retry" | "setDisplayName">>({
    status: "loading",
    user: null,
    errorDetail: null,
  });
  const [retryNonce, setRetryNonce] = useState(0);
  // Guards against a stale response from an attempt superseded by a retry,
  // not against retrying at all -- a failed attempt must stay retryable.
  const latestAttemptId = useRef(0);

  useEffect(() => {
    // Seed a reasonable Accept-Language before the auth call resolves.
    setAuthState({ language: defaultLanguageFor(launchParams.tgWebAppData?.user?.languageCode) });
  }, [launchParams]);

  useEffect(() => {
    if (!rawInitData) {
      return;
    }
    const attemptId = ++latestAttemptId.current;

    login(rawInitData)
      .then((result) => {
        if (latestAttemptId.current !== attemptId) {
          return;
        }
        setAuthState({ token: result.accessToken, language: result.user.language });
        setState({ status: "ready", user: result.user, errorDetail: null });
      })
      .catch((err: unknown) => {
        if (latestAttemptId.current !== attemptId) {
          return;
        }
        const detail = err instanceof ApiError ? (err.problem.detail ?? null) : null;
        setState({ status: "error", user: null, errorDetail: detail });
      });
  }, [rawInitData, retryNonce]);

  const retry = () => {
    // Set from the click handler, not the effect body, so this doesn't
    // trigger React's "no setState synchronously in an effect" warning.
    setState({ status: "loading", user: null, errorDetail: null });
    setRetryNonce((n) => n + 1);
  };

  const setDisplayName = (displayName: string) => {
    setState((current) =>
      current.user ? { ...current, user: { ...current.user, displayName } } : current,
    );
  };

  return (
    <AuthContext.Provider value={{ ...state, retry, setDisplayName }}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}
