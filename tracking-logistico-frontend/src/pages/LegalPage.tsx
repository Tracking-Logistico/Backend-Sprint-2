import { Link } from "react-router-dom";
import { usePageMeta } from "@/hooks/usePageMeta";

type Kind = "terms" | "privacy";

export function LegalPage({ kind }: { kind: Kind }) {
  const terms = kind === "terms";
  const title = terms
    ? "Términos y condiciones"
    : "Política de tratamiento de datos";
  usePageMeta(title, `${title} de LogisTrack`);
  return (
    <main className="min-h-screen bg-[#f4f7f5] px-5 py-12 sm:px-8">
      <article className="mx-auto max-w-3xl rounded-2xl border border-slate-200 bg-white p-7 shadow-sm sm:p-10">
        <Link
          to="/registro"
          className="text-sm font-semibold text-emerald-700 hover:underline"
        >
          ← Volver al registro
        </Link>
        <p className="mt-9 text-xs font-semibold uppercase tracking-[0.16em] text-emerald-700">
          Versión 2026-09 · entorno académico de pruebas
        </p>
        <h1 className="mt-3 text-3xl font-semibold tracking-tight">{title}</h1>
        <p className="mt-5 rounded-lg bg-amber-50 p-4 text-sm leading-6 text-amber-900">
          Documento informativo provisional para las pruebas del proyecto. Antes
          de registrar usuarios reales, la entidad responsable debe publicar
          aquí sus términos y su política completos y vigentes.
        </p>
        {terms ? (
          <div className="mt-8 space-y-6 text-sm leading-7 text-slate-700">
            <section>
              <h2 className="text-lg font-semibold text-slate-950">
                Objeto del sistema
              </h2>
              <p>
                LogisTrack permite solicitar y seguir envíos de prueba, y
                coordinar procesos logísticos entre clientes y personal interno
                autorizado.
              </p>
            </section>
            <section>
              <h2 className="text-lg font-semibold text-slate-950">
                Uso de la cuenta
              </h2>
              <p>
                Cada persona debe registrar datos de contacto veraces, mantener
                sus credenciales bajo control y respetar las funcionalidades
                asociadas a su rol.
              </p>
            </section>
            <section>
              <h2 className="text-lg font-semibold text-slate-950">
                Alcance de las pruebas
              </h2>
              <p>
                Los estados y etiquetas generados en este entorno no constituyen
                por sí mismos aceptación de un servicio de transporte comercial.
              </p>
            </section>
          </div>
        ) : (
          <div className="mt-8 space-y-6 text-sm leading-7 text-slate-700">
            <section>
              <h2 className="text-lg font-semibold text-slate-950">
                Datos tratados
              </h2>
              <p>
                El sistema almacena información de registro, contacto,
                direcciones de envíos, eventos de seguimiento y aceptación de
                documentos para gestionar la operación de prueba.
              </p>
            </section>
            <section>
              <h2 className="text-lg font-semibold text-slate-950">
                Finalidades
              </h2>
              <p>
                Los datos se usan para autenticar cuentas, enviar enlaces de
                verificación y recuperación, asociar pedidos y conservar su
                trazabilidad.
              </p>
            </section>
            <section>
              <h2 className="text-lg font-semibold text-slate-950">
                Acceso y conservación
              </h2>
              <p>
                El acceso depende del rol. La desactivación de la cuenta impide
                nuevos ingresos y conserva el historial logístico en esta
                versión académica.
              </p>
            </section>
          </div>
        )}
      </article>
    </main>
  );
}
