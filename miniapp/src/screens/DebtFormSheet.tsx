import { useState, type ReactNode } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createDebt, updateDebt } from "../api/debts";
import { ApiError } from "../api/problemDetail";
import type { Debt, DebtInterestMethod, DebtType } from "../api/types";
import { useDictionary } from "../i18n/dictionary";
import { todayKey } from "../analytics/month";

const TYPES: DebtType[] = ["BANK_LOAN", "INSTALLMENT", "CREDIT_CARD", "PERSONAL_DEBT", "OTHER"];
const METHODS: DebtInterestMethod[] = ["ANNUITY_MONTHLY", "ACTUAL_365"];

export function DebtFormSheet({ onClose, editing }: { onClose: () => void; editing?: Debt }) {
  const dictionary = useDictionary();
  const queryClient = useQueryClient();

  const [name, setName] = useState(editing?.name ?? "");
  const [lender, setLender] = useState(editing?.lender ?? "");
  const [type, setType] = useState<DebtType>(editing?.type ?? "BANK_LOAN");
  const [originalAmount, setOriginalAmount] = useState(editing?.originalAmount.split(".")[0] ?? "");
  const [annualRate, setAnnualRate] = useState(editing?.annualRate ?? "0");
  const [interestMethod, setInterestMethod] = useState<DebtInterestMethod>(editing?.interestMethod ?? "ANNUITY_MONTHLY");
  const [startDate, setStartDate] = useState(editing?.startDate ?? todayKey());
  const [termMonths, setTermMonths] = useState("12");
  const [paymentDay, setPaymentDay] = useState(editing?.paymentDay ?? 10);
  const [error, setError] = useState<string | null>(null);

  const canSave =
    name.trim() !== "" &&
    (editing || (originalAmount !== "" && Number(originalAmount) > 0 && Number(termMonths) > 0));

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editing) {
        return updateDebt(editing.id, {
          name: name.trim(),
          lender: lender.trim() === "" ? null : lender.trim(),
          annualRate,
          interestMethod,
          paymentDay,
        });
      }
      return createDebt({
        name: name.trim(),
        lender: lender.trim() === "" ? null : lender.trim(),
        type,
        originalAmount: `${originalAmount}.00`,
        annualRate,
        interestMethod,
        startDate,
        termMonths: Number(termMonths),
        paymentDay,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["debts"] });
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
          {editing ? dictionary.debts.formTitleEdit : dictionary.debts.formTitleNew}
        </p>

        <Field label={dictionary.debts.nameLabel}>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder={dictionary.debts.namePlaceholder}
            maxLength={60}
            className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
          />
        </Field>

        <Field label={dictionary.debts.lenderLabel}>
          <input
            value={lender}
            onChange={(event) => setLender(event.target.value)}
            className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
          />
        </Field>

        {!editing && (
          <Field label={dictionary.debts.typeFieldLabel}>
            <div className="flex flex-wrap gap-2">
              {TYPES.map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => setType(t)}
                  className={`rounded-full px-3 py-1 text-xs ${
                    type === t ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                  }`}
                >
                  {dictionary.debts.typeLabel[t]}
                </button>
              ))}
            </div>
          </Field>
        )}

        {!editing && (
          <Field label={dictionary.debts.originalAmountLabel}>
            <input
              value={originalAmount}
              onChange={(event) => setOriginalAmount(event.target.value.replace(/[^\d]/g, ""))}
              inputMode="numeric"
              placeholder="0"
              className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
            />
          </Field>
        )}

        <Field label={dictionary.debts.annualRateLabel}>
          <input
            value={annualRate}
            onChange={(event) => setAnnualRate(event.target.value.replace(/[^\d.]/g, ""))}
            inputMode="decimal"
            className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
          />
        </Field>

        <Field label={dictionary.debts.interestMethodFieldLabel}>
          <div className="flex gap-2">
            {METHODS.map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => setInterestMethod(m)}
                className={`flex-1 rounded-full py-2 text-xs font-medium ${
                  interestMethod === m ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                }`}
              >
                {dictionary.debts.interestMethodLabel[m]}
              </button>
            ))}
          </div>
        </Field>

        {!editing && (
          <Field label={dictionary.debts.startDateLabel}>
            <input
              type="date"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
              className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
            />
          </Field>
        )}

        {!editing && (
          <Field label={dictionary.debts.termMonthsLabel}>
            <input
              type="number"
              min={1}
              max={600}
              value={termMonths}
              onChange={(event) => setTermMonths(event.target.value)}
              className="w-24 rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
            />
          </Field>
        )}

        <Field label={dictionary.debts.paymentDayLabel}>
          <input
            type="number"
            min={1}
            max={31}
            value={paymentDay}
            onChange={(event) => setPaymentDay(Number(event.target.value))}
            className="w-24 rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
          />
        </Field>

        {error ? <p className="text-xs text-warn-400">{error}</p> : null}

        <button
          type="button"
          onClick={() => saveMutation.mutate()}
          disabled={!canSave || saveMutation.isPending}
          className="rounded-full bg-gold-400 py-3 text-sm font-semibold text-navy-900 disabled:opacity-50"
        >
          {dictionary.debts.save}
        </button>
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
