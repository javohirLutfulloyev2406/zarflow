import { apiRequest } from "./client";
import type { HabitSummary } from "./types";

export function getHabitSummary(): Promise<HabitSummary> {
  return apiRequest<HabitSummary>("/habits");
}
