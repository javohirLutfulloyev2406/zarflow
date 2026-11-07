import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { getAnalyticsMonth } from "../api/analytics";
import type { AnalyticsCategoryBreakdown, AnalyticsMonthResponse, Language } from "../api/types";
import { DISCRETIONARY_COLOR, ESSENTIAL_COLOR, FALLBACK_CATEGORY_COLOR } from "../analytics/colors";
import { currentMonthKey, monthLabel, shiftMonth } from "../analytics/month";
import { EmptyStateCard } from "../components/EmptyStateCard";
import { SkeletonCard } from "../components/SkeletonCard";
import { useDictionary } from "../i18n/dictionary";
import { useLanguage } from "../i18n/useLanguage";
import { formatMoney } from "../money/formatMoney";
import { hapticSelectionChanged } from "../telegram/haptics";

export function Analytics() {
  const dictionary = useDictionary();
  const language = useLanguage();
  const navigate = useNavigate();
  const [month, setMonth] = useState(currentMonthKey());
  const locale = language === "ru" ? "ru-RU" : "uz-UZ";

  const { data, isPending, isError } = useQuery({
    queryKey: ["analytics", "month", month],
    queryFn: () => getAnalyticsMonth(month),
  });

  function goToCategory(categoryId: string) {
    hapticSelectionChanged();
    navigate(`/history?categoryId=${categoryId}&month=${month}`);
  }

  const isEmpty = data && Number(data.income) === 0 && Number(data.expense) === 0;

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="font-heading text-lg font-semibold">{dictionary.analytics.title}</h1>

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

      {isPending ? (
        <div className="flex flex-col gap-3">
          <SkeletonCard label="" />
          <SkeletonCard label="" />
        </div>
      ) : null}

      {isError ? <EmptyStateCard>{dictionary.error.generic}</EmptyStateCard> : null}

      {data && isEmpty ? <EmptyStateCard>{dictionary.analytics.empty}</EmptyStateCard> : null}

      {data && !isEmpty ? (
        <>
          <DonutCard data={data} language={language} dictionary={dictionary} />
          <EssentialDiscretionaryCard data={data} language={language} dictionary={dictionary} />
          <CategoryList data={data} language={language} onSelect={goToCategory} />
        </>
      ) : null}
    </div>
  );
}

function DonutCard({
  data,
  language,
  dictionary,
}: {
  data: AnalyticsMonthResponse;
  language: Language;
  dictionary: ReturnType<typeof useDictionary>;
}) {
  if (data.categories.length === 0) {
    return <EmptyStateCard>{dictionary.analytics.empty}</EmptyStateCard>;
  }

  const chartData = data.categories.map((c) => ({
    name: c.name,
    amount: c.amount,
    value: Number(c.amount),
    color: c.color ?? FALLBACK_CATEGORY_COLOR,
  }));

  return (
    <div className="relative rounded-2xl bg-navy-800 p-4">
      <ResponsiveContainer width="100%" height={200}>
        <PieChart>
          <Pie data={chartData} dataKey="value" nameKey="name" innerRadius={60} outerRadius={90} paddingAngle={2} stroke="none">
            {chartData.map((entry) => (
              <Cell key={entry.name} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            content={({ active, payload }) => {
              const first = payload?.[0];
              if (!active || !first) {
                return null;
              }
              const entry = first.payload as { name: string; amount: string };
              return (
                <div className="rounded-lg bg-navy-900 px-2 py-1 text-xs text-ivory-50">
                  {entry.name}: {formatMoney(entry.amount, language)}
                </div>
              );
            }}
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-xs text-slate-400">{dictionary.analytics.expenseLabel}</span>
        <span className="font-heading text-base font-semibold">{formatMoney(data.expense, language)}</span>
      </div>
    </div>
  );
}

function EssentialDiscretionaryCard({
  data,
  language,
  dictionary,
}: {
  data: AnalyticsMonthResponse;
  language: Language;
  dictionary: ReturnType<typeof useDictionary>;
}) {
  const barData = [
    { name: "", essential: Number(data.essentialExpense), discretionary: Number(data.discretionaryExpense) },
  ];

  return (
    <div className="rounded-2xl bg-navy-800 p-4">
      <div className="mb-2 flex justify-between text-xs text-slate-400">
        <span className="flex items-center gap-1">
          <span className="inline-block h-2 w-2 rounded-full" style={{ backgroundColor: ESSENTIAL_COLOR }} />
          {dictionary.analytics.essentialLabel}: {formatMoney(data.essentialExpense, language)}
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block h-2 w-2 rounded-full" style={{ backgroundColor: DISCRETIONARY_COLOR }} />
          {dictionary.analytics.discretionaryLabel}: {formatMoney(data.discretionaryExpense, language)}
        </span>
      </div>
      <ResponsiveContainer width="100%" height={64}>
        <BarChart data={barData} layout="vertical" barCategoryGap="30%">
          <XAxis type="number" hide />
          <YAxis type="category" dataKey="name" hide />
          <Tooltip formatter={(value) => formatMoney(String(value ?? 0), language)} />
          <Bar dataKey="essential" fill={ESSENTIAL_COLOR} radius={4} />
          <Bar dataKey="discretionary" fill={DISCRETIONARY_COLOR} radius={4} />
        </BarChart>
      </ResponsiveContainer>
      <p className="mt-2 text-xs text-slate-400">
        {dictionary.analytics.dailyAverageLabel}: {formatMoney(data.dailyAverageExpense, language)}
      </p>
    </div>
  );
}

function CategoryList({
  data,
  language,
  onSelect,
}: {
  data: AnalyticsMonthResponse;
  language: Language;
  onSelect: (categoryId: string) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      {data.categories.map((category) => (
        <CategoryRow key={category.categoryId} category={category} language={language} onSelect={onSelect} />
      ))}
    </div>
  );
}

function CategoryRow({
  category,
  language,
  onSelect,
}: {
  category: AnalyticsCategoryBreakdown;
  language: Language;
  onSelect: (categoryId: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onSelect(category.categoryId)}
      className="flex items-center justify-between rounded-2xl bg-navy-800 p-3 text-left"
    >
      <div className="flex items-center gap-2">
        <span
          className="inline-block h-2 w-2 rounded-full"
          style={{ backgroundColor: category.color ?? FALLBACK_CATEGORY_COLOR }}
        />
        <span className="text-sm">{category.name}</span>
      </div>
      <div className="flex items-center gap-3">
        <TrendBadge percent={category.changeVsPrevMonthPercent} />
        <div className="flex flex-col items-end">
          <span className="text-sm">{formatMoney(category.amount, language)}</span>
          <span className="text-xs text-slate-400">{category.sharePercent}%</span>
        </div>
      </div>
    </button>
  );
}

function TrendBadge({ percent }: { percent: string | null }) {
  if (percent === null) {
    return null;
  }
  const numeric = Number(percent);
  if (numeric === 0) {
    return null;
  }
  const isIncrease = numeric > 0;
  return (
    <span className={`text-xs ${isIncrease ? "text-warn-400" : "text-success-400"}`}>
      {isIncrease ? "↑" : "↓"} {Math.abs(numeric)}%
    </span>
  );
}
