import { AlertTriangle, ArrowUpRight, LockKeyhole } from "lucide-react";
import type { ReactNode } from "react";

interface UnavailableModuleProps {
  story: string;
  title: string;
  description: string;
  children?: ReactNode;
}

export function UnavailableModule({
  story,
  title,
  description,
  children,
}: UnavailableModuleProps) {
  return (
    <section
      className="overflow-hidden rounded-2xl border border-amber-200 bg-white shadow-sm"
      aria-labelledby="module-title"
    >
      <div className="border-b border-amber-100 bg-amber-50 px-5 py-4">
        <div className="flex items-start gap-3">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-amber-100 text-amber-700">
            <LockKeyhole className="size-4" />
          </span>
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-amber-700">
              {story} · Contrato pendiente
            </p>
            <h2
              id="module-title"
              className="mt-1 text-lg font-semibold text-slate-950"
            >
              {title}
            </h2>
            <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600">
              {description}
            </p>
          </div>
        </div>
      </div>
      {children && <div className="p-5">{children}</div>}
      <div className="flex items-center gap-2 border-t border-slate-100 px-5 py-3 text-xs text-slate-500">
        <AlertTriangle className="size-4 text-amber-600" />
        El backend actual no publica un endpoint REST para operar este módulo.
        <ArrowUpRight className="ml-auto size-4" />
      </div>
    </section>
  );
}
