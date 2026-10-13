import { useEffect, useState } from "react";
import { api, getApiError } from "@/lib/api";
import { useAuthStore } from "@/stores/authStore";
import type { DriverDeliveryDetail } from "@/types/api";

export function useDriverDeliveryDetail(pedidoId: number | null) {
  const token = useAuthStore((state) => state.accessToken);
  const [detail, setDetail] = useState<DriverDeliveryDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!pedidoId || !token) return;

    let cancelled = false;
    void Promise.resolve().then(() => {
      if (cancelled) return;
      setLoading(true);
      setDetail(null);
      setError("");
    });
    void api
      .driverDeliveryDetail(pedidoId, token)
      .then((response) => {
        if (!cancelled) setDetail(response);
      })
      .catch((reason: unknown) => {
        if (!cancelled)
          setError(
            getApiError(
              reason,
              "No fue posible cargar la información del envío.",
            ),
          );
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [pedidoId, token]);

  return { detail, loading, error };
}
