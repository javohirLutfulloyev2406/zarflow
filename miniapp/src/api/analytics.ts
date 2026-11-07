import { apiRequest } from "./client";
import type { AnalyticsCalendarResponse, AnalyticsDayResponse, AnalyticsMonthResponse } from "./types";

export function getAnalyticsMonth(month: string): Promise<AnalyticsMonthResponse> {
  return apiRequest<AnalyticsMonthResponse>(`/analytics/month?month=${encodeURIComponent(month)}`);
}

export function getAnalyticsCalendar(month: string): Promise<AnalyticsCalendarResponse> {
  return apiRequest<AnalyticsCalendarResponse>(`/analytics/calendar?month=${encodeURIComponent(month)}`);
}

export function getAnalyticsDay(date: string): Promise<AnalyticsDayResponse> {
  return apiRequest<AnalyticsDayResponse>(`/analytics/day?date=${encodeURIComponent(date)}`);
}
