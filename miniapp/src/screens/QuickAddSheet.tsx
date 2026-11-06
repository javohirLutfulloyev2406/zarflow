import { openTelegramLink } from "@telegram-apps/sdk-react";

import { useDictionary } from "../i18n/dictionary";

const botUsername = import.meta.env.VITE_BOT_USERNAME;

export function QuickAddSheet({ onClose }: { onClose: () => void }) {
  const dictionary = useDictionary();

  function goToBot() {
    if (botUsername && openTelegramLink.isAvailable()) {
      openTelegramLink(`https://t.me/${botUsername}`);
    }
    onClose();
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end bg-black/50" onClick={onClose}>
      <div
        className="rounded-t-2xl bg-navy-800 p-5 text-center text-ivory-50"
        onClick={(event) => event.stopPropagation()}
      >
        <p className="mb-4 text-sm">{dictionary.quickAdd.title}</p>
        <button
          type="button"
          onClick={goToBot}
          className="w-full rounded-full bg-emerald-700 py-3 text-sm font-medium text-ivory-50"
        >
          {dictionary.quickAdd.button}
        </button>
      </div>
    </div>
  );
}
