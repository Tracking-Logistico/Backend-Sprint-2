import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ClipboardList, Map, PackageCheck } from "lucide-react";
import { UnavailableModule } from "@/components/shared/UnavailableModule";
import { usePageMeta } from "@/hooks/usePageMeta";

const modules = {
  pedidos: {
    icon: ClipboardList,
    story: "HU-03A",
    title: "Recepción y priorización de pedidos",
    description:
      "Aquí el operador podrá revisar, validar y ordenar los pedidos entrantes según prioridad y capacidad operativa.",
  },
  envios: {
    icon: PackageCheck,
    story: "HU-03B",
    title: "Activación de envíos y etiquetas",
    description:
      "Este espacio centralizará la activación, generación de etiqueta y consulta de seguimiento de cada envío.",
  },
  rutas: {
    icon: Map,
    story: "HU-09",
    title: "Gestión y asignación de rutas",
    description:
      "La vista permitirá organizar recorridos, asignar conductores y consultar el estado de cada ruta.",
  },
} as const;
export function ModulePage() {
  const { module } = useParams<{ module: keyof typeof modules }>();
  const config = module ? modules[module] : undefined;
  usePageMeta(config?.title ?? "Módulo", "Módulo operativo de LogisTrack.");
  if (!config) return <div className="p-8">Módulo no encontrado.</div>;
  const Icon = config.icon;
  return (
    <div className="mx-auto max-w-6xl px-5 py-8 sm:px-8 lg:px-10">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link
            to="/panel"
            className="inline-flex items-center gap-2 text-sm text-slate-500 hover:text-slate-950"
          >
            <ArrowLeft className="size-4" />
            Volver al resumen
          </Link>
          <div className="mt-7 flex items-center gap-3">
            <span className="grid size-11 place-items-center rounded-xl bg-slate-950 text-emerald-300">
              <Icon className="size-5" />
            </span>
            <div>
              <p className="text-sm font-medium text-emerald-700">
                {config.story}
              </p>
              <h1 className="text-3xl font-semibold tracking-tight">
                {config.title}
              </h1>
            </div>
          </div>
        </div>
      </div>
      <div className="mt-10">
        <UnavailableModule {...config}>
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-xl border border-dashed border-slate-200 p-5">
              <p className="text-sm font-medium">Interfaz lista</p>
              <p className="mt-2 text-xs leading-5 text-slate-500">
                La navegación y los estados de carga, vacío y error ya están
                contemplados.
              </p>
            </div>
            <div className="rounded-xl border border-dashed border-slate-200 p-5">
              <p className="text-sm font-medium">Contrato requerido</p>
              <p className="mt-2 text-xs leading-5 text-slate-500">
                Faltan endpoints REST y DTOs en el backend actual para persistir
                cambios.
              </p>
            </div>
            <div className="rounded-xl border border-dashed border-slate-200 p-5">
              <p className="text-sm font-medium">Sin datos falsos</p>
              <p className="mt-2 text-xs leading-5 text-slate-500">
                No se muestran registros inventados ni se realizan mutaciones
                locales que parezcan persistidas.
              </p>
            </div>
          </div>
        </UnavailableModule>
      </div>
    </div>
  );
}
