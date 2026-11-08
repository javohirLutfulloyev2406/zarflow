import { apiRequest } from "./client";
import type { Insight } from "./types";

export function listInsights(): Promise<Insight[]> {
  return apiRequest<Insight[]>("/insights");
}

export function dismissInsight(id: string): Promise<void> {
  return apiRequest<void>(`/insights/${id}/dismiss`, { method: "POST" });
}
