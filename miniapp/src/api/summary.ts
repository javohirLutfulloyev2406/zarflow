import { apiRequest } from "./client";
import type { TodayAndMonthSummary } from "./types";

export function getTodayAndMonth(): Promise<TodayAndMonthSummary> {
  return apiRequest<TodayAndMonthSummary>("/summary/today-and-month");
}
