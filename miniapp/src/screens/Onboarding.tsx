import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "../api/problemDetail";
import type { AccountType, OnboardingRequest } from "../api/types";
import { completeOnboarding } from "../api/users";
import { useDictionary } from "../i18n/dictionary";

const ACCOUNT_TYPES: AccountType[] = ["CASH", "BANK_CARD", "BANK_ACCOUNT", "SAVINGS", "OTHER"];

export function Onboarding() {
  const dictionary = useDictionary();
  const queryClient = useQueryClient();
  const [step, setStep] = useState<1 | 2>(1);
  const [accountName, setAccountName] = useState("");
  const [accountType, setAccountType] = useState<AccountType>("CASH");
  const [initialBalance, setInitialBalance] = useState("");
  const [income, setIncome] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (body: OnboardingRequest) => completeOnboarding(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["me"] });
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  function handleNext() {
    if (!accountName.trim() || initialBalance.trim() === "") {
      return;
    }
    setError(null);
    setStep(2);
  }

  function handleFinish() {
    setError(null);
    mutation.mutate({
      accountName: accountName.trim(),
      accountType,
      initialBalance: initialBalance.trim(),
      expectedMonthlyIncome: income.trim() === "" ? null : income.trim(),
    });
  }

  return (
    <div className="flex h-full flex-col justify-center gap-5 bg-navy-900 p-6 text-ivory-50">
      {step === 1 ? (
        <>
          <h1 className="font-heading text-lg font-semibold">{dictionary.onboarding.step1Title}</h1>
          <div>
            <label className="mb-1 block text-xs text-slate-400" htmlFor="account-name">
              {dictionary.onboarding.accountNameLabel}
            </label>
            <input
              id="account-name"
              value={accountName}
              onChange={(event) => setAccountName(event.target.value)}
              placeholder={dictionary.onboarding.accountNamePlaceholder}
              maxLength={60}
              className="w-full rounded-lg bg-navy-800 px-3 py-2 text-sm text-ivory-50"
            />
          </div>
          <div>
            <p className="mb-1 text-xs text-slate-400">{dictionary.onboarding.accountTypeLabel}</p>
            <div className="flex flex-wrap gap-2">
              {ACCOUNT_TYPES.map((type) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setAccountType(type)}
                  className={`rounded-full px-3 py-1 text-sm ${
                    accountType === type ? "bg-emerald-700 text-ivory-50" : "bg-navy-800 text-slate-400"
                  }`}
                >
                  {dictionary.accountTypes[type]}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs text-slate-400" htmlFor="initial-balance">
              {dictionary.onboarding.initialBalanceLabel}
            </label>
            <input
              id="initial-balance"
              type="text"
              inputMode="decimal"
              value={initialBalance}
              onChange={(event) => setInitialBalance(event.target.value)}
              placeholder={dictionary.onboarding.initialBalancePlaceholder}
              className="w-full rounded-lg bg-navy-800 px-3 py-2 text-sm text-ivory-50"
            />
          </div>
          {error ? <p className="text-xs text-warn-400">{error}</p> : null}
          <button
            type="button"
            onClick={handleNext}
            disabled={!accountName.trim() || initialBalance.trim() === ""}
            className="rounded-full bg-gold-400 py-3 text-sm font-semibold text-navy-900 disabled:opacity-50"
          >
            {dictionary.onboarding.next}
          </button>
        </>
      ) : (
        <>
          <h1 className="font-heading text-lg font-semibold">{dictionary.onboarding.step2Title}</h1>
          <div>
            <label className="mb-1 block text-xs text-slate-400" htmlFor="income">
              {dictionary.onboarding.incomeLabel}
            </label>
            <input
              id="income"
              type="text"
              inputMode="decimal"
              value={income}
              onChange={(event) => setIncome(event.target.value)}
              placeholder={dictionary.onboarding.incomePlaceholder}
              className="w-full rounded-lg bg-navy-800 px-3 py-2 text-sm text-ivory-50"
            />
          </div>
          {error ? <p className="text-xs text-warn-400">{error}</p> : null}
          <button
            type="button"
            onClick={handleFinish}
            disabled={mutation.isPending}
            className="rounded-full bg-gold-400 py-3 text-sm font-semibold text-navy-900 disabled:opacity-50"
          >
            {dictionary.onboarding.finish}
          </button>
        </>
      )}
    </div>
  );
}
