import { useCallback, useEffect, useState } from "react";
import type {
  IncidentType,
  IncidentsResponse,
  RegisterIncidentPayload,
  RegisterIncidentResponse,
} from "@/types/api";
import {
  createIncident,
  getIncidentTypes,
  getOrderIncidents,
} from "../services/incidentsService";

export function useIncidentTypes(token: string | null) {
  const [types, setTypes] = useState<IncidentType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      setTypes(await getIncidentTypes(token));
      setError(null);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  return { types, loading, error, reload: load };
}

export function useIncidents(orderId: number, token: string | null) {
  const [data, setData] = useState<IncidentsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      setData(await getOrderIncidents(orderId, token));
      setError(null);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [orderId, token]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const register = useCallback(
    async (
      payload: RegisterIncidentPayload,
    ): Promise<RegisterIncidentResponse> => {
      if (!token) throw new Error("La sesión ha expirado.");
      const result = await createIncident(
        orderId,
        {
          ...payload,
          versionEsperada: data?.versionPedido,
        },
        token,
      );
      await load();
      return result;
    },
    [data?.versionPedido, load, orderId, token],
  );

  return { data, loading, error, reload: load, register };
}
