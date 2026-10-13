import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/authStore";
import type { DeliveryEvent, OfflineDeliveryEvent } from "@/types/api";
import {
  acceptedSyncResults,
  enqueueDelivery,
  getOfflineQueue,
  getOfflineQueueAgeMs,
  isNetworkError,
  OFFLINE_EVENT_MAX_AGE_MS,
  OFFLINE_QUEUE_LIMIT,
  removeOfflineEvents,
} from "@/features/driver/services/offlineDeliveryQueue";

export function useOfflineDeliveryQueue() {
  const token = useAuthStore((state) => state.accessToken);
  const [isOnline, setIsOnline] = useState(() => navigator.onLine);
  const [queue, setQueue] = useState<OfflineDeliveryEvent[]>(getOfflineQueue);

  const refreshQueue = useCallback(() => setQueue(getOfflineQueue()), []);

  const synchronize = useCallback(async () => {
    if (!token || !navigator.onLine) return;
    const pending = getOfflineQueue();
    if (!pending.length) return;
    try {
      const response = await api.syncDriverDeliveries(pending, token);
      const accepted = acceptedSyncResults(response.resultados);
      removeOfflineEvents(accepted);
      response.resultados
        .filter((result) => result.estado === "RECHAZADO")
        .forEach((result) =>
          toast.error(
            `Evento no sincronizado: ${result.motivoRechazo ?? "el servidor lo rechazó."}`,
          ),
        );
      refreshQueue();
      window.dispatchEvent(new CustomEvent("logistrack-driver-refresh"));
    } catch (error) {
      if (!isNetworkError(error))
        toast.error("No fue posible sincronizar las entregas pendientes.");
    }
  }, [refreshQueue, token]);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      void Promise.resolve().then(synchronize);
    };
    const handleOffline = () => setIsOnline(false);
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    window.addEventListener("logistrack-queue-change", refreshQueue);
    void Promise.resolve().then(synchronize);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
      window.removeEventListener("logistrack-queue-change", refreshQueue);
    };
  }, [refreshQueue, synchronize]);

  const queueEvent = useCallback(
    (pedidoId: number, evento: DeliveryEvent) => {
      const item = enqueueDelivery(pedidoId, evento);
      refreshQueue();
      const count = getOfflineQueue().length;
      if (count === 80)
        toast.warning("Tienes 80 eventos pendientes sin conexión.");
      return item;
    },
    [refreshQueue],
  );

  const ageWarning =
    queue.length > 0 &&
    getOfflineQueueAgeMs() >= OFFLINE_EVENT_MAX_AGE_MS - 4 * 60 * 60 * 1000;
  const limitReached = queue.length >= OFFLINE_QUEUE_LIMIT;
  useEffect(() => {
    if (ageWarning)
      toast.warning(
        "Hay un evento pendiente próximo a cumplir 24 horas sin sincronizar.",
      );
  }, [ageWarning]);

  return {
    isOnline,
    pendingEvents: queue.length,
    queue,
    queueEvent,
    synchronize,
    limitReached,
    ageWarning,
  };
}
