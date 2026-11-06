import { useEffect, useState } from "react";

import { bootstrapTelegram, isRunningInTelegram } from "./telegram/sdk";
import { useTelegramTheme } from "./telegram/theme";
import { OutsideTelegramScreen } from "./components/OutsideTelegramScreen";

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
    return (
      <div className="flex h-full items-center justify-center bg-navy-900 px-6 text-center text-ivory-50">
        Kutilmagan xatolik yuz berdi. Ilovani qayta ochib ko'ring.
      </div>
    );
  }

  return <AppShell loading={status === "checking"} />;
}

function AppShell({ loading }: { loading: boolean }) {
  useTelegramTheme();

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center font-heading text-xl text-gold-400">
        ZarFlow
      </div>
    );
  }

  return (
    <div className="flex h-full items-center justify-center font-heading text-xl text-gold-400">
      ZarFlow
    </div>
  );
}
