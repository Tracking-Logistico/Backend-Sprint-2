import type { MyShipmentsFilters } from "../hooks/useMyShipments";
import type { OrderStatus } from "@/types/api";

const statuses: Array<{ value: OrderStatus; label: string }> = [
  { value: "SOLICITADO", label: "Pendiente de validación" },
  { value: "CORRECCION_SOLICITADA", label: "Corrección solicitada" },
  { value: "CREADO", label: "Despacho aprobado" },
  { value: "RECIBIDO_EN_ORIGEN", label: "Recibido en origen" },
  { value: "EN_TRANSITO", label: "En tránsito" },
  { value: "EN_REPARTO", label: "En reparto" },
  { value: "ENTREGADO", label: "Entregado" },
  { value: "RECHAZADO", label: "Rechazado" },
];

const inputClass =
  "h-11 w-full rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/10";

export function MyShipmentsFilters({
  filters,
  onChange,
}: {
  filters: MyShipmentsFilters;
  onChange: (next: Partial<MyShipmentsFilters>) => void;
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 shadow-sm">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <label className="text-sm font-medium">
          Estado
          <select
            id="shipments-status-filter"
            aria-label="Filtrar envíos por estado"
            className={`${inputClass} mt-2`}
            value={filters.estado}
            onChange={(event) =>
              onChange({ estado: event.target.value as OrderStatus | "" })
            }
          >
            <option value="">Todos los estados</option>
            {statuses.map((status) => (
              <option key={status.value} value={status.value}>
                {status.label}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-medium">
          Desde
          <input
            id="shipments-date-from"
            aria-label="Filtrar envíos desde una fecha"
            className={`${inputClass} mt-2`}
            type="date"
            value={filters.fechaDesde}
            onChange={(event) => onChange({ fechaDesde: event.target.value })}
          />
        </label>
        <label className="text-sm font-medium">
          Hasta
          <input
            id="shipments-date-to"
            aria-label="Filtrar envíos hasta una fecha"
            className={`${inputClass} mt-2`}
            type="date"
            value={filters.fechaHasta}
            onChange={(event) => onChange({ fechaHasta: event.target.value })}
          />
        </label>
        <label className="text-sm font-medium">
          Ordenar por
          <select
            id="shipments-sort"
            aria-label="Ordenar envíos"
            className={`${inputClass} mt-2`}
            value={`${filters.sort},${filters.direction}`}
            onChange={(event) => {
              const [sort, direction] = event.target.value.split(",") as [
                MyShipmentsFilters["sort"],
                MyShipmentsFilters["direction"],
              ];
              onChange({ sort, direction });
            }}
          >
            <option value="fechaCreacion,desc">Fecha: más recientes</option>
            <option value="fechaCreacion,asc">Fecha: más antiguas</option>
            <option value="estado,asc">Estado: A-Z</option>
            <option value="estado,desc">Estado: Z-A</option>
          </select>
        </label>
      </div>
    </div>
  );
}
