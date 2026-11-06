const botUsername = import.meta.env.VITE_BOT_USERNAME;

export function OutsideTelegramScreen() {
  const botLink = botUsername ? `https://t.me/${botUsername}` : undefined;

  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 bg-navy-900 px-6 text-center text-ivory-50">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-700">
        <span className="font-heading text-2xl font-bold text-gold-400">Z</span>
      </div>
      <p className="font-heading text-lg font-semibold">ZarFlow</p>
      <p className="text-sm text-slate-400">Ilovani Telegram orqali oching.</p>
      {botLink ? (
        <a
          href={botLink}
          className="rounded-full bg-emerald-700 px-5 py-2 text-sm font-medium text-ivory-50"
        >
          Botni ochish
        </a>
      ) : null}
    </div>
  );
}
