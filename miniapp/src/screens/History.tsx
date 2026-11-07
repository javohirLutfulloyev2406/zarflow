import { useOutletContext } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import type { TabsOutletContext } from "../app/TabsLayout";
import { listAccounts } from "../api/accounts";
import { listCategories } from "../api/categories";
import { listTransactions } from "../api/transactions";
import type { Transaction } from "../api/types";
import { EmptyStateCard } from "../components/EmptyStateCard";
import { SkeletonCard } from "../components/SkeletonCard";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";

function dayKey(iso: string): string {
  const date = new Date(iso);
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

function groupByDay(transactions: Transaction[]): Array<{ key: string; date: Date; items: Transaction[] }> {
  const groups: Array<{ key: string; date: Date; items: Transaction[] }> = [];
  for (const t of transactions) {
    const key = dayKey(t.transactionDate);
    const last = groups[groups.length - 1];
    if (last && last.key === key) {
      last.items.push(t);
    } else {
      groups.push({ key, date: new Date(t.transactionDate), items: [t] });
    }
  }
  return groups;
}

export function History() {
  const dictionary = useDictionary();
  const language = useLanguage();
  const { openEdit } = useOutletContext<TabsOutletContext>();

  const { data, isPending, isError } = useQuery({
    queryKey: ["transactions"],
    queryFn: () => listTransactions({ size: 100 }),
  });
  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: listCategories });
  const { data: accounts = [] } = useQuery({ queryKey: ["accounts"], queryFn: listAccounts });

  if (isPending) {
    return (
      <div className="flex flex-col gap-3 p-4">
        <SkeletonCard label="" />
        <SkeletonCard label="" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-4">
        <EmptyStateCard>{dictionary.error.generic}</EmptyStateCard>
      </div>
    );
  }

  if (data.items.length === 0) {
    return (
      <div className="p-4">
        <EmptyStateCard>{dictionary.history.empty}</EmptyStateCard>
      </div>
    );
  }

  const now = new Date();
  const today = dayKey(now.toISOString());
  const yesterdayDate = new Date(now);
  yesterdayDate.setDate(now.getDate() - 1);
  const yesterday = dayKey(yesterdayDate.toISOString());
  const locale = language === "ru" ? "ru-RU" : "uz-UZ";

  function dayLabel(key: string, date: Date): string {
    if (key === today) return dictionary.history.today;
    if (key === yesterday) return dictionary.history.yesterday;
    return date.toLocaleDateString(locale, { day: "numeric", month: "long" });
  }

  function categoryLabel(categoryId: string | null): string {
    const category = categories.find((c) => c.id === categoryId);
    if (!category) return "";
    return language === "ru" ? category.nameRu : category.nameUz;
  }

  function accountLabel(accountId: string): string {
    return accounts.find((a) => a.id === accountId)?.name ?? "";
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="font-heading text-lg font-semibold">{dictionary.history.title}</h1>
      {groupByDay(data.items).map((group) => (
        <div key={group.key} className="flex flex-col gap-2">
          <p className="text-xs text-slate-400">{dayLabel(group.key, group.date)}</p>
          <div className="flex flex-col gap-2">
            {group.items.map((transaction) => (
              <button
                key={transaction.id}
                type="button"
                onClick={() => openEdit(transaction)}
                className="flex items-center justify-between rounded-2xl bg-navy-800 p-4 text-left"
              >
                <div className="flex flex-col">
                  <span className="text-sm">
                    {transaction.type === "TRANSFER" ? dictionary.quickAdd.tabTransfer : categoryLabel(transaction.categoryId)}
                  </span>
                  <span className="text-xs text-slate-400">
                    {accountLabel(transaction.accountId)}
                    {transaction.description ? ` · ${transaction.description}` : ""}
                  </span>
                </div>
                <span
                  className={
                    transaction.type === "EXPENSE"
                      ? "text-warn-400"
                      : transaction.type === "INCOME"
                        ? "text-success-400"
                        : "text-ivory-50"
                  }
                >
                  {transaction.type === "TRANSFER"
                    ? formatMoney(transaction.amount, language)
                    : `${transaction.type === "EXPENSE" ? "-" : "+"}${formatMoney(transaction.amount, language)}`}
                </span>
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
