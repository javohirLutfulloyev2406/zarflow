import { useEffect } from "react";
import { miniApp, useSignal } from "@telegram-apps/sdk-react";

/**
 * Mirrors Telegram's own dark/light signal onto `<html data-theme>`, which
 * `styles/tokens.css` uses to re-map the brand's CSS variables. Telegram's signal is
 * the single source of truth here — no `prefers-color-scheme` fallback, since outside
 * Telegram we render {@link OutsideTelegramScreen} instead of the themed app shell.
 */
export function useTelegramTheme(): void {
  const isDark = useSignal(miniApp.isDark);

  useEffect(() => {
    document.documentElement.dataset.theme = isDark ? "dark" : "light";
  }, [isDark]);
}
