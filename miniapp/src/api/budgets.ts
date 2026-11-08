import { apiRequest } from "./client";
import type { BudgetResponse, TodayBudgetResponse, UpdateBudgetRequest } from "./types";

export function getBudget(month: string): Promise<BudgetResponse> {
  return apiRequest<BudgetResponse>(`/budgets/${month}`);
}

export function updateBudget(month: string, body: UpdateBudgetRequest): Promise<BudgetResponse> {
  return apiRequest<BudgetResponse>(`/budgets/${month}`, { method: "PUT", body: JSON.stringify(body) });
}

export function getTodayBudget(): Promise<TodayBudgetResponse> {
  return apiRequest<TodayBudgetResponse>("/budgets/today");
}
