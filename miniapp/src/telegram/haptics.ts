import { hapticFeedback } from "@telegram-apps/sdk-react";

export function hapticSelectionChanged(): void {
  if (hapticFeedback.selectionChanged.isAvailable()) {
    hapticFeedback.selectionChanged();
  }
}

export function hapticLightImpact(): void {
  if (hapticFeedback.impactOccurred.isAvailable()) {
    hapticFeedback.impactOccurred("light");
  }
}
