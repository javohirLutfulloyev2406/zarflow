import { useEffect, useState } from "react";

import { AppRouter } from "./app/router";
import { Providers } from "./app/providers";
import { OutsideTelegramScreen } from "./components/OutsideTelegramScreen";
import { useAuth } from "./auth/AuthProvider";
import { useDictionary } from "./i18n/dictionary";
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
    return <GenericErrorScreen detail={auth.errorDetail} />;
  }

  return <AppRouter />;
}

function GenericErrorScreen({ detail }: { detail?: string | null }) {
  const dictionary = useDictionary();
  return (
    <div className="flex h-full items-center justify-center bg-navy-900 px-6 text-center text-ivory-50">
      {detail ?? dictionary.error.generic}
    </div>
  );
}
