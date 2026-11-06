import {
  backButton,
  init as initSdk,
  isTMA,
  miniApp,
  viewport,
} from "@telegram-apps/sdk-react";

/**
 * True if this page is running inside an actual Telegram client. Checked with the
 * synchronous heuristic (no round-trip) — good enough to decide which screen to render.
 */
export function isRunningInTelegram(): boolean {
  return isTMA();
}

/**
 * Initializes the SDK and mounts the components this app depends on. Every mount call
 * is guarded by the library's own `.isAvailable()` convention (see each component's
 * JSDoc in @telegram-apps/sdk) so an unsupported client version degrades silently
 * instead of throwing. Must only be called after {@link isRunningInTelegram} is true.
 */
export async function bootstrapTelegram(): Promise<void> {
  initSdk();

  if (miniApp.mountSync.isAvailable()) {
    miniApp.mountSync();
  }

  if (backButton.mount.isAvailable()) {
    backButton.mount();
  }

  if (viewport.mount.isAvailable() && !viewport.isMounting()) {
    await viewport.mount();
  }
  if (viewport.expand.isAvailable()) {
    viewport.expand();
  }
  if (viewport.bindCssVars.isAvailable()) {
    viewport.bindCssVars();
  }

  if (miniApp.ready.isAvailable()) {
    miniApp.ready();
  }
}
