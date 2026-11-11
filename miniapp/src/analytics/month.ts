/** "yyyy-MM" month keys, matching AnalyticsController's ?month= query param. */

export function parseMonthKey(monthKey: string): { year: number; month: number } {
  const [yearPart, monthPart] = monthKey.split("-");
  return { year: Number(yearPart), month: Number(monthPart) };
}

export function currentMonthKey(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

export function shiftMonth(monthKey: string, delta: number): string {
  const { year, month } = parseMonthKey(monthKey);
  const date = new Date(year, month - 1 + delta, 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

export function monthLabel(monthKey: string, locale: string): string {
  const { year, month } = parseMonthKey(monthKey);
  const date = new Date(year, month - 1, 1);
  const label = date.toLocaleDateString(locale, { month: "long", year: "numeric" });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export function todayKey(): string {
  return dateKeyOf(new Date());
}

/** The device-local "yyyy-MM-dd" calendar date of an ISO instant (or a Date), matching todayKey(). */
export function dateKeyOf(instant: string | Date): string {
  const date = typeof instant === "string" ? new Date(instant) : instant;
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

/** Combines a "yyyy-MM-dd" date key with the current time-of-day into an ISO instant --
 * backdating a transaction should only move its day, not its time. */
export function instantAtDateKey(dateKey: string): string {
  const now = new Date();
  const parts = dateKey.split("-");
  const year = Number(parts[0]);
  const month = Number(parts[1]);
  const day = Number(parts[2]);
  return new Date(
    year,
    month - 1,
    day,
    now.getHours(),
    now.getMinutes(),
    now.getSeconds(),
    now.getMilliseconds(),
  ).toISOString();
}
