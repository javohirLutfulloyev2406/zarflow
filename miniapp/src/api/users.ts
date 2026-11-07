import { apiRequest } from "./client";
import type { UpdateDisplayNameRequest, UpdateSettingsRequest, UserProfile } from "./types";

export function getMe(): Promise<UserProfile> {
  return apiRequest<UserProfile>("/users/me");
}

export function updateSettings(body: UpdateSettingsRequest): Promise<UserProfile> {
  return apiRequest<UserProfile>("/users/me/settings", {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function updateDisplayName(body: UpdateDisplayNameRequest): Promise<UserProfile> {
  return apiRequest<UserProfile>("/users/me/display-name", {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
