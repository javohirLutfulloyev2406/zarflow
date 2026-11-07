import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { listCategories } from "../api/categories";
import { getTodayAndMonth } from "../api/summary";
import { EmptyStateCard } from "../components/EmptyStateCard";
import { SkeletonCard } from "../components/SkeletonCard";
import { useAuth } from "../auth/AuthProvider";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";

export function Home() {
  const dictionary = useDictionary();
  const language = useLanguage();
  const { user } = useAuth();

  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: listCategories });
  const summary = useQuery({ queryKey: ["summary"], queryFn: getTodayAndMonth });

  function categoryLabel(categoryId: string | null): string {
    const category = categories.find((c) => c.id === categoryId);
    if (!category) return "";
    return language === "ru" ? category.nameRu : category.nameUz;
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <header className="flex items-center justify-between">
        <h1 className="font-heading text-lg font-semibold">
          {dictionary.greeting(user?.firstName ?? "")}
        </h1>
        <Link
          to="/profile"
          aria-label="Profile"
          className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-700"
        >
          <span className="font-heading text-sm font-bold text-gold-400">
            {(user?.firstName ?? "Z").charAt(0).toUpperCase()}
          </span>
        </Link>
      </header>

      {summary.isPending ? (
        <>
          <SkeletonCard label={dictionary.home.todaySpent} />
          <SkeletonCard label={dictionary.home.balancesTitle} />
        </>
      ) : summary.isError ? (
        <EmptyStateCard>{dictionary.error.generic}</EmptyStateCard>
      ) : (
        <>
          <div className="rounded-2xl bg-navy-800 p-4">
            <p className="text-xs text-slate-400">{dictionary.home.todaySpent}</p>
            <p className="font-heading text-2xl font-semibold text-ivory-50">
              {formatMoney(summary.data.today.total, language)}
            </p>
            <div className="mt-3 flex flex-col gap-2">
              {summary.data.today.expenses.length === 0 ? (
                <p className="text-xs text-slate-400">{dictionary.home.noExpensesToday}</p>
              ) : (
                summary.data.today.expenses.map((expense) => (
                  <div key={expense.id} className="flex items-center justify-between text-sm">
                    <span>{categoryLabel(expense.categoryId)}</span>
                    <span className="text-warn-400">-{formatMoney(expense.amount, language)}</span>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2">
            <div className="rounded-2xl bg-navy-800 p-3 text-center">
              <p className="text-xs text-slate-400">{dictionary.home.monthIncome}</p>
              <p className="text-sm font-semibold text-success-400">
                {formatMoney(summary.data.month.income, language)}
              </p>
            </div>
            <div className="rounded-2xl bg-navy-800 p-3 text-center">
              <p className="text-xs text-slate-400">{dictionary.home.monthExpense}</p>
              <p className="text-sm font-semibold text-warn-400">
                {formatMoney(summary.data.month.expense, language)}
              </p>
            </div>
            <div className="rounded-2xl bg-navy-800 p-3 text-center">
              <p className="text-xs text-slate-400">{dictionary.home.monthNet}</p>
              <p className="text-sm font-semibold text-ivory-50">
                {formatMoney(summary.data.month.net, language)}
              </p>
            </div>
          </div>

          <div className="rounded-2xl bg-navy-800 p-4">
            <p className="mb-2 text-xs text-slate-400">{dictionary.home.balancesTitle}</p>
            <div className="flex flex-col gap-2">
              {summary.data.balances.map((balance) => (
                <div key={balance.accountId} className="flex items-center justify-between text-sm">
                  <span>{balance.accountName}</span>
                  <span className="font-medium">{formatMoney(balance.balance, language)}</span>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
