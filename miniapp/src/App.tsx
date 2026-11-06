import { useEffect, useState } from "react";

import { OutsideTelegramScreen } from "./components/OutsideTelegramScreen";
import { AuthProvider, useAuth } from "./auth/AuthProvider";
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
    return <LoadingPlaceholder />;
  }

  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  );
}

function AppShell() {
  useTelegramTheme();
  const auth = useAuth();

  if (auth.status === "loading") {
    return <LoadingPlaceholder />;
  }

  if (auth.status === "error") {
    return <GenericErrorScreen detail={auth.errorDetail} />;
  }

  return (
    <div className="flex h-full items-center justify-center font-heading text-xl text-gold-400">
      Xayrli kun, {auth.user?.firstName}
    </div>
  );
}

function LoadingPlaceholder() {
  return (
    <div className="flex h-full items-center justify-center font-heading text-xl text-gold-400">
      ZarFlow
    </div>
  );
}

function GenericErrorScreen({ detail }: { detail?: string | null }) {
  return (
    <div className="flex h-full items-center justify-center bg-navy-900 px-6 text-center text-ivory-50">
      {detail ?? "Kutilmagan xatolik yuz berdi. Ilovani qayta ochib ko'ring."}
    </div>
  );
}
