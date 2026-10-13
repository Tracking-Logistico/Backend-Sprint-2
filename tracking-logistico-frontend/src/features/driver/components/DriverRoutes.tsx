import { useCallback, useEffect, useState } from "react";
import { MapPin, Navigation, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { api, getApiError } from "@/lib/api";
import { useAuthStore } from "@/stores/authStore";
import { usePageMeta } from "@/hooks/usePageMeta";
import type { DriverRoute } from "@/types/api";

export function DriverRoutes() {
  usePageMeta("Rutas", "Orden de paradas y siguiente parada recomendada.");
  const token = useAuthStore((state) => state.accessToken);
  const [route, setRoute] = useState<DriverRoute | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      setRoute(await api.driverRoute(token));
    } catch (err) {
      setError(getApiError(err, "No fue posible cargar tu ruta."));
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    const timer = window.setTimeout(() => void refresh(), 0);
    return () => window.clearTimeout(timer);
  }, [refresh]);

  return (
    <div className="mx-auto max-w-4xl px-5 py-8 sm:px-8 lg:px-10">
      <header className="flex flex-wrap items-start justify-between gap-4 border-b border-border pb-7">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[.19em] text-primary">
            Jornada del conductor
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight">Rutas</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Consulta el orden de tus paradas y la siguiente parada recomendada.
          </p>
        </div>
        <Button
          variant="outline"
          disabled={loading}
          onClick={() => void refresh()}
        >
          <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />{" "}
          Actualizar
        </Button>
      </header>

      {error && (
        <div
          role="alert"
          className="mt-5 rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive"
        >
          {error}
        </div>
      )}

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Navigation className="size-5 text-primary" /> Ruta calculada
          </CardTitle>
        </CardHeader>
        <CardContent className="gap-3">
          {loading && (
            <div
              role="status"
              aria-label="Cargando ruta"
              className="grid gap-3"
            >
              {[1, 2, 3].map((item) => (
                <Skeleton key={item} className="h-16 rounded-lg" />
              ))}
            </div>
          )}
          {!loading && route?.siguiente && (
            <div className="rounded-lg border border-primary/30 bg-primary/10 p-3">
              <p className="text-xs font-semibold uppercase text-primary">
                Siguiente parada recomendada
              </p>
              <p className="mt-1 font-medium">
                {route.siguiente.direccion}, {route.siguiente.ciudad}
              </p>
            </div>
          )}
          {!loading &&
            route?.paradas.map((stop) => (
              <div
                key={stop.paradaId}
                className="flex gap-3 rounded-lg border p-3"
              >
                <span className="grid size-7 shrink-0 place-items-center rounded-full bg-sidebar text-xs font-bold text-sidebar-foreground">
                  {stop.orden}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium">
                    {stop.direccion}, {stop.ciudad}
                  </p>
                  <p className="text-xs text-muted-foreground">{stop.estado}</p>
                  {stop.sinUbicacion && (
                    <p className="mt-1 flex items-center gap-1 text-xs font-semibold text-secondary-foreground">
                      <MapPin className="size-3" /> Sin ubicación
                    </p>
                  )}
                </div>
              </div>
            ))}
          {!loading && !route?.paradas.length && (
            <p className="text-sm text-muted-foreground">
              No hay paradas pendientes en la ruta.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
