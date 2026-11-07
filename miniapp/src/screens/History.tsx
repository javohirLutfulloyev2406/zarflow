import { useMemo, useState } from "react";
import { useOutletContext, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import type { TabsOutletContext } from "../app/TabsLayout";
import { getAnalyticsCalendar, getAnalyticsDay } from "../api/analytics";
import { listAccounts } from "../api/accounts";
import { listCategories } from "../api/categories";
import { listTransactions } from "../api/transactions";
import type { Transaction } from "../api/types";
import { CalendarHeatmap } from "../analytics/CalendarHeatmap";
import { currentMonthKey, monthLabel, parseMonthKey, shiftMonth } from "../analytics/month";
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
  const [searchParams, setSearchParams] = useSearchParams();

  const categoryFilterId = searchParams.get("categoryId");
  const [month, setMonth] = useState(searchParams.get("month") ?? currentMonthKey());
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const locale = language === "ru" ? "ru-RU" : "uz-UZ";

  const monthRange = useMemo(() => {
    const { year, month: m } = parseMonthKey(month);
    const lastDay = new Date(year, m, 0).getDate();
    return { from: `${month}-01`, to: `${month}-${String(lastDay).padStart(2, "0")}` };
  }, [month]);

  const { data, isPending, isError } = useQuery({
    queryKey: ["transactions", categoryFilterId, month],
    queryFn: () =>
      categoryFilterId
        ? listTransactions({ categoryId: categoryFilterId, from: monthRange.from, to: monthRange.to, size: 100 })
        : listTransactions({ size: 100 }),
  });
  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: listCategories });
  const { data: accounts = [] } = useQuery({ queryKey: ["accounts"], queryFn: listAccounts });

  const { data: calendar } = useQuery({
    queryKey: ["analytics", "calendar", month],
    queryFn: () => getAnalyticsCalendar(month),
  });

  const { data: dayDetail, isPending: dayPending } = useQuery({
    queryKey: ["analytics", "day", selectedDate],
    queryFn: () => getAnalyticsDay(selectedDate as string),
    enabled: selectedDate !== null,
  });

  function categoryLabel(categoryId: string | null): string {
    const category = categories.find((c) => c.id === categoryId);
    if (!category) return "";
    return language === "ru" ? category.nameRu : category.nameUz;
  }

  function accountLabel(accountId: string): string {
    return accounts.find((a) => a.id === accountId)?.name ?? "";
  }

  function clearFilter() {
    setSearchParams({});
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="font-heading text-lg font-semibold">{dictionary.history.title}</h1>

      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => setMonth((m) => shiftMonth(m, -1))}
          aria-label="previous month"
          className="px-3 text-lg text-slate-400"
        >
          ‹
        </button>
        <span className="text-sm font-medium">{monthLabel(month, locale)}</span>
        <button
          type="button"
          onClick={() => setMonth((m) => shiftMonth(m, 1))}
          aria-label="next month"
          className="px-3 text-lg text-slate-400"
        >
          ›
        </button>
      </div>

      {calendar ? (
        calendar.days.length === 0 ? (
          <EmptyStateCard>{dictionary.analytics.empty}</EmptyStateCard>
        ) : (
          <CalendarHeatmap
            monthKey={month}
            days={calendar.days}
            selectedDate={selectedDate}
            onSelectDate={(date) => setSelectedDate((current) => (current === date ? null : date))}
            weekdayLabels={dictionary.analytics.calendarWeekdays}
          />
        )
      ) : null}

      {selectedDate ? (
        <div className="rounded-2xl bg-navy-800 p-4">
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-medium">
              {new Date(`${selectedDate}T00:00:00`).toLocaleDateString(locale, { day: "numeric", month: "long" })}
            </span>
            <button type="button" onClick={() => setSelectedDate(null)} className="text-xs text-slate-400">
              {dictionary.history.closeDayDetail}
            </button>
          </div>
          {dayPending ? <SkeletonCard label="" /> : null}
          {dayDetail && dayDetail.transactions.length === 0 ? (
            <p className="text-xs text-slate-400">{dictionary.history.dayDetailEmpty}</p>
          ) : null}
          {dayDetail && dayDetail.transactions.length > 0 ? (
            <div className="flex flex-col gap-2">
              {dayDetail.transactions.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => openEdit(t as unknown as Transaction)}
                  className="flex items-center justify-between text-sm"
                >
                  <span>{categoryLabel(t.categoryId)}</span>
                  <span className={t.type === "EXPENSE" ? "text-warn-400" : "text-success-400"}>
                    {t.type === "EXPENSE" ? "-" : "+"}
                    {formatMoney(t.amount, language)}
                  </span>
                </button>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}

      {categoryFilterId ? (
        <div className="flex items-center justify-between rounded-full bg-navy-800 px-3 py-2 text-xs">
          <span>{categoryLabel(categoryFilterId)}</span>
          <button type="button" onClick={clearFilter} className="font-semibold text-gold-400">
            {dictionary.history.clearFilter}
          </button>
        </div>
      ) : null}

      {isPending ? (
        <div className="flex flex-col gap-3">
          <SkeletonCard label="" />
          <SkeletonCard label="" />
        </div>
      ) : null}

      {isError ? <EmptyStateCard>{dictionary.error.generic}</EmptyStateCard> : null}

      {data && data.items.length === 0 ? <EmptyStateCard>{dictionary.history.empty}</EmptyStateCard> : null}

      {data
        ? groupByDay(data.items).map((group) => {
            const now = new Date();
            const today = dayKey(now.toISOString());
            const yesterdayDate = new Date(now);
            yesterdayDate.setDate(now.getDate() - 1);
            const yesterday = dayKey(yesterdayDate.toISOString());

            function dayLabel(key: string, date: Date): string {
              if (key === today) return dictionary.history.today;
              if (key === yesterday) return dictionary.history.yesterday;
              return date.toLocaleDateString(locale, { day: "numeric", month: "long" });
            }

            return (
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
                          {transaction.type === "TRANSFER"
                            ? dictionary.quickAdd.tabTransfer
                            : categoryLabel(transaction.categoryId)}
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
            );
          })
        : null}
    </div>
  );
}
