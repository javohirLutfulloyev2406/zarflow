import { useEffect, useState } from "react";

import { AppRouter } from "./app/router";
import { Providers } from "./app/providers";
import { DebugPanel } from "./debug/DebugPanel";
import { OutsideTelegramScreen } from "./components/OutsideTelegramScreen";
import { useAuth } from "./auth/AuthProvider";
import { useDictionary } from "./i18n/dictionary";
import { NamePrompt } from "./screens/NamePrompt";
import { Splash } from "./screens/Splash";
import { bootstrapTelegram, isRunningInTelegram } from "./telegram/sdk";
import { useTelegramTheme } from "./telegram/theme";

type BootStatus = "checking" | "ready" | "error";

export function App() {
  const [inTelegram] = useState(isRunningInTelegram);
  const [status, setStatus] = useState<BootStatus>("checking");

  useEffect(() => {
    if (!inTelegram) {
      return;
    }
    bootstrapTelegram()
      .then(() => setStatus("ready"))
      .catch(() => setStatus("error"));
  }, [inTelegram]);

  if (!inTelegram) {
    return <OutsideTelegramScreen />;
  }

  if (status === "error") {
    return <GenericErrorScreen />;
  }

  if (status === "checking") {
    return <Splash />;
  }

  return (
    <Providers>
      <AppShell />
    </Providers>
  );
}

function AppShell() {
  useTelegramTheme();
  const auth = useAuth();

  if (auth.status === "loading") {
    return <Splash />;
  }

  if (auth.status === "error") {
    return <GenericErrorScreen detail={auth.errorDetail} onRetry={auth.retry} />;
  }

  if (!auth.user?.displayName) {
    return <NamePrompt />;
  }

  return (
    <>
      <AppRouter />
      <DebugPanel />
    </>
  );
}

function GenericErrorScreen({ detail, onRetry }: { detail?: string | null; onRetry?: () => void }) {
  const dictionary = useDictionary();
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 bg-navy-900 px-6 text-center text-ivory-50">
      <p>{detail ?? dictionary.error.generic}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-full bg-gold-400 px-6 py-2 font-semibold text-navy-900"
        >
          {dictionary.error.retry}
        </button>
      )}
    </div>
  );
}
