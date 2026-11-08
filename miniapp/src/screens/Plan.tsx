import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { getBudget, getTodayBudget, updateBudget } from "../api/budgets";
import { listCategories } from "../api/categories";
import type { BudgetResponse, Category } from "../api/types";
import { currentMonthKey, monthLabel } from "../analytics/month";
import { EmptyStateCard } from "../components/EmptyStateCard";
import { SkeletonCard } from "../components/SkeletonCard";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";
import { RecurringSection } from "./RecurringSection";

export function Plan() {
  const dictionary = useDictionary();
  const language = useLanguage();
  const month = currentMonthKey();
  const locale = language === "ru" ? "ru-RU" : "uz-UZ";

  const { data, isPending, isError } = useQuery({ queryKey: ["budget", month], queryFn: () => getBudget(month) });
  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: listCategories });
  const { data: today } = useQuery({ queryKey: ["budget", "today"], queryFn: getTodayBudget });

  const expenseCategories = categories.filter((c) => c.type === "EXPENSE");

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="font-heading text-lg font-semibold">
        {dictionary.nav.plan} · {monthLabel(month, locale)}
      </h1>

      {today?.forecastPositive ? (
        <div className="rounded-2xl bg-navy-800 p-3 text-xs text-warn-400">
          {dictionary.budget.forecastOverage(formatMoney(today.forecastOverage ?? "0.00", language))}
        </div>
      ) : null}

      {isPending ? <SkeletonCard label="" /> : null}
      {isError ? <EmptyStateCard>{dictionary.error.generic}</EmptyStateCard> : null}
      {data ? <BudgetForm month={month} initial={data} categories={expenseCategories} /> : null}

      <RecurringSection />
    </div>
  );
}

function BudgetForm({ month, initial, categories }: { month: string; initial: BudgetResponse; categories: Category[] }) {
  const dictionary = useDictionary();
  const language = useLanguage();
  const queryClient = useQueryClient();

  const [expectedIncome, setExpectedIncome] = useState(initial.expectedIncome ?? "");
  const [fixedPayments, setFixedPayments] = useState(initial.fixedPayments ?? "");
  const [fixedManual, setFixedManual] = useState(!initial.fixedPaymentsIsAuto);
  const [loanPayments, setLoanPayments] = useState(initial.loanPayments ?? "");
  const [loanManual, setLoanManual] = useState(!initial.loanPaymentsIsAuto);
  const [plannedSavings, setPlannedSavings] = useState(initial.plannedSavings ?? "");
  const [limits, setLimits] = useState<Record<string, string>>(
    Object.fromEntries(initial.limits.map((l) => [l.categoryId, l.amount])),
  );
  const [message, setMessage] = useState<string | null>(null);

  const freeBudget =
    numberOf(expectedIncome) - numberOf(fixedPayments) - numberOf(loanPayments) - numberOf(plannedSavings);

  const mutation = useMutation({
    mutationFn: () =>
      updateBudget(month, {
        expectedIncome: toMoneyString(expectedIncome),
        fixedPayments: fixedManual ? toMoneyString(fixedPayments) : null,
        loanPayments: loanManual ? toMoneyString(loanPayments) : null,
        plannedSavings: toMoneyString(plannedSavings),
        limits: Object.entries(limits)
            .filter(([, amount]) => amount.trim() !== "" && numberOf(amount) > 0)
            .map(([categoryId, amount]) => ({ categoryId, amount: toMoneyString(amount) })),
      }),
    onSuccess: () => {
      setMessage(dictionary.budget.saved);
      queryClient.invalidateQueries({ queryKey: ["budget"] });
    },
    onError: () => setMessage(dictionary.error.generic),
  });

  function spentFor(categoryId: string): string {
    return initial.limits.find((l) => l.categoryId === categoryId)?.spent ?? "0.00";
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="rounded-2xl bg-navy-800 p-4">
        <NumberField label={dictionary.budget.expectedIncomeLabel} value={expectedIncome} onChange={setExpectedIncome} />
        <AutoNumberField
          label={dictionary.budget.fixedPaymentsLabel}
          value={fixedPayments}
          onChange={setFixedPayments}
          manual={fixedManual}
          onManualChange={setFixedManual}
        />
        <AutoNumberField
          label={dictionary.budget.loanPaymentsLabel}
          value={loanPayments}
          onChange={setLoanPayments}
          manual={loanManual}
          onManualChange={setLoanManual}
        />
        <NumberField label={dictionary.budget.plannedSavingsLabel} value={plannedSavings} onChange={setPlannedSavings} last />

        <div className="mt-3 rounded-xl bg-navy-900 p-3 text-center">
          <p className="text-xs text-slate-400">{dictionary.budget.freeBudgetLabel}</p>
          <p className={`font-heading text-xl font-semibold ${freeBudget < 0 ? "text-warn-400" : "text-success-400"}`}>
            {formatMoney(freeBudget.toFixed(2), language)}
          </p>
        </div>
        {freeBudget < 0 ? <p className="mt-2 text-xs text-warn-400">{dictionary.budget.negativeFreeBudgetWarning}</p> : null}
      </div>

      <div className="rounded-2xl bg-navy-800 p-4">
        <p className="mb-3 text-sm font-medium">{dictionary.budget.limitsTitle}</p>
        <div className="flex flex-col gap-3">
          {categories.map((category) => {
            const amountValue = limits[category.id] ?? "";
            const spent = numberOf(spentFor(category.id));
            const limitNum = numberOf(amountValue);
            const pct = limitNum > 0 ? Math.min(100, (spent / limitNum) * 100) : 0;
            return (
              <div key={category.id}>
                <div className="mb-1 flex items-center justify-between gap-2 text-xs">
                  <span className="flex items-center gap-2">
                    <span className="inline-block h-2 w-2 rounded-full" style={{ backgroundColor: category.color }} />
                    {language === "ru" ? category.nameRu : category.nameUz}
                  </span>
                  <input
                    value={amountValue}
                    onChange={(event) => setLimits((prev) => ({ ...prev, [category.id]: event.target.value }))}
                    inputMode="decimal"
                    placeholder={dictionary.budget.limitAmountPlaceholder}
                    className="w-28 rounded-lg bg-navy-900 px-2 py-1 text-right text-xs text-ivory-50"
                  />
                </div>
                {limitNum > 0 ? (
                  <div className="h-2 overflow-hidden rounded-full bg-navy-900">
                    <div
                      className={`h-full rounded-full ${pct >= 80 ? "bg-warn-400" : "bg-emerald-700"}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                ) : null}
              </div>
            );
          })}
        </div>
      </div>

      {message ? <p className="text-sm text-success-400">{message}</p> : null}

      <button
        type="button"
        onClick={() => {
          setMessage(null);
          mutation.mutate();
        }}
        disabled={mutation.isPending}
        className="rounded-full bg-emerald-700 py-3 text-sm font-medium text-ivory-50 disabled:opacity-50"
      >
        {dictionary.budget.save}
      </button>
    </div>
  );
}

function NumberField({
  label,
  value,
  onChange,
  last,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  last?: boolean;
}) {
  return (
    <div className={last ? "" : "mb-3"}>
      <label className="mb-1 block text-xs text-slate-400">{label}</label>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        inputMode="decimal"
        placeholder="0"
        className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
      />
    </div>
  );
}

function AutoNumberField({
  label,
  value,
  onChange,
  manual,
  onManualChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  manual: boolean;
  onManualChange: (manual: boolean) => void;
}) {
  const dictionary = useDictionary();
  return (
    <div className="mb-3">
      <div className="mb-1 flex items-center justify-between">
        <label className="text-xs text-slate-400">{label}</label>
        <button
          type="button"
          onClick={() => onManualChange(!manual)}
          className="text-xs text-gold-400"
        >
          {manual ? dictionary.budget.useAuto : dictionary.budget.useManual}
        </button>
      </div>
      <div className="relative">
        <input
          value={value}
          onChange={(event) => onChange(event.target.value)}
          disabled={!manual}
          inputMode="decimal"
          placeholder="0"
          className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50 disabled:opacity-60"
        />
        {!manual ? (
          <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400">
            {dictionary.budget.autoLabel}
          </span>
        ) : null}
      </div>
    </div>
  );
}

function numberOf(raw: string): number {
  const n = Number(raw);
  return Number.isFinite(n) ? n : 0;
}

function toMoneyString(raw: string): string {
  const trimmed = raw.trim();
  if (trimmed === "") return "0.00";
  return trimmed.includes(".") ? trimmed : `${trimmed}.00`;
}
