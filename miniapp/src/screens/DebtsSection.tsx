import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { listDebts } from "../api/debts";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";
import { DebtFormSheet } from "./DebtFormSheet";

export function DebtsSection() {
  const dictionary = useDictionary();
  const language = useLanguage();
  const navigate = useNavigate();
  const [showForm, setShowForm] = useState(false);

  const { data: debts = [] } = useQuery({ queryKey: ["debts"], queryFn: listDebts });
  const active = debts.filter((d) => d.status === "ACTIVE");

  return (
    <div className="rounded-2xl bg-navy-800 p-4">
      <div className="mb-3 flex items-center justify-between">
        <p className="text-sm font-medium">{dictionary.debts.sectionTitle}</p>
        <button type="button" onClick={() => setShowForm(true)} className="text-xs font-semibold text-gold-400">
          {dictionary.debts.addButton}
        </button>
      </div>

      {active.length === 0 ? (
        <p className="text-xs text-slate-400">{dictionary.debts.emptyState}</p>
      ) : (
        <div className="flex flex-col gap-2">
          {active.map((debt) => {
            const paidFraction =
              1 - Number(debt.outstandingPrincipal) / Math.max(Number(debt.originalAmount), 1);
            return (
              <button
                key={debt.id}
                type="button"
                onClick={() => navigate(`/debts/${debt.id}`)}
                className="flex flex-col gap-2 rounded-xl bg-navy-900 px-3 py-2 text-left"
              >
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium text-ivory-50">{debt.name}</p>
                  <p className="text-sm font-semibold text-ivory-50">
                    {formatMoney(debt.outstandingPrincipal, language)}
                  </p>
                </div>
                <div className="h-1.5 overflow-hidden rounded-full bg-navy-800">
                  <div
                    className="h-full rounded-full bg-emerald-700"
                    style={{ width: `${Math.min(100, Math.max(0, paidFraction * 100))}%` }}
                  />
                </div>
              </button>
            );
          })}
        </div>
      )}

      {showForm ? <DebtFormSheet onClose={() => setShowForm(false)} /> : null}
    </div>
  );
}
