import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { getTodayBudget } from "../api/budgets";
import { listCategories } from "../api/categories";
import { listUpcomingOccurrences } from "../api/recurring";
import { getTodayAndMonth } from "../api/summary";
import type { TodayBudgetResponse, UpcomingOccurrence } from "../api/types";
import { todayKey } from "../analytics/month";
import { SemicircleGauge } from "../budget/SemicircleGauge";
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
  const name = user?.displayName ?? user?.firstName ?? "";

  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: listCategories });
  const summary = useQuery({ queryKey: ["summary"], queryFn: getTodayAndMonth });
  const budgetToday = useQuery({ queryKey: ["budget", "today"], queryFn: getTodayBudget });
  const upcoming = useQuery({ queryKey: ["recurring", "upcoming", 7], queryFn: () => listUpcomingOccurrences(7) });

  function categoryLabel(categoryId: string | null): string {
    const category = categories.find((c) => c.id === categoryId);
    if (!category) return "";
    return language === "ru" ? category.nameRu : category.nameUz;
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <header className="flex items-center justify-between">
        <h1 className="font-heading text-lg font-semibold">{dictionary.greeting(name)}</h1>
        <Link
          to="/profile"
          aria-label="Profile"
          className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-700"
        >
          <span className="font-heading text-sm font-bold text-gold-400">
            {(name || "Z").charAt(0).toUpperCase()}
          </span>
        </Link>
      </header>

      {budgetToday.data ? <BudgetHeroCard data={budgetToday.data} /> : null}

      {upcoming.data && upcoming.data.length > 0 ? <UpcomingPaymentsCard occurrences={upcoming.data} /> : null}

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

function UpcomingPaymentsCard({ occurrences }: { occurrences: UpcomingOccurrence[] }) {
  const dictionary = useDictionary();
  const language = useLanguage();
  const today = todayKey();
  const tomorrow = shiftDateKey(today, 1);

  const pending = occurrences
    .filter((o) => o.status === "PENDING")
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate));

  if (pending.length === 0) {
    return null;
  }

  return (
    <div className="rounded-2xl bg-navy-800 p-4">
      <p className="mb-2 text-xs text-slate-400">{dictionary.recurring.homeCardTitle}</p>
      <div className="flex flex-col gap-2">
        {pending.map((occurrence) => {
          const dueSoon = occurrence.dueDate === today || occurrence.dueDate === tomorrow;
          return (
            <div key={occurrence.occurrenceId} className="flex items-center justify-between text-sm">
              <span className={dueSoon ? "text-warn-400" : "text-ivory-50"}>{occurrence.ruleName}</span>
              <span className={`text-xs ${dueSoon ? "text-warn-400" : "text-slate-400"}`}>
                {formatMoney(occurrence.amount, language)}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function shiftDateKey(dateKey: string, deltaDays: number): string {
  const date = new Date(`${dateKey}T00:00:00`);
  date.setDate(date.getDate() + deltaDays);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function BudgetHeroCard({ data }: { data: TodayBudgetResponse }) {
  const dictionary = useDictionary();
  const language = useLanguage();

  if (!data.hasBudget) {
    return (
      <Link to="/plan" className="block rounded-2xl bg-navy-800 p-4 text-center">
        <p className="text-sm font-medium">{dictionary.budget.setupTitle}</p>
        <p className="mt-1 text-xs font-semibold text-gold-400">{dictionary.budget.setupCta}</p>
      </Link>
    );
  }

  const dailyAllowance = Number(data.dailyAllowance);
  const spentToday = Number(data.spentToday);
  const usedFraction = dailyAllowance > 0 ? spentToday / dailyAllowance : 1;

  return (
    <div className="rounded-2xl bg-navy-800 p-4">
      <div className="flex items-center gap-4">
        <SemicircleGauge usedFraction={usedFraction} overspent={data.overspentToday} />
        <div className="flex-1">
          {data.overspentToday ? (
            <p className="font-heading text-lg font-semibold text-warn-400">
              {dictionary.budget.overspentLine(formatMoney(data.overspentAmount ?? "0.00", language))}
            </p>
          ) : (
            <p className="font-heading text-2xl font-semibold text-ivory-50">
              {formatMoney(data.todayLeft ?? "0.00", language)}
            </p>
          )}
          <p className="text-xs text-slate-400">
            {dictionary.budget.dailyAllowanceLabel} {formatMoney(data.dailyAllowance ?? "0.00", language)}
          </p>
        </div>
      </div>

      {data.forecastPositive ? (
        <p className="mt-2 text-xs text-warn-400">
          {dictionary.budget.forecastOverage(formatMoney(data.forecastOverage ?? "0.00", language))}
        </p>
      ) : null}

      <div className="mt-3 grid grid-cols-2 gap-2">
        <div className="rounded-xl bg-navy-900 p-2 text-center">
          <p className="text-xs text-slate-400">{dictionary.budget.todaySpentTile}</p>
          <p className="text-sm font-semibold">{formatMoney(data.spentToday ?? "0.00", language)}</p>
        </div>
        <div className="rounded-xl bg-navy-900 p-2 text-center">
          <p className="text-xs text-slate-400">{dictionary.budget.tomorrowTile}</p>
          <p className="text-sm font-semibold">
            {data.tomorrowAllowance ? `≈ ${formatMoney(data.tomorrowAllowance, language)}` : "—"}
          </p>
        </div>
      </div>
    </div>
  );
}
