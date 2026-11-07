import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { listAccounts } from "../api/accounts";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/problemDetail";
import { createTransaction, deleteTransaction, transfer, updateTransaction } from "../api/transactions";
import type { Transaction, TransactionType } from "../api/types";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";
import { hapticLightImpact } from "../telegram/haptics";

const KEYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "back"];

export function QuickAddSheet({
  onClose,
  onSaved,
  editing,
}: {
  onClose: () => void;
  onSaved: (transactionId: string | null) => void;
  editing?: Transaction;
}) {
  const dictionary = useDictionary();
  const language = useLanguage();
  const queryClient = useQueryClient();

  const [tab, setTab] = useState<TransactionType>(editing?.type ?? "EXPENSE");
  const [amountDigits, setAmountDigits] = useState(() => (editing ? (editing.amount.split(".")[0] ?? "") : ""));
  const [categoryId, setCategoryId] = useState<string | null>(editing?.categoryId ?? null);
  const [showAllCategories, setShowAllCategories] = useState(false);
  const [accountId, setAccountId] = useState<string | null>(editing?.accountId ?? null);
  const [toAccountId, setToAccountId] = useState<string | null>(null);
  const [note, setNote] = useState(editing?.description ?? "");
  const [error, setError] = useState<string | null>(null);

  const { data: accounts = [] } = useQuery({ queryKey: ["accounts"], queryFn: listAccounts });
  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: listCategories });

  const effectiveAccountId = accountId ?? accounts.find((a) => a.isDefault)?.id ?? accounts[0]?.id ?? null;
  const effectiveToAccountId =
    toAccountId ?? accounts.find((a) => a.id !== effectiveAccountId)?.id ?? null;

  const categoryType = tab === "INCOME" ? "INCOME" : "EXPENSE";
  const relevantCategories = categories.filter((c) => c.type === categoryType);
  const topCategories = relevantCategories.slice(0, 6);
  const extraCategories = relevantCategories.slice(6);

  function pressKey(key: string) {
    if (key === "back") {
      setAmountDigits((prev) => prev.slice(0, -1));
      return;
    }
    setAmountDigits((prev) => {
      const next = (prev + key).replace(/^0+(?=\d)/, "");
      return next.length > 12 ? prev : next;
    });
  }

  const canSave =
    amountDigits !== "" &&
    amountDigits !== "0" &&
    (tab === "TRANSFER"
      ? effectiveAccountId !== null && effectiveToAccountId !== null && effectiveAccountId !== effectiveToAccountId
      : effectiveAccountId !== null && categoryId !== null);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const amount = `${amountDigits}.00`;
      const description = note.trim() === "" ? null : note.trim();
      if (tab === "TRANSFER") {
        const result = await transfer({
          fromAccountId: effectiveAccountId!,
          toAccountId: effectiveToAccountId!,
          amount,
          description,
        });
        return { id: null, result };
      }
      if (editing) {
        const result = await updateTransaction(editing.id, {
          accountId: effectiveAccountId!,
          categoryId: categoryId!,
          amount,
          necessity: null,
          transactionDate: null,
          description,
        });
        return { id: result.id, result };
      }
      const result = await createTransaction(
        { accountId: effectiveAccountId!, categoryId: categoryId!, type: tab, amount, description },
        crypto.randomUUID(),
      );
      return { id: result.id, result };
    },
    onSuccess: ({ id }) => {
      hapticLightImpact();
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
      onSaved(id);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteTransaction(editing!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["summary"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
      onSaved(null);
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
        {!editing && (
          <div className="flex gap-2">
            {(["EXPENSE", "INCOME", "TRANSFER"] as TransactionType[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => {
                  setTab(t);
                  setCategoryId(null);
                }}
                className={`flex-1 rounded-full py-2 text-sm font-medium ${
                  tab === t ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                }`}
              >
                {t === "EXPENSE" ? dictionary.quickAdd.tabExpense : t === "INCOME" ? dictionary.quickAdd.tabIncome : dictionary.quickAdd.tabTransfer}
              </button>
            ))}
          </div>
        )}

        <p className="text-center font-heading text-2xl font-semibold">
          {formatMoney(amountDigits === "" ? "0" : amountDigits, language)}
        </p>

        <div className="grid grid-cols-3 gap-2">
          {KEYS.map((key) => (
            <button
              key={key}
              type="button"
              onClick={() => pressKey(key)}
              className="rounded-xl bg-navy-900 py-3 text-lg font-medium text-ivory-50"
            >
              {key === "back" ? "⌫" : key}
            </button>
          ))}
        </div>

        {tab === "TRANSFER" ? (
          <>
            <div>
              <p className="mb-1 text-xs text-slate-400">{dictionary.quickAdd.fromAccountLabel}</p>
              <div className="flex flex-wrap gap-2">
                {accounts.map((account) => (
                  <button
                    key={account.id}
                    type="button"
                    onClick={() => setAccountId(account.id)}
                    className={`rounded-full px-3 py-1 text-sm ${
                      effectiveAccountId === account.id ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                    }`}
                  >
                    {account.name}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <p className="mb-1 text-xs text-slate-400">{dictionary.quickAdd.toAccountLabel}</p>
              <div className="flex flex-wrap gap-2">
                {accounts
                  .filter((account) => account.id !== effectiveAccountId)
                  .map((account) => (
                    <button
                      key={account.id}
                      type="button"
                      onClick={() => setToAccountId(account.id)}
                      className={`rounded-full px-3 py-1 text-sm ${
                        effectiveToAccountId === account.id ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                      }`}
                    >
                      {account.name}
                    </button>
                  ))}
              </div>
            </div>
          </>
        ) : (
          <>
            <div>
              <p className="mb-1 text-xs text-slate-400">{dictionary.quickAdd.categoriesTitle}</p>
              <div className="grid grid-cols-3 gap-2">
                {(showAllCategories ? relevantCategories : topCategories).map((category) => (
                  <button
                    key={category.id}
                    type="button"
                    onClick={() => setCategoryId(category.id)}
                    className={`rounded-xl px-2 py-2 text-center text-xs ${
                      categoryId === category.id ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-300"
                    }`}
                  >
                    {language === "ru" ? category.nameRu : category.nameUz}
                  </button>
                ))}
                {!showAllCategories && extraCategories.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setShowAllCategories(true)}
                    className="rounded-xl bg-navy-900 px-2 py-2 text-center text-xs text-slate-400"
                  >
                    {dictionary.quickAdd.other}
                  </button>
                )}
              </div>
            </div>

            <div>
              <p className="mb-1 text-xs text-slate-400">{dictionary.quickAdd.accountLabel}</p>
              <div className="flex flex-wrap gap-2">
                {accounts.map((account) => (
                  <button
                    key={account.id}
                    type="button"
                    onClick={() => setAccountId(account.id)}
                    className={`rounded-full px-3 py-1 text-sm ${
                      effectiveAccountId === account.id ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"
                    }`}
                  >
                    {account.name}
                  </button>
                ))}
              </div>
            </div>
          </>
        )}

        <input
          value={note}
          onChange={(event) => setNote(event.target.value)}
          placeholder={dictionary.quickAdd.notePlaceholder}
          maxLength={280}
          className="w-full rounded-lg bg-navy-900 px-3 py-2 text-sm text-ivory-50"
        />

        {error ? <p className="text-xs text-warn-400">{error}</p> : null}

        <div className="flex gap-2">
          {editing && (
            <button
              type="button"
              onClick={() => deleteMutation.mutate()}
              disabled={deleteMutation.isPending}
              className="rounded-full bg-navy-900 px-4 py-3 text-sm font-medium text-warn-400 disabled:opacity-50"
            >
              {dictionary.quickAdd.delete}
            </button>
          )}
          <button
            type="button"
            onClick={() => saveMutation.mutate()}
            disabled={!canSave || saveMutation.isPending}
            className="flex-1 rounded-full bg-gold-400 py-3 text-sm font-semibold text-navy-900 disabled:opacity-50"
          >
            {dictionary.quickAdd.save}
          </button>
        </div>
      </div>
    </div>
  );
}
