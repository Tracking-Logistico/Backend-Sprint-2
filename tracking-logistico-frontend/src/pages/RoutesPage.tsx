import { useCallback, useEffect, useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  Bell,
  ClipboardList,
  MapPinned,
  RefreshCw,
  Route as RouteIcon,
  Send,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { api, getApiError } from "@/lib/api";
import { useAuthStore } from "@/stores/authStore";
import { usePageMeta } from "@/hooks/usePageMeta";
import type {
  AssignmentEvent,
  DriverResponse,
  OrderResponse,
  RouteNotification,
  RouteResponse,
} from "@/types/api";

function formatDate(date: string) {
  return new Date(date).toLocaleString("es-CO", {
    dateStyle: "short",
    timeStyle: "short",
  });
}
function ratio(value: number, limit: number) {
  return Math.min(100, Math.round((value / (limit || 1)) * 100));
}

export function RoutesPage() {
  usePageMeta("Rutas", "Organización y asignación de entregas");
  const token = useAuthStore((s) => s.accessToken);
  const role = useAuthStore((s) => s.role);
  const [pending, setPending] = useState<OrderResponse[]>([]);
  const [drivers, setDrivers] = useState<DriverResponse[]>([]);
  const [selectedDriver, setSelectedDriver] = useState<number | "">("");
  const [selectedOrders, setSelectedOrders] = useState<number[]>([]);
  const [route, setRoute] = useState<RouteResponse | null>(null);
  const [notifications, setNotifications] = useState<RouteNotification[]>([]);
  const [history, setHistory] = useState<AssignmentEvent[]>([]);
  const [historyOrder, setHistoryOrder] = useState<number | null>(null);
  const [reassignId, setReassignId] = useState<number | null>(null);
  const [reassignDriver, setReassignDriver] = useState<number | "">("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const refresh = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      if (role === "OPERADOR") {
        const [orders, activeDrivers] = await Promise.all([
          api.listPendingRouteOrders(token),
          api.listDrivers(token),
        ]);
        setPending(orders.content);
        setDrivers(activeDrivers);
        setSelectedOrders((old) =>
          old.filter((id) =>
            orders.content.some((order) => order.id === id),
          ),
        );
        if (selectedDriver) {
          try {
            setRoute(await api.getDriverRoute(Number(selectedDriver), token));
          } catch (e) {
            setRoute(null);
            setError(getApiError(e));
          }
        }
      } else if (role === "CONDUCTOR") {
        const alerts = await api.routeNotifications(token);
        setNotifications(alerts);
        try {
          setRoute(await api.myDriverRoute(token));
        } catch (e) {
          if (
            typeof e === "object" &&
            e !== null &&
            "status" in e &&
            e.status === 404
          )
            setRoute(null);
          else throw e;
        }
      }
    } catch (e) {
      setError(getApiError(e));
    } finally {
      setLoading(false);
    }
  }, [token, role, selectedDriver]);

  useEffect(() => {
    void Promise.resolve().then(refresh);
  }, [refresh]);

  const currentDriver = drivers.find((d) => d.usuarioId === selectedDriver);
  const selectedWeight = pending
    .filter((p) => selectedOrders.includes(p.id))
    .reduce((total, p) => total + p.pesoKg, 0);
  const selectedVolume = pending
    .filter((p) => selectedOrders.includes(p.id))
    .reduce((total, p) => total + p.largoCm * p.anchoCm * p.altoCm, 0);
  const beyondCapacity = Boolean(
    currentDriver &&
    (currentDriver.entregasAsignadas + selectedOrders.length >
      currentDriver.maxEntregasDia ||
      currentDriver.pesoAsignadoKg + selectedWeight >
        currentDriver.capacidadMaxKg ||
      currentDriver.volumenAsignadoCm3 + selectedVolume >
        currentDriver.capacidadMaxVolumenCm3),
  );

  async function execute(operation: () => Promise<void>) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await operation();
    } catch (e) {
      setError(getApiError(e));
    } finally {
      setBusy(false);
    }
  }

  async function changeDriver(id: number | "") {
    setSelectedDriver(id);
    setRoute(null);
    setReassignId(null);
    setHistoryOrder(null);
    if (!token || !id) return;
    try {
      setRoute(await api.getDriverRoute(id, token));
    } catch (e) {
      if (
        !(
          typeof e === "object" &&
          e !== null &&
          "status" in e &&
          e.status === 404
        )
      )
        setError(getApiError(e));
    }
  }

  async function assign() {
    if (
      !token ||
      !selectedDriver ||
      !selectedOrders.length ||
      busy ||
      beyondCapacity
    )
      return;
    await execute(async () => {
      await api.assignRouteOrders(
        selectedOrders,
        Number(selectedDriver),
        token,
      );
      setSelectedOrders([]);
      setMessage(
        "Asignación registrada; el conductor recibió un aviso en su panel.",
      );
      await refresh();
    });
  }

  async function reorder(stopId: number, direction: -1 | 1) {
    if (!token || !route || busy) return;
    const i = route.paradas.findIndex((p) => p.id === stopId);
    const j = i + direction;
    if (i < 0 || j < 0 || j >= route.paradas.length) return;
    const ids = route.paradas.map((p) => p.pedidoId);
    [ids[i], ids[j]] = [ids[j], ids[i]];
    await execute(async () => {
      setRoute(await api.reorderRoute(route.id, ids, token));
      setMessage("Orden personalizado guardado.");
    });
  }

  async function reassign(pedidoId: number) {
    if (!token || !reassignDriver || busy) return;
    await execute(async () => {
      await api.reassignRouteOrder(
        pedidoId,
        Number(reassignDriver),
        reason.trim(),
        token,
      );
      setReassignId(null);
      setReassignDriver("");
      setReason("");
      setMessage(
        "Envío reasignado y notificado al nuevo conductor. Se conservó la trazabilidad.",
      );
      await refresh();
    });
  }

  async function showHistory(pedidoId: number) {
    if (!token) return;
    setHistoryOrder(pedidoId);
    setHistory([]);
    try {
      setHistory(await api.routeAssignmentHistory(pedidoId, token));
    } catch (e) {
      setError(getApiError(e));
    }
  }

  async function readNotification(id: number) {
    if (!token) return;
    await execute(async () => {
      await api.readRouteNotification(id, token);
      setNotifications((old) =>
        old.map((n) => (n.id === id ? { ...n, leida: true } : n)),
      );
    });
  }

  return (
    <div className="mx-auto max-w-7xl px-5 py-9 sm:px-8 lg:px-10">
      <header className="flex flex-wrap items-start justify-between gap-4 border-b border-border pb-7">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[.19em] text-primary">
            Operación logística
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight">
            {role === "CONDUCTOR"
              ? "Mis entregas de hoy"
              : "Planificación de rutas"}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {role === "CONDUCTOR"
              ? "Consulta tus entregas, su orden y las novedades de asignación."
              : "Selecciona envíos, comprueba la capacidad del conductor y organiza la jornada."}
          </p>
        </div>
        <Button
          variant="outline"
          disabled={loading || busy}
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
      {message && (
        <div
          role="status"
          className="mt-5 rounded-xl border border-primary/30 bg-primary/10 p-4 text-sm text-primary"
        >
          {message}
        </div>
      )}

      {role === "CONDUCTOR" && (
        <section className="mt-7 rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-2">
            <Bell className="size-5 text-primary" />
            <h2 className="font-semibold">Novedades de mi ruta</h2>
            <span className="ml-auto rounded-full bg-primary/10 px-2 py-0.5 text-xs text-primary">
              {notifications.filter((n) => !n.leida).length} sin leer
            </span>
          </div>
          {!notifications.length ? (
            <p className="text-sm text-muted-foreground">
              No tienes notificaciones recientes.
            </p>
          ) : (
            <div className="grid gap-2 sm:grid-cols-2">
              {notifications.map((n) => (
                <div
                  key={n.id}
                  className={`rounded-xl border p-3 text-sm ${n.leida ? "border-border bg-muted" : "border-primary/30 bg-primary/10"}`}
                >
                  <p className="font-medium">{n.mensaje}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Pedido #{n.pedidoId} · {formatDate(n.fecha)}
                  </p>
                  {!n.leida && (
                    <Button
                      size="sm"
                      variant="outline"
                      className="mt-2"
                      disabled={busy}
                      onClick={() => void readNotification(n.id)}
                    >
                      Marcar como leída
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      <div
        className={`mt-7 grid gap-7 ${role === "OPERADOR" ? "lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]" : ""}`}
      >
        {role === "OPERADOR" && (
          <section className="min-w-0 rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ClipboardList className="size-5 text-primary" />
                <h2 className="font-semibold">Pendientes de asignación</h2>
              </div>
              <span className="rounded-full bg-muted px-3 py-1 text-xs font-semibold">
                {pending.length}
              </span>
            </div>
            <p className="mt-2 text-sm text-muted-foreground">
              Selecciona uno o varios envíos. La operación será completa o no se
              realizará.
            </p>
            <div className="mt-5 max-h-[630px] space-y-2 overflow-y-auto pr-1">
              {loading && (
                <div role="status" aria-label="Cargando envíos por asignar" className="space-y-2">
                  <Skeleton className="h-20 w-full rounded-xl" />
                  <Skeleton className="h-20 w-full rounded-xl" />
                </div>
              )}
              {!loading && !pending.length && (
                <p className="rounded-xl border border-dashed p-5 text-sm text-muted-foreground">
                  No hay envíos por asignar.
                </p>
              )}
              {pending.map((p) => (
                <label
                  key={p.id}
                  className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 transition ${selectedOrders.includes(p.id) ? "border-primary bg-primary/10" : "border-border hover:border-primary/40"}`}
                >
                  <input
                    type="checkbox"
                    aria-label={`Seleccionar envío ${p.numeroTracking || p.numeroPedido}`}
                    className="mt-1 accent-primary"
                    checked={selectedOrders.includes(p.id)}
                    onChange={(e) =>
                      setSelectedOrders((ids) =>
                        e.target.checked
                          ? [...ids, p.id]
                          : ids.filter((id) => id !== p.id),
                      )
                    }
                  />
                  <span className="min-w-0 flex-1">
                    <span className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-sm font-semibold">
                        {p.numeroTracking || p.numeroPedido}
                      </span>
                      <span className="rounded-full bg-muted px-2 py-0.5 text-xs">
                        {p.prioridadConfirmada || p.prioridadSugerida}
                      </span>
                    </span>
                    <span className="mt-1 block text-sm text-foreground">
                      {p.direccionDestino} · {p.ciudadDestino}
                    </span>
                    <span className="mt-1 block text-xs text-muted-foreground">
                      {p.destinatarioNombre || "Destinatario no indicado"} ·{" "}
                      {p.pesoKg} kg · {p.estado.replaceAll("_", " ")}
                    </span>
                  </span>
                </label>
              ))}
            </div>
            <div className="mt-5 border-t border-border pt-5">
              <label htmlFor="conductor-ruta" className="text-sm font-medium">
                Conductor disponible
              </label>
              <select
                id="conductor-ruta"
                value={selectedDriver}
                onChange={(e) =>
                  void changeDriver(
                    e.target.value ? Number(e.target.value) : "",
                  )
                }
                className="mt-2 h-11 w-full rounded-xl border border-input bg-background px-3 text-sm"
              >
                <option value="">Seleccionar conductor</option>
                {drivers.map((d) => (
                  <option key={d.usuarioId} value={d.usuarioId}>
                    {d.nombre} · {d.entregasAsignadas}/{d.maxEntregasDia}{" "}
                    entregas
                  </option>
                ))}
              </select>
              {currentDriver && (
                <div className="mt-4 space-y-3 rounded-xl bg-muted p-4 text-xs">
                  <p className="font-semibold text-foreground">
                    Capacidad de la jornada (incluye lo seleccionado)
                  </p>
                  {(
                    [
                      [
                        "Entregas",
                        currentDriver.entregasAsignadas + selectedOrders.length,
                        currentDriver.maxEntregasDia,
                        "",
                      ],
                      [
                        "Peso",
                        currentDriver.pesoAsignadoKg + selectedWeight,
                        currentDriver.capacidadMaxKg,
                        "kg",
                      ],
                      [
                        "Volumen",
                        currentDriver.volumenAsignadoCm3 + selectedVolume,
                        currentDriver.capacidadMaxVolumenCm3,
                        "cm³",
                      ],
                    ] as Array<[string, number, number, string]>
                  ).map(([name, amount, limit, unit]) => (
                    <div key={name}>
                      <div className="flex justify-between gap-2">
                        <span>{name}</span>
                        <span
                          className={
                            amount > limit
                              ? "font-semibold text-destructive"
                              : "text-muted-foreground"
                          }
                        >
                          {Math.round(amount * 100) / 100} / {limit} {unit}
                        </span>
                      </div>
                      <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-muted-foreground/20">
                        <div
                          className={`h-full rounded-full ${amount > limit ? "bg-destructive" : "bg-primary"}`}
                          style={{ width: `${ratio(amount, limit)}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
              {beyondCapacity && (
                <p role="alert" className="mt-3 text-sm text-destructive">
                  La selección supera la capacidad disponible. El servidor
                  también validará el límite.
                </p>
              )}
              <Button
                className="mt-4 w-full"
                disabled={
                  !selectedDriver ||
                  selectedOrders.length === 0 ||
                  busy ||
                  beyondCapacity
                }
                onClick={() => void assign()}
              >
                <Send className="size-4" />
                Asignar {selectedOrders.length || ""}{" "}
                {selectedOrders.length === 1 ? "envío" : "envíos"}
              </Button>
            </div>
          </section>
        )}

        <section className="min-w-0 rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div className="flex items-center gap-2">
            <MapPinned className="size-5 text-primary" />
            <h2 className="font-semibold">
              {role === "CONDUCTOR" ? "Ruta asignada" : "Ruta del conductor"}
            </h2>
          </div>
          {route ? (
            <>
              <div className="mt-4 rounded-xl bg-sidebar px-4 py-3 text-sm text-sidebar-foreground">
                <div className="flex justify-between">
                  <span>
                    {new Date(route.fecha + "T12:00:00").toLocaleDateString(
                      "es-CO",
                      { dateStyle: "long" },
                    )}
                  </span>
                  <span>{route.paradas.length} entregas</span>
                </div>
              </div>
              <ol className="mt-5 space-y-3">
                {route.paradas.map((stop, i) => (
                  <li
                    key={stop.id}
                    className="rounded-xl border border-border p-4"
                  >
                    <div className="flex items-start gap-3">
                      <span className="grid size-9 shrink-0 place-items-center rounded-full bg-primary/10 text-sm font-bold text-primary">
                        {stop.orden}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <strong className="font-mono text-sm">
                            {stop.numeroTracking ||
                              stop.numeroPedido ||
                              `Pedido #${stop.pedidoId}`}
                          </strong>
                          {stop.prioridad && (
                            <span
                              className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${stop.prioridad === "ALTA" ? "bg-secondary text-secondary-foreground" : "bg-muted text-muted-foreground"}`}
                            >
                              {stop.prioridad}
                            </span>
                          )}
                        </div>
                        <p className="mt-1 text-sm font-medium text-foreground">
                          {stop.direccionDestino || "Dirección pendiente"}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {stop.ciudadDestino || ""}
                        </p>
                        <p className="mt-2 text-xs text-muted-foreground">
                          {stop.destinatarioNombre || "Destinatario"} ·{" "}
                          {stop.destinatarioTelefono || "Sin teléfono"} ·{" "}
                          {stop.pesoKg ?? "—"} kg
                        </p>
                      </div>
                      {role === "OPERADOR" && (
                        <div className="flex flex-col gap-1">
                          <Button
                            size="icon-xs"
                            variant="ghost"
                            aria-label="Subir parada"
                            disabled={i === 0 || busy}
                            onClick={() => void reorder(stop.id, -1)}
                          >
                            <ArrowUp className="size-4" />
                          </Button>
                          <Button
                            size="icon-xs"
                            variant="ghost"
                            aria-label="Bajar parada"
                            disabled={i === route.paradas.length - 1 || busy}
                            onClick={() => void reorder(stop.id, 1)}
                          >
                            <ArrowDown className="size-4" />
                          </Button>
                        </div>
                      )}
                    </div>
                    {role === "OPERADOR" && (
                      <div className="mt-3 flex flex-wrap gap-2 border-t border-border pt-3">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            setReassignId(stop.pedidoId);
                            setReassignDriver("");
                            setHistoryOrder(null);
                          }}
                        >
                          Reasignar
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => void showHistory(stop.pedidoId)}
                        >
                          Ver historial
                        </Button>
                      </div>
                    )}
                    {role === "OPERADOR" && reassignId === stop.pedidoId && (
                      <div className="mt-3 space-y-2 rounded-xl bg-muted p-3">
                        <label
                          className="text-xs font-medium"
                          htmlFor={`nuevo-${stop.id}`}
                        >
                          Nuevo conductor
                        </label>
                        <select
                          id={`nuevo-${stop.id}`}
                          aria-label="Seleccionar nuevo conductor"
                          className="h-10 w-full rounded-lg border border-input bg-background px-2 text-sm"
                          value={reassignDriver}
                          onChange={(e) =>
                            setReassignDriver(
                              e.target.value ? Number(e.target.value) : "",
                            )
                          }
                        >
                          <option value="">Selecciona un conductor</option>
                          {drivers
                            .filter((d) => d.usuarioId !== route.conductorId)
                            .map((d) => (
                              <option key={d.usuarioId} value={d.usuarioId}>
                                {d.nombre} · {d.entregasAsignadas}/
                                {d.maxEntregasDia}
                              </option>
                            ))}
                        </select>
                        <label
                          className="block text-xs font-medium"
                          htmlFor={`motivo-${stop.id}`}
                        >
                          Motivo de reasignación
                        </label>
                        <textarea
                          id={`motivo-${stop.id}`}
                          aria-label="Motivo de reasignación"
                          value={reason}
                          maxLength={500}
                          onChange={(e) => setReason(e.target.value)}
                          placeholder="Ej. avería del vehículo"
                          className="w-full rounded-lg border border-input bg-background p-2 text-sm"
                        />
                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            disabled={!reassignDriver || busy}
                            onClick={() => void reassign(stop.pedidoId)}
                          >
                            Confirmar reasignación
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setReassignId(null)}
                          >
                            Cancelar
                          </Button>
                        </div>
                      </div>
                    )}
                    {role === "OPERADOR" && historyOrder === stop.pedidoId && (
                      <div className="mt-3 rounded-xl bg-muted p-3 text-xs">
                        <h3 className="font-semibold">
                          Historial de asignaciones
                        </h3>
                        {!history.length && (
                          <p className="mt-2 text-muted-foreground">Sin registros.</p>
                        )}
                        {history.map((h) => (
                          <p key={h.id} className="mt-2 border-t pt-2">
                            {h.accion === "ASIGNACION"
                              ? "Asignación"
                              : "Reasignación"}{" "}
                            · {formatDate(h.fecha)} · Conductor{" "}
                            {h.conductorAnteriorId || "sin asignar"} →{" "}
                            {h.conductorNuevoId}
                            {h.motivo ? ` · ${h.motivo}` : ""}
                          </p>
                        ))}
                      </div>
                    )}
                  </li>
                ))}
              </ol>
              {!route.paradas.length && (
                <p className="mt-5 text-sm text-muted-foreground">
                  Tu ruta existe, pero no tiene entregas pendientes.
                </p>
              )}
            </>
          ) : (
            <div className="mt-5 rounded-xl border border-dashed border-border p-7 text-center">
              <RouteIcon className="mx-auto size-7 text-muted-foreground" />
              <p className="mt-3 text-sm text-muted-foreground">
                {loading
                  ? "Consultando ruta..."
                  : role === "CONDUCTOR"
                    ? "Todavía no tienes una ruta para hoy."
                    : "Selecciona un conductor para consultar su ruta."}
              </p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
