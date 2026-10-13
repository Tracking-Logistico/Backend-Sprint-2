import { ClipboardList, Map, PackageCheck, Settings } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";
import { usePageMeta } from "@/hooks/usePageMeta";
import type { Role } from "@/types/api";
import { DriverDashboard } from "@/features/driver/components/DriverDashboard";

const cards = [
  {
    key: "pedidos",
    title: "Pedidos",
    description: "Solicita, consulta o valida pedidos según tu rol.",
    icon: ClipboardList,
    roles: ["CLIENTE", "OPERADOR"] as Role[],
  },
  {
    key: "envios",
    title: "Envíos",
    description: "Activa tracking, genera etiquetas y avanza el despacho.",
    icon: PackageCheck,
    roles: ["OPERADOR"] as Role[],
  },
  {
    key: "rutas",
    title: "Rutas",
    description: "Organiza entregas o consulta tu jornada asignada.",
    icon: Map,
    roles: ["OPERADOR", "CONDUCTOR"] as Role[],
  },
  {
    key: "configuracion",
    title: "Cuenta",
    description: "Actualiza la seguridad de tu cuenta.",
    icon: Settings,
    roles: ["CLIENTE", "OPERADOR", "CONDUCTOR"] as Role[],
  },
];

export function DashboardPage() {
  usePageMeta("Panel", "Centro operativo de LogisTrack.");
  const role = useAuthStore((s) => s.role);
  const roleName =
    role === "CLIENTE"
      ? "Cliente"
      : role === "OPERADOR"
        ? "Operador"
        : "Conductor";
  if (role === "CONDUCTOR") return <DriverDashboard />;
  return (
    <div className="mx-auto max-w-6xl px-5 py-8 sm:px-8 lg:px-10">
      <header className="border-b border-border pb-8">
        <p className="text-sm font-semibold uppercase tracking-[0.16em] text-primary">
          Panel {roleName}
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
          Todo lo importante, sin ruido.
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
          Accede únicamente a las funciones habilitadas para tu cuenta. Los
          datos y acciones disponibles se obtienen directamente del backend.
        </p>
      </header>
      <section className="mt-8 grid gap-4 md:grid-cols-2">
        {cards
          .filter((c) => role && c.roles.includes(role))
          .map(({ key, title, description, icon: Icon }) => (
            <Link
              key={key}
              to={`/panel/${key}`}
              className="group rounded-2xl border border-border bg-card p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
            >
              <span className="grid size-11 place-items-center rounded-xl bg-sidebar text-sidebar-primary">
                <Icon className="size-5" />
              </span>
              <h2 className="mt-6 text-lg font-semibold">{title}</h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {description}
              </p>
            </Link>
          ))}
      </section>
    </div>
  );
}
