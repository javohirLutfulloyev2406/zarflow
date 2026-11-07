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
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
