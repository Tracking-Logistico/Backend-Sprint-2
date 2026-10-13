import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, CheckCircle2, KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/shared/FormField";
import { api, getApiError } from "@/lib/api";
import { usePageMeta } from "@/hooks/usePageMeta";

export function ResetPasswordPage() {
  usePageMeta(
    "Restablecer contraseña",
    "Crea una nueva contraseña para tu cuenta.",
  );
  const { token } = useParams<{ token: string }>();
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      setError("El enlace no contiene un token válido.");
      return;
    }
    setError("");
    setLoading(true);
    try {
      await api.resetPassword(token, password, confirmation);
      setSuccess(true);
    } catch (err) {
      setError(getApiError(err, "No se pudo restablecer la contraseña."));
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <main className="grid min-h-[calc(100vh-4.5rem)] place-items-center bg-[#f4f7f5] px-5">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <CheckCircle2 className="mx-auto size-14 text-emerald-600" />
          <h1 className="mt-6 text-3xl font-semibold">
            Contraseña actualizada
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-500">
            Tus sesiones anteriores fueron cerradas. Ya puedes iniciar sesión
            con tu nueva contraseña.
          </p>
          <Link
            to="/login"
            className="mt-8 inline-flex rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-medium text-white"
          >
            Ir a iniciar sesión
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="grid min-h-[calc(100vh-4.5rem)] place-items-center bg-[#f4f7f5] px-5">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-9">
        <div className="grid size-11 place-items-center rounded-lg bg-emerald-100 text-emerald-700">
          <KeyRound className="size-5" />
        </div>
        <h1 className="mt-7 text-3xl font-semibold">Nueva contraseña</h1>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          Debe tener mínimo 8 caracteres, mayúscula, minúscula, número y
          carácter especial.
        </p>
        <form className="mt-8 space-y-5" onSubmit={submit}>
          <FormField
            label="Nueva contraseña"
            name="password"
            type="password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <FormField
            label="Confirmar contraseña"
            name="confirmation"
            type="password"
            required
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
          />
          {error && (
            <p
              role="alert"
              className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700"
            >
              {error}
            </p>
          )}
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? "Actualizando..." : "Actualizar contraseña"}
          </Button>
        </form>
        <Link
          to="/login"
          className="mt-6 inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-950"
        >
          <ArrowLeft className="size-4" />
          Volver al inicio de sesión
        </Link>
      </div>
    </main>
  );
}
