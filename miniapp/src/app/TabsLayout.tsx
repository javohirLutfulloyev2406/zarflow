import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";

import { BottomNav } from "../components/BottomNav";
import { deleteTransaction } from "../api/transactions";
import type { Transaction } from "../api/types";
import { useDictionary } from "../i18n/dictionary";
import { QuickAddSheet } from "../screens/QuickAddSheet";
import { useBackButton } from "../telegram/useBackButton";

export interface TabsOutletContext {
  openEdit: (transaction: Transaction) => void;
}

const UNDO_WINDOW_MS = 5000;

export function TabsLayout() {
  useBackButton(false);
  const dictionary = useDictionary();
  const queryClient = useQueryClient();
  const [sheet, setSheet] = useState<{ editing?: Transaction } | null>(null);
  const [undoId, setUndoId] = useState<string | null>(null);

  useEffect(() => {
    if (!undoId) {
      return;
    }
    const timer = window.setTimeout(() => setUndoId(null), UNDO_WINDOW_MS);
    return () => window.clearTimeout(timer);
  }, [undoId]);

  function handleSaved(transactionId: string | null, wasEdit: boolean) {
    setSheet(null);
    if (transactionId && !wasEdit) {
      setUndoId(transactionId);
    }
  }

  async function handleUndo() {
    if (!undoId) {
      return;
    }
    const id = undoId;
    setUndoId(null);
    await deleteTransaction(id);
    queryClient.invalidateQueries({ queryKey: ["summary"] });
    queryClient.invalidateQueries({ queryKey: ["transactions"] });
    queryClient.invalidateQueries({ queryKey: ["accounts"] });
  }

  const context: TabsOutletContext = {
    openEdit: (transaction) => setSheet({ editing: transaction }),
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto">
        <Outlet context={context} />
      </div>
      <BottomNav onAddClick={() => setSheet({})} />
      {sheet ? (
        <QuickAddSheet
          editing={sheet.editing}
          onClose={() => setSheet(null)}
          onSaved={(id) => handleSaved(id, Boolean(sheet.editing))}
        />
      ) : null}
      {undoId ? (
        <div className="fixed inset-x-4 bottom-20 z-40 flex items-center justify-between rounded-xl bg-navy-800 px-4 py-3 text-sm text-ivory-50 shadow-lg">
          <span>{dictionary.quickAdd.savedToast}</span>
          <button type="button" onClick={handleUndo} className="font-semibold text-gold-400">
            {dictionary.quickAdd.undo}
          </button>
        </div>
      ) : null}
    </div>
  );
}
