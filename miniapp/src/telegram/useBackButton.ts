import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { backButton } from "@telegram-apps/sdk-react";

/**
 * Shows Telegram's native BackButton only while the calling screen is mounted
 * with visible=true (Profile), and always hides it again on unmount — bottom-
 * nav tabs must never show it, per PRD §5's Mini App requirements.
 */
export function useBackButton(visible: boolean): void {
  const navigate = useNavigate();

  useEffect(() => {
    if (!visible) {
      if (backButton.hide.isAvailable()) {
        backButton.hide();
      }
      return;
    }

    if (backButton.show.isAvailable()) {
      backButton.show();
    }
    const off = backButton.onClick.isAvailable()
      ? backButton.onClick(() => navigate(-1))
      : undefined;

    return () => {
      off?.();
      if (backButton.hide.isAvailable()) {
        backButton.hide();
      }
    };
  }, [visible, navigate]);
}
