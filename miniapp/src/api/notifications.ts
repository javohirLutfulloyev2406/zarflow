import { apiRequest } from "./client";
import type { NotificationSettings, UpdateNotificationSettingsRequest } from "./types";

export function getNotificationSettings(): Promise<NotificationSettings> {
  return apiRequest<NotificationSettings>("/notifications/settings");
}

export function updateNotificationSettings(body: UpdateNotificationSettingsRequest): Promise<NotificationSettings> {
  return apiRequest<NotificationSettings>("/notifications/settings", {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
