import { useMemo } from "react";

import type { AnalyticsCalendarDay } from "../api/types";
import { HEATMAP_HUE } from "./colors";
import { parseMonthKey, todayKey } from "./month";

interface CalendarHeatmapProps {
  monthKey: string;
  days: AnalyticsCalendarDay[];
  selectedDate: string | null;
  onSelectDate: (date: string) => void;
  weekdayLabels: string[];
}

/** One hue (warn-400), light-to-dark via opacity -- sequential, not categorical. */
function intensityFor(expense: number, max: number): number {
  if (max <= 0 || expense <= 0) {
    return 0;
  }
  const ratio = expense / max;
  if (ratio > 0.75) return 1;
  if (ratio > 0.5) return 0.75;
  if (ratio > 0.25) return 0.5;
  return 0.25;
}

export function CalendarHeatmap({ monthKey, days, selectedDate, onSelectDate, weekdayLabels }: CalendarHeatmapProps) {
  const { year, month } = parseMonthKey(monthKey);
  const today = todayKey();

  const byDate = useMemo(() => {
    const map = new Map<string, AnalyticsCalendarDay>();
    for (const d of days) {
      map.set(d.date, d);
    }
    return map;
  }, [days]);

  const maxExpense = useMemo(() => days.reduce((max, d) => Math.max(max, Number(d.expense)), 0), [days]);

  const daysInMonth = new Date(year, month, 0).getDate();
  const firstWeekday = (new Date(year, month - 1, 1).getDay() + 6) % 7; // Monday-start, like the backend's week.

  const cells: Array<{ date: string | null; day: number | null }> = [];
  for (let i = 0; i < firstWeekday; i++) {
    cells.push({ date: null, day: null });
  }
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push({ date: `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`, day });
  }

  return (
    <div className="rounded-2xl bg-navy-800 p-3">
      <div className="mb-1 grid grid-cols-7 gap-1 text-center text-[10px] text-slate-400">
        {weekdayLabels.map((label, index) => (
          <span key={`${label}-${index}`}>{label}</span>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {cells.map((cell, index) => {
          if (!cell.date) {
            return <div key={`blank-${index}`} />;
          }
          const data = byDate.get(cell.date);
          const expense = data ? Number(data.expense) : 0;
          const intensity = intensityFor(expense, maxExpense);
          const isToday = cell.date === today;
          const isSelected = cell.date === selectedDate;
          return (
            <button
              key={cell.date}
              type="button"
              onClick={() => onSelectDate(cell.date as string)}
              className={`flex aspect-square items-center justify-center rounded-lg text-xs ${
                isToday ? "ring-2 ring-gold-400" : ""
              } ${isSelected ? "ring-2 ring-ivory-50" : ""}`}
              style={{ backgroundColor: intensity === 0 ? "transparent" : `rgba(${HEATMAP_HUE}, ${intensity})` }}
            >
              <span className={intensity > 0.5 ? "text-navy-900" : "text-ivory-50"}>{cell.day}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
