import { useState, type ReactNode } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { archiveRecurringRule, createRecurringRule, updateRecurringRule } from "../api/recurring";
import { ApiError } from "../api/problemDetail";
import type { Account, Category, RecurringKind, RecurringRule, RecurringSchedule } from "../api/types";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { todayKey } from "../analytics/month";

export function RecurringFormSheet({
  onClose,
  accounts,
  categories,
  editing,
}: {
  onClose: () => void;
  accounts: Account[];
  categories: Category[];
  editing?: RecurringRule;
}) {
  const dictionary = useDictionary();
  const language = useLanguage();
  const queryClient = useQueryClient();

  const [kind, setKind] = useState<RecurringKind>(editing?.kind ?? "REGULAR");
  const [name, setName] = useState(editing?.name ?? "");
  const [amount, setAmount] = useState(editing?.amount.split(".")[0] ?? "");
  const [categoryId, setCategoryId] = useState<string | null>(editing?.categoryId ?? null);
  const [accountId, setAccountId] = useState<string | null>(editing?.accountId ?? accounts[0]?.id ?? null);
  const [schedule, setSchedule] = useState<RecurringSchedule>(editing?.schedule ?? "MONTHLY");
  const [dayOfMonth, setDayOfMonth] = useState(editing?.dayOfMonth ?? 1);
  const [dayOfWeek, setDayOfWeek] = useState(editing?.dayOfWeek ?? 1);
  const [everyNDays, setEveryNDays] = useState(editing?.everyNDays ?? 30);
  const [startDate, setStartDate] = useState(editing?.startDate ?? todayKey());
  const [autoCreate, setAutoCreate] = useState(editing?.autoCreate ?? false);
  const [remindDaysBefore, setRemindDaysBefore] = useState(editing?.remindDaysBefore ?? 1);
  const [error, setError] = useState<string | null>(null);

  const categoryType = kind === "INCOME" ? "INCOME" : "EXPENSE";
  const relevantCategories = categories.filter((c) => c.type === categoryType);

  const canSave =
    name.trim() !== "" &&
    amount !== "" &&
    amount !== "0" &&
    accountId !== null &&
    (kind === "LOAN" || categoryId !== null) &&
    (schedule !== "EVERY_N_DAYS" || everyNDays > 0);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const resolvedCategoryId = kind === "LOAN" ? null : categoryId;
      if (editing) {
        return updateRecurringRule(editing.id, {
          name: name.trim(),
          amount: `${amount}.00`,
          categoryId: resolvedCategoryId,
          accountId: accountId!,
          schedule,
          dayOfMonth: schedule === "MONTHLY" ? dayOfMonth : null,
          dayOfWeek: schedule === "WEEKLY" ? dayOfWeek : null,
          everyNDays: schedule === "EVERY_N_DAYS" ? everyNDays : null,
          autoCreate,
          remindDaysBefore,
        });
      }
      return createRecurringRule({
        name: name.trim(),
        kind,
        amount: `${amount}.00`,
        categoryId: resolvedCategoryId,
        accountId: accountId!,
        schedule,
        dayOfMonth: schedule === "MONTHLY" ? dayOfMonth : null,
        dayOfWeek: schedule === "WEEKLY" ? dayOfWeek : null,
        everyNDays: schedule === "EVERY_N_DAYS" ? everyNDays : null,
        startDate,
        autoCreate,
        remindDaysBefore,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["recurring"] });
      queryClient.invalidateQueries({ queryKey: ["budget"] });
      onClose();
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  const archiveMutation = useMutation({
    mutationFn: () => archiveRecurringRule(editing!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["recurring"] });
      queryClient.invalidateQueries({ queryKey: ["budget"] });
      onClose();
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end bg-black/50" onClick={onClose}>
      <div
        className="flex max-h-[90vh] flex-col gap-4 overflow-y-auto rounded-t-2xl bg-navy-800 p-5 text-ivory-50"
        onClick={(event) => event.stopPropagation()}
      >
        <p className="text-center font-heading text-lg font-semibold">
          {editing ? dictionary.recurring.formTitleEdit : dictionary.recurring.formTitleNew}
        </p>

        {!editing && (
          <div className="flex gap-2">
            {(["REGULAR", "LOAN", "INCOME"] as RecurringKind[]).map((k) => (
              <button
                key={k}
                type="button"
                onClick={() => {
                  setKind(k);
                  setCategoryId(null);
                }}
                className={`flex-1 rounded-full py-2 text-sm font-medium ${
                  kind === k ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                }`}
              >
                {dictionary.recurring.kindLabel[k]}
              </button>
            ))}
          </div>
        )}

        <Field label={dictionary.recurring.nameLabel}>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder={dictionary.recurring.namePlaceholder}
            maxLength={60}
            className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
          />
        </Field>

        <Field label={dictionary.recurring.amountLabel}>
          <input
            value={amount}
            onChange={(event) => setAmount(event.target.value.replace(/[^\d]/g, ""))}
            inputMode="numeric"
            placeholder="0"
            className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
          />
        </Field>

        {kind !== "LOAN" && (
          <Field label={dictionary.recurring.categoryLabel}>
            <div className="flex flex-wrap gap-2">
              {relevantCategories.map((category) => (
                <button
                  key={category.id}
                  type="button"
                  onClick={() => setCategoryId(category.id)}
                  className={`rounded-full px-3 py-1 text-xs ${
                    categoryId === category.id ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                  }`}
                >
                  {language === "ru" ? category.nameRu : category.nameUz}
                </button>
              ))}
            </div>
          </Field>
        )}

        <Field label={dictionary.recurring.accountLabel}>
          <div className="flex flex-wrap gap-2">
            {accounts.map((account) => (
              <button
                key={account.id}
                type="button"
                onClick={() => setAccountId(account.id)}
                className={`rounded-full px-3 py-1 text-sm ${
                  accountId === account.id ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                }`}
              >
                {account.name}
              </button>
            ))}
          </div>
        </Field>

        <Field label={dictionary.recurring.scheduleFieldLabel}>
          <div className="flex gap-2">
            {(["MONTHLY", "WEEKLY", "EVERY_N_DAYS"] as RecurringSchedule[]).map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => setSchedule(s)}
                className={`flex-1 rounded-full py-2 text-xs font-medium ${
                  schedule === s ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                }`}
              >
                {dictionary.recurring.scheduleLabel[s]}
              </button>
            ))}
          </div>
        </Field>

        {schedule === "MONTHLY" && (
          <Field label={dictionary.recurring.dayOfMonthLabel}>
            <input
              type="number"
              min={1}
              max={31}
              value={dayOfMonth}
              onChange={(event) => setDayOfMonth(Number(event.target.value))}
              className="w-24 rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
            />
          </Field>
        )}

        {schedule === "WEEKLY" && (
          <Field label={dictionary.recurring.dayOfWeekLabel}>
            <div className="flex gap-1">
              {dictionary.recurring.weekdayShort.map((label, index) => {
                const isoDay = index + 1;
                return (
                  <button
                    key={isoDay}
                    type="button"
                    onClick={() => setDayOfWeek(isoDay)}
                    className={`flex-1 rounded-lg py-2 text-xs ${
                      dayOfWeek === isoDay ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                    }`}
                  >
                    {label}
                  </button>
                );
              })}
            </div>
          </Field>
        )}

        {schedule === "EVERY_N_DAYS" && (
          <Field label={dictionary.recurring.everyNDaysLabel}>
            <input
              type="number"
              min={1}
              value={everyNDays}
              onChange={(event) => setEveryNDays(Number(event.target.value))}
              className="w-24 rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
            />
          </Field>
        )}

        {!editing && (
          <Field label={dictionary.recurring.startDateLabel}>
            <input
              type="date"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
              className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
            />
          </Field>
        )}

        <Field label={dictionary.recurring.remindDaysBeforeLabel}>
          <input
            type="number"
            min={0}
            max={14}
            value={remindDaysBefore}
            onChange={(event) => setRemindDaysBefore(Number(event.target.value))}
            className="w-24 rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
          />
        </Field>

        <button
          type="button"
          onClick={() => setAutoCreate((v) => !v)}
          className={`flex items-center justify-between rounded-xl px-3 py-2 text-sm ${
            autoCreate ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
          }`}
        >
          <span>{dictionary.recurring.autoCreateLabel}</span>
          <span>{autoCreate ? "✓" : ""}</span>
        </button>

        {error ? <p className="text-xs text-warn-400">{error}</p> : null}

        <div className="flex gap-2">
          {editing && (
            <button
              type="button"
              onClick={() => archiveMutation.mutate()}
              disabled={archiveMutation.isPending}
              className="rounded-full bg-navy-900 px-4 py-3 text-sm font-medium text-warn-400 disabled:opacity-50"
            >
              {dictionary.recurring.archive}
            </button>
          )}
          <button
            type="button"
            onClick={() => saveMutation.mutate()}
            disabled={!canSave || saveMutation.isPending}
            className="flex-1 rounded-full bg-gold-400 py-3 text-sm font-semibold text-navy-900 disabled:opacity-50"
          >
            {dictionary.recurring.save}
          </button>
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <p className="mb-1 text-xs text-slate-400">{label}</p>
      {children}
    </div>
  );
}
