import { apiRequest } from "./client";
import type {
  CreateRecurringRuleRequest,
  RecurringOccurrenceResult,
  RecurringRule,
  UpcomingOccurrence,
  UpdateRecurringRuleRequest,
} from "./types";

export function listRecurringRules(): Promise<RecurringRule[]> {
  return apiRequest<RecurringRule[]>("/recurring");
}

export function createRecurringRule(body: CreateRecurringRuleRequest): Promise<RecurringRule> {
  return apiRequest<RecurringRule>("/recurring", { method: "POST", body: JSON.stringify(body) });
}

export function updateRecurringRule(id: string, body: UpdateRecurringRuleRequest): Promise<RecurringRule> {
  return apiRequest<RecurringRule>(`/recurring/${id}`, { method: "PUT", body: JSON.stringify(body) });
}

export function archiveRecurringRule(id: string): Promise<RecurringRule> {
  return apiRequest<RecurringRule>(`/recurring/${id}`, { method: "DELETE" });
}

export function listUpcomingOccurrences(days = 30): Promise<UpcomingOccurrence[]> {
  return apiRequest<UpcomingOccurrence[]>(`/recurring/upcoming?days=${days}`);
}

export function payOccurrence(occurrenceId: string): Promise<RecurringOccurrenceResult> {
  return apiRequest<RecurringOccurrenceResult>(`/recurring/occurrences/${occurrenceId}/pay`, { method: "POST" });
}

export function skipOccurrence(occurrenceId: string): Promise<RecurringOccurrenceResult> {
  return apiRequest<RecurringOccurrenceResult>(`/recurring/occurrences/${occurrenceId}/skip`, { method: "POST" });
}
