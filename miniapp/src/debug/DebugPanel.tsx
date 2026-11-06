import { useState } from "react";
import { useRawInitData } from "@telegram-apps/sdk-react";

const isDebugEnabled = import.meta.env.DEV && import.meta.env.VITE_DEBUG === "true";

/**
 * Dev-only: lets you grab a real initData sample to feed the backend's golden
 * test (ZARFLOW_TEST_INIT_DATA). Gated so Vite's static DEV replacement lets
 * Rollup tree-shake this whole branch, DebugPanelContent included, out of
 * production builds — verified post-build, not just asserted (see README).
 */
export function DebugPanel() {
  if (!isDebugEnabled) {
    return null;
  }
  return <DebugPanelContent />;
}

function DebugPanelContent() {
  const rawInitData = useRawInitData();
  const [copied, setCopied] = useState(false);

  async function copy() {
    if (!rawInitData) {
      return;
    }
    await navigator.clipboard.writeText(rawInitData);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <button
      type="button"
      onClick={copy}
      className="fixed bottom-20 right-4 z-50 rounded-full bg-warn-400 px-3 py-2 text-xs font-medium text-navy-900"
    >
      {copied ? "Copied!" : "Copy raw initData"}
    </button>
  );
}
