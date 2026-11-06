import type { Language } from "../api/types";

/** Mirrors the backend's UserAccountsService.defaultLanguageFor exactly. */
export function defaultLanguageFor(languageCode: string | undefined): Language {
  return languageCode?.startsWith("ru") ? "ru" : "uz";
}
