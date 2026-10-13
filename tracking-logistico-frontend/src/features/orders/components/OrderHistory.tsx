import { useState } from "react";
import { History } from "lucide-react";
import { api, getApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Alert } from "@/components/ui/alert";
import type { OrderEvent } from "@/types/api";

export function OrderHistory({
  id,
  token,
}: {
  id: number;
  token: string | null;
}) {
  const [open, setOpen] = useState(false);
  const [events, setEvents] = useState<OrderEvent[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  async function toggle() {
    if (open) {
      setOpen(false);
      return;
    }
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      setEvents(await api.orderHistory(id, token));
      setOpen(true);
    } catch (err) {
      setError(getApiError(err));
    } finally {
      setLoading(false);
    }
  }
  return (
    <div className="mt-4">
      <Button size="sm" variant="ghost" onClick={() => void toggle()}>
        <History className="size-4" />
        {loading ? "Cargando..." : open ? "Ocultar historial" : "Ver historial"}
      </Button>
      {error && (
        <Alert variant="destructive" className="mt-3">
          {error}
        </Alert>
      )}
      {open && (
        <div className="mt-3 space-y-2 border-l-2 border-primary/30 pl-4 text-sm">
          {events.length === 0 ? (
            <p className="text-muted-foreground">Sin eventos registrados.</p>
          ) : (
            events.map((event) => (
              <div key={event.id}>
                <p className="font-medium">
                  {event.tipoEvento.replaceAll("_", " ")}
                </p>
                <p className="text-xs text-muted-foreground">
                  {event.detalle || "Sin detalle"} ·{" "}
                  {new Date(event.fecha).toLocaleString()}
                </p>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
