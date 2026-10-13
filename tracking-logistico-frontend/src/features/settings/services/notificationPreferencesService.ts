import { api } from "@/lib/api";
import type {
  NotificationPreference,
  UpdateNotificationPreferencePayload,
} from "@/types/api";

export function getNotificationPreference(
  token: string,
): Promise<NotificationPreference> {
  return api.getNotificationPreference(token);
}

export function updateNotificationPreference(
  payload: UpdateNotificationPreferencePayload,
  token: string,
): Promise<NotificationPreference> {
  return api.updateNotificationPreference(payload, token);
}
