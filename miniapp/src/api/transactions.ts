import { apiRequest } from "./client";
import type {
  AdjustRequest,
  CreateTransactionRequest,
  Transaction,
  TransactionsPage,
  TransactionsSearchParams,
  TransferRequest,
  TransferResponse,
  UpdateTransactionRequest,
} from "./types";

export function createTransaction(body: CreateTransactionRequest, idempotencyKey: string): Promise<Transaction> {
  return apiRequest<Transaction>("/transactions", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },
    body: JSON.stringify(body),
  });
}

export function updateTransaction(id: string, body: UpdateTransactionRequest): Promise<Transaction> {
  return apiRequest<Transaction>(`/transactions/${id}`, { method: "PUT", body: JSON.stringify(body) });
}

export function deleteTransaction(id: string): Promise<void> {
  return apiRequest<void>(`/transactions/${id}`, { method: "DELETE" });
}

export function listTransactions(params: TransactionsSearchParams = {}): Promise<TransactionsPage> {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, String(value));
    }
  }
  const qs = query.toString();
  return apiRequest<TransactionsPage>(`/transactions${qs ? `?${qs}` : ""}`);
}

export function transfer(body: TransferRequest): Promise<TransferResponse> {
  return apiRequest<TransferResponse>("/accounts/transfer", { method: "POST", body: JSON.stringify(body) });
}

export function adjustAccount(accountId: string, body: AdjustRequest): Promise<Transaction> {
  return apiRequest<Transaction>(`/accounts/${accountId}/adjust`, { method: "POST", body: JSON.stringify(body) });
}
