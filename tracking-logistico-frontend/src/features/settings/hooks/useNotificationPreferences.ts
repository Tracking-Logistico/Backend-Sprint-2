import { useEffect, useState } from "react";
import { getApiError } from "@/lib/api";
import type { NotificationChannel, NotificationPreference } from "@/types/api";
import {
  getNotificationPreference,
  updateNotificationPreference,
} from "../services/notificationPreferencesService";

function errorMessage(error: unknown, fallback: string) {
  const status =
    typeof error === "object" && error !== null && "status" in error
      ? (error as { status?: number }).status
      : undefined;
  if (status === 400)
    return "Revisa el canal y el número de teléfono ingresados.";
  if (status === 401) return "Tu sesión expiró. Inicia sesión nuevamente.";
  if (status === 403)
    return "No tienes permiso para cambiar estas preferencias.";
  if (status === 404) return "No encontramos las preferencias de notificación.";
  if (status === 409)
    return "Las preferencias cambiaron. Recarga la página e inténtalo de nuevo.";
  return getApiError(error, fallback);
}

export function useNotificationPreferences(
  token: string | null,
  defaultPhone: string,
) {
  const [preference, setPreference] = useState<NotificationPreference | null>(
    null,
  );
  const [channel, setChannel] = useState<NotificationChannel>("EMAIL");
  const [phone, setPhone] = useState(defaultPhone);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      await Promise.resolve();
      if (cancelled) return;
      if (!token) {
        setLoading(false);
        setError("La sesión no contiene la información necesaria.");
        return;
      }
      setLoading(true);
      setError("");
      try {
        const result = await getNotificationPreference(token);
        if (cancelled) return;
        setPreference(result);
        setChannel(result.canal);
        setPhone(result.telefonoSms ?? defaultPhone);
      } catch (err: unknown) {
        if (!cancelled)
          setError(errorMessage(err, "No pudimos cargar tus preferencias."));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [defaultPhone, token]);

  async function save() {
    const normalizedPhone = phone.trim();
    if ((channel === "SMS" || channel === "AMBOS") && !normalizedPhone) {
      setError("Ingresa un teléfono para recibir notificaciones por SMS.");
      return false;
    }
    if (normalizedPhone && !/^\+?[1-9][0-9]{7,14}$/.test(normalizedPhone)) {
      setError(
        "El teléfono debe tener entre 8 y 15 dígitos y puede incluir prefijo +.",
      );
      return false;
    }
    if (!token) {
      setError("La sesión no contiene la información necesaria.");
      return false;
    }
    setSaving(true);
    setError("");
    try {
      const result = await updateNotificationPreference(
        { canal: channel, telefonoSms: normalizedPhone || null },
        token,
      );
      setPreference(result);
      setChannel(result.canal);
      setPhone(result.telefonoSms ?? "");
      return true;
    } catch (err: unknown) {
      setError(errorMessage(err, "No pudimos guardar tus preferencias."));
      return false;
    } finally {
      setSaving(false);
    }
  }

  return {
    preference,
    channel,
    setChannel,
    phone,
    setPhone,
    loading,
    saving,
    error,
    save,
  };
}
