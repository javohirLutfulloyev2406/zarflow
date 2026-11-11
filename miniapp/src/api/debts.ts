import { apiRequest } from "./client";
import type {
  CreateDebtRequest,
  Debt,
  DebtPayment,
  DebtPaymentResult,
  DebtProgress,
  DebtScheduleRow,
  DebtSimulateResult,
  DebtStrategyComparison,
  ManualScheduleRowRequest,
  PayDebtRequest,
  UpdateDebtRequest,
} from "./types";

export function listDebts(): Promise<Debt[]> {
  return apiRequest<Debt[]>("/debts");
}

export function getDebt(id: string): Promise<Debt> {
  return apiRequest<Debt>(`/debts/${id}`);
}

export function createDebt(body: CreateDebtRequest): Promise<Debt> {
  return apiRequest<Debt>("/debts", { method: "POST", body: JSON.stringify(body) });
}

export function updateDebt(id: string, body: UpdateDebtRequest): Promise<Debt> {
  return apiRequest<Debt>(`/debts/${id}`, { method: "PUT", body: JSON.stringify(body) });
}

export function setManualSchedule(id: string, rows: ManualScheduleRowRequest[]): Promise<Debt> {
  return apiRequest<Debt>(`/debts/${id}/manual-schedule`, { method: "POST", body: JSON.stringify({ rows }) });
}

export function getDebtSchedule(id: string): Promise<DebtScheduleRow[]> {
  return apiRequest<DebtScheduleRow[]>(`/debts/${id}/schedule`);
}

export function getDebtPayments(id: string): Promise<DebtPayment[]> {
  return apiRequest<DebtPayment[]>(`/debts/${id}/payments`);
}

export function payDebt(id: string, body: PayDebtRequest): Promise<DebtPaymentResult> {
  return apiRequest<DebtPaymentResult>(`/debts/${id}/payments`, { method: "POST", body: JSON.stringify(body) });
}

export function getDebtProgress(id: string): Promise<DebtProgress> {
  return apiRequest<DebtProgress>(`/debts/${id}/progress`);
}

export function simulateDebt(id: string, extraMonthly: string): Promise<DebtSimulateResult> {
  return apiRequest<DebtSimulateResult>(`/debts/${id}/simulate`, { method: "POST", body: JSON.stringify({ extraMonthly }) });
}

export function getDebtStrategy(extraMonthly: string): Promise<DebtStrategyComparison> {
  return apiRequest<DebtStrategyComparison>("/debts/strategy", { method: "POST", body: JSON.stringify({ extraMonthly }) });
}
