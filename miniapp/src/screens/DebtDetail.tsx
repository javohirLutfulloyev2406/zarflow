import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { listAccounts } from "../api/accounts";
import { getDebt, getDebtPayments, getDebtProgress, getDebtSchedule, payDebt, simulateDebt } from "../api/debts";
import { ApiError } from "../api/problemDetail";
import type { Debt, DebtPayment, DebtScheduleRow } from "../api/types";
import { todayKey } from "../analytics/month";
import { EmptyStateCard } from "../components/EmptyStateCard";
import { SkeletonCard } from "../components/SkeletonCard";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";
import { useBackButton } from "../telegram/useBackButton";
import { DebtFormSheet } from "./DebtFormSheet";

const SIMULATOR_CHIPS = [0, 200_000, 300_000, 500_000];

export function DebtDetail() {
  useBackButton(true);
  const { id } = useParams<{ id: string }>();
  const dictionary = useDictionary();
  const navigate = useNavigate();

  const debtQuery = useQuery({ queryKey: ["debts", id], queryFn: () => getDebt(id!), enabled: !!id });

  if (debtQuery.isPending) {
    return (
      <div className="flex flex-col gap-4 p-4">
        <SkeletonCard label="" />
      </div>
    );
  }
  if (debtQuery.isError || !debtQuery.data) {
    return (
      <div className="flex flex-col gap-4 p-4">
        <EmptyStateCard>{dictionary.error.generic}</EmptyStateCard>
      </div>
    );
  }

  return <DebtDetailBody id={id!} debt={debtQuery.data} onBack={() => navigate(-1)} />;
}

function DebtDetailBody({ id, debt }: { id: string; debt: Debt; onBack: () => void }) {
  const dictionary = useDictionary();
  const language = useLanguage();
  const queryClient = useQueryClient();
  const [showEdit, setShowEdit] = useState(false);
  const [extraMonthly, setExtraMonthly] = useState(0);
  const [customExtra, setCustomExtra] = useState("");

  const progressQuery = useQuery({ queryKey: ["debts", id, "progress"], queryFn: () => getDebtProgress(id) });
  const scheduleQuery = useQuery({ queryKey: ["debts", id, "schedule"], queryFn: () => getDebtSchedule(id) });
  const paymentsQuery = useQuery({ queryKey: ["debts", id, "payments"], queryFn: () => getDebtPayments(id) });
  const simulateQuery = useQuery({
    queryKey: ["debts", id, "simulate", extraMonthly],
    queryFn: () => simulateDebt(id, `${extraMonthly}.00`),
    enabled: extraMonthly > 0,
  });

  const nextRow = scheduleQuery.data?.find((r) => !r.paid) ?? null;
  const originalAmount = Number(debt.originalAmount);
  const outstanding = Number(debt.outstandingPrincipal);
  const paidFraction = originalAmount > 0 ? 1 - outstanding / originalAmount : 0;

  return (
    <div className="flex flex-col gap-4 p-4">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-lg font-semibold">{debt.name}</h1>
        <button type="button" onClick={() => setShowEdit(true)} aria-label={dictionary.debts.save} className="text-lg text-gold-400">
          ✎
        </button>
      </div>

      <div className="rounded-2xl bg-navy-800 p-4">
        <p className="text-xs text-slate-400">{dictionary.debts.outstandingLabel}</p>
        <p className="font-heading text-2xl font-semibold text-ivory-50">
          {formatMoney(debt.outstandingPrincipal, language)}
        </p>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-navy-900">
          <div
            className="h-full rounded-full bg-emerald-700"
            style={{ width: `${Math.min(100, Math.max(0, paidFraction * 100))}%` }}
          />
        </div>
        <p className="mt-2 text-xs text-slate-400">
          {dictionary.debts.paidPercentLabel}: {progressQuery.data ? progressQuery.data.paidPercent : "0"}%
        </p>
        {debt.scheduleIsEstimated ? (
          <p className="mt-1 text-xs text-slate-500">({dictionary.debts.estimatedLabel})</p>
        ) : null}
      </div>

      <div className="grid grid-cols-3 gap-2">
        <Tile label={dictionary.debts.monthlyPaymentLabel} value={formatMoney(debt.monthlyPayment, language)} />
        <Tile label={dictionary.debts.monthsLeftLabel} value={String(progressQuery.data?.monthsLeft ?? "—")} />
        <Tile label={dictionary.debts.closeDateLabel} value={progressQuery.data?.estimatedCloseDate ?? debt.endDate} />
      </div>

      {debt.status === "ACTIVE" ? (
        <NextPaymentCard debtId={id} nextRow={nextRow} />
      ) : (
        <div className="rounded-2xl bg-navy-800 p-4 text-center text-sm text-success-400">
          {dictionary.debts.debtClosedCelebration}
        </div>
      )}

      {debt.status === "ACTIVE" ? (
        <div className="rounded-2xl bg-navy-800 p-4">
          <p className="mb-3 text-sm font-medium">{dictionary.debts.simulatorTitle}</p>
          <div className="flex flex-wrap gap-2">
            {SIMULATOR_CHIPS.map((chip) => (
              <button
                key={chip}
                type="button"
                onClick={() => {
                  setExtraMonthly(chip);
                  setCustomExtra("");
                }}
                className={`rounded-full px-3 py-1 text-xs ${
                  extraMonthly === chip && customExtra === "" ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                }`}
              >
                {chip === 0 ? "0" : `+${formatMoney(String(chip), language)}`}
              </button>
            ))}
            <input
              value={customExtra}
              onChange={(event) => {
                const digits = event.target.value.replace(/[^\d]/g, "");
                setCustomExtra(digits);
                setExtraMonthly(digits === "" ? 0 : Number(digits));
              }}
              placeholder={dictionary.debts.simulatorCustomPlaceholder}
              inputMode="numeric"
              className="w-28 rounded-full bg-navy-900 px-3 py-1 text-xs text-ivory-50"
            />
          </div>
          <p className="mt-3 text-sm text-ivory-50">
            {extraMonthly > 0 && simulateQuery.data
              ? dictionary.debts.simulatorResultSaved(
                  simulateQuery.data.monthsSaved, formatMoney(simulateQuery.data.interestSaved, language),
                )
              : dictionary.debts.simulatorResultNone}
          </p>
        </div>
      ) : null}

      <div className="rounded-2xl bg-navy-800 p-4">
        <p className="mb-3 text-sm font-medium">{dictionary.debts.historyTitle}</p>
        {!paymentsQuery.data || paymentsQuery.data.length === 0 ? (
          <p className="text-xs text-slate-400">{dictionary.debts.historyEmpty}</p>
        ) : (
          <div className="flex flex-col gap-2">
            {paymentsQuery.data.map((payment) => (
              <PaymentRow key={payment.id} payment={payment} />
            ))}
          </div>
        )}
      </div>

      {showEdit ? (
        <DebtFormSheet
          editing={debt}
          onClose={() => {
            setShowEdit(false);
            queryClient.invalidateQueries({ queryKey: ["debts"] });
          }}
        />
      ) : null}
    </div>
  );
}

function Tile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-navy-900 p-2 text-center">
      <p className="text-xs text-slate-400">{label}</p>
      <p className="text-sm font-semibold text-ivory-50">{value}</p>
    </div>
  );
}

function NextPaymentCard({ debtId, nextRow }: { debtId: string; nextRow: DebtScheduleRow | null }) {
  const dictionary = useDictionary();
  const language = useLanguage();
  const queryClient = useQueryClient();
  const { data: accounts = [] } = useQuery({ queryKey: ["accounts"], queryFn: listAccounts });

  const defaultTotal = nextRow ? Number(nextRow.principal) + Number(nextRow.interest) + Number(nextRow.fee) : 0;
  const [amount, setAmount] = useState(String(Math.round(defaultTotal)));
  const [paidOn, setPaidOn] = useState(todayKey());
  const [accountId, setAccountId] = useState<string | null>(null);
  const [extra, setExtra] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const effectiveAccountId = accountId ?? accounts.find((a) => a.isDefault)?.id ?? accounts[0]?.id ?? null;

  const payMutation = useMutation({
    mutationFn: () =>
      payDebt(debtId, { amount: `${amount}.00`, paidOn, accountId: effectiveAccountId!, extra }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["debts"] });
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  return (
    <div className="rounded-2xl bg-navy-800 p-4">
      <p className="mb-2 text-sm font-medium">{dictionary.debts.nextPaymentTitle}</p>
      {!nextRow ? (
        <p className="text-xs text-slate-400">{dictionary.debts.noNextPayment}</p>
      ) : (
        <p className="mb-3 text-xs text-slate-400">
          {nextRow.dueDate} — {formatMoney(String(defaultTotal.toFixed(2)), language)}
        </p>
      )}

      <div className="flex flex-col gap-2">
        <input
          value={amount}
          onChange={(event) => setAmount(event.target.value.replace(/[^\d]/g, ""))}
          inputMode="numeric"
          placeholder={dictionary.debts.payAmountLabel}
          className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
        />
        <input
          type="date"
          value={paidOn}
          onChange={(event) => setPaidOn(event.target.value)}
          className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
        />
        <div className="flex flex-wrap gap-2">
          {accounts.map((account) => (
            <button
              key={account.id}
              type="button"
              onClick={() => setAccountId(account.id)}
              className={`rounded-full px-3 py-1 text-xs ${
                effectiveAccountId === account.id ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
              }`}
            >
              {account.name}
            </button>
          ))}
        </div>
        <button
          type="button"
          onClick={() => setExtra((v) => !v)}
          className={`flex items-center justify-between rounded-xl px-3 py-2 text-xs ${
            extra ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
          }`}
        >
          <span>{dictionary.debts.payExtraToggle}</span>
          <span>{extra ? "✓" : ""}</span>
        </button>
        {error ? <p className="text-xs text-warn-400">{error}</p> : null}
        <button
          type="button"
          onClick={() => {
            setError(null);
            payMutation.mutate();
          }}
          disabled={amount === "" || Number(amount) <= 0 || !effectiveAccountId || payMutation.isPending}
          className="rounded-full bg-emerald-700 py-3 text-sm font-medium text-ivory-50 disabled:opacity-50"
        >
          {dictionary.debts.payButton}
        </button>
      </div>
    </div>
  );
}

function PaymentRow({ payment }: { payment: DebtPayment }) {
  const language = useLanguage();
  const total = Number(payment.principal) + Number(payment.interest) + Number(payment.fee);
  const principalShare = total > 0 ? (Number(payment.principal) / total) * 100 : 0;

  return (
    <div className="rounded-xl bg-navy-900 p-3">
      <div className="mb-1 flex items-center justify-between text-sm">
        <span className="text-ivory-50">{payment.paidOn}</span>
        <span className="font-medium text-ivory-50">{formatMoney(String(total.toFixed(2)), language)}</span>
      </div>
      <div className="flex h-1.5 overflow-hidden rounded-full bg-navy-800">
        <div className="h-full bg-emerald-700" style={{ width: `${principalShare}%` }} />
        <div className="h-full bg-warn-400" style={{ width: `${100 - principalShare}%` }} />
      </div>
    </div>
  );
}
