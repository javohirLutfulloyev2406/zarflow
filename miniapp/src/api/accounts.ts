import { apiRequest } from "./client";
import type { Account, CreateAccountRequest, UpdateAccountRequest } from "./types";

export function listAccounts(): Promise<Account[]> {
  return apiRequest<Account[]>("/accounts");
}

export function createAccount(body: CreateAccountRequest): Promise<Account> {
  return apiRequest<Account>("/accounts", { method: "POST", body: JSON.stringify(body) });
}

export function updateAccount(id: string, body: UpdateAccountRequest): Promise<Account> {
  return apiRequest<Account>(`/accounts/${id}`, { method: "PUT", body: JSON.stringify(body) });
}

export function archiveAccount(id: string): Promise<Account> {
  return apiRequest<Account>(`/accounts/${id}/archive`, { method: "POST" });
}
