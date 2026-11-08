import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { listAccounts } from "../api/accounts";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/problemDetail";
import { listRecurringRules, listUpcomingOccurrences, payOccurrence } from "../api/recurring";
import type { RecurringRule, UpcomingOccurrence } from "../api/types";
import { todayKey } from "../analytics/month";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";
import { RecurringFormSheet } from "./RecurringFormSheet";

export function RecurringSection() {
  const dictionary = useDictionary();
  const language = useLanguage();
  const [sheet, setSheet] = useState<"new" | RecurringRule | null>(null);

  const { data: accounts = [] } = useQuery({ queryKey: ["accounts"], queryFn: listAccounts });
  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: listCategories });
  const rules = useQuery({ queryKey: ["recurring", "rules"], queryFn: listRecurringRules });
  const upcoming = useQuery({ queryKey: ["recurring", "upcoming", 30], queryFn: () => listUpcomingOccurrences(30) });

  const activeRules = (rules.data ?? []).filter((rule) => rule.active);
  const groups = groupByDate(upcoming.data ?? []);

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-2xl bg-navy-800 p-4">
        <div className="mb-3 flex items-center justify-between">
          <p className="text-sm font-medium">{dictionary.recurring.sectionTitle}</p>
          <button
            type="button"
            onClick={() => setSheet("new")}
            className="text-xs font-semibold text-gold-400"
          >
            {dictionary.recurring.addButton}
          </button>
        </div>

        {activeRules.length === 0 ? (
          <p className="text-xs text-slate-400">{dictionary.recurring.emptyState}</p>
        ) : (
          <div className="flex flex-col gap-2">
            {activeRules.map((rule) => (
              <button
                key={rule.id}
                type="button"
                onClick={() => setSheet(rule)}
                className="flex items-center justify-between rounded-xl bg-navy-900 px-3 py-2 text-left"
              >
                <div>
                  <p className="text-sm font-medium text-ivory-50">{rule.name}</p>
                  <p className="text-xs text-slate-400">
                    {dictionary.recurring.kindLabel[rule.kind]} · {dictionary.recurring.nextDueLabel(rule.nextDueDate)}
                  </p>
                </div>
                <p className="text-sm font-semibold text-ivory-50">{formatMoney(rule.amount, language)}</p>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="rounded-2xl bg-navy-800 p-4">
        <p className="mb-3 text-sm font-medium">{dictionary.recurring.upcomingTitle}</p>
        {groups.length === 0 ? (
          <p className="text-xs text-slate-400">{dictionary.recurring.upcomingEmpty}</p>
        ) : (
          <div className="flex flex-col gap-3">
            {groups.map(({ date, occurrences }) => (
              <div key={date}>
                <p className="mb-1 text-xs text-slate-400">{dueDateLabel(date, dictionary)}</p>
                <div className="flex flex-col gap-1">
                  {occurrences.map((occurrence) => (
                    <OccurrenceRow key={occurrence.occurrenceId} occurrence={occurrence} />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {sheet !== null && (
        <RecurringFormSheet
          onClose={() => setSheet(null)}
          accounts={accounts}
          categories={categories}
          editing={sheet === "new" ? undefined : sheet}
        />
      )}
    </div>
  );
}

function OccurrenceRow({ occurrence }: { occurrence: UpcomingOccurrence }) {
  const dictionary = useDictionary();
  const language = useLanguage();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const payMutation = useMutation({
    mutationFn: () => payOccurrence(occurrence.occurrenceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["recurring"] });
      queryClient.invalidateQueries({ queryKey: ["budget"] });
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  if (occurrence.status !== "PENDING") {
    return (
      <div className="flex items-center justify-between rounded-lg bg-navy-900 px-3 py-2 text-sm">
        <span className="text-slate-400">{occurrence.ruleName}</span>
        <span className="text-xs text-slate-500">
          {occurrence.status === "PAID" ? dictionary.recurring.statusPaid : dictionary.recurring.statusSkipped}
        </span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center justify-between rounded-lg bg-navy-900 px-3 py-2 text-sm">
        <div>
          <p className="text-ivory-50">{occurrence.ruleName}</p>
          <p className="text-xs text-slate-400">{formatMoney(occurrence.amount, language)}</p>
        </div>
        <button
          type="button"
          onClick={() => {
            setError(null);
            payMutation.mutate();
          }}
          disabled={payMutation.isPending}
          className="rounded-full bg-emerald-700 px-3 py-1 text-xs font-medium text-ivory-50 disabled:opacity-50"
        >
          {dictionary.recurring.payButton}
        </button>
      </div>
      {error ? <p className="px-3 text-xs text-warn-400">{error}</p> : null}
    </div>
  );
}

function groupByDate(occurrences: UpcomingOccurrence[]): { date: string; occurrences: UpcomingOccurrence[] }[] {
  const byDate = new Map<string, UpcomingOccurrence[]>();
  for (const occurrence of occurrences) {
    const list = byDate.get(occurrence.dueDate) ?? [];
    list.push(occurrence);
    byDate.set(occurrence.dueDate, list);
  }
  return Array.from(byDate.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, occurrences]) => ({ date, occurrences }));
}

function dueDateLabel(date: string, dictionary: ReturnType<typeof useDictionary>): string {
  const today = todayKey();
  const tomorrow = shiftDateKey(today, 1);
  if (date === today) return dictionary.recurring.dueToday;
  if (date === tomorrow) return dictionary.recurring.dueTomorrow;
  return date;
}

function shiftDateKey(dateKey: string, deltaDays: number): string {
  const date = new Date(`${dateKey}T00:00:00`);
  date.setDate(date.getDate() + deltaDays);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
