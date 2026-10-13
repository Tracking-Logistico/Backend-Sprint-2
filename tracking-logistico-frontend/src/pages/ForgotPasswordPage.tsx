import { useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/shared/FormField";
import { api, getApiError } from "@/lib/api";
import { usePageMeta } from "@/hooks/usePageMeta";

export function ForgotPasswordPage() {
  usePageMeta(
    "Recuperar contraseña",
    "Solicita instrucciones para recuperar tu contraseña.",
  );
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      const response = await api.forgotPassword(email);
      setMessage(response);
    } catch (err) {
      setError(getApiError(err));
    } finally {
      setLoading(false);
    }
  }
  return (
    <main className="grid min-h-[calc(100vh-4.5rem)] place-items-center bg-[#f4f7f5] px-5">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-9">
        <div className="grid size-11 place-items-center rounded-lg bg-emerald-100 text-emerald-700">
          <Mail className="size-5" aria-hidden="true" />
        </div>
        <h1 className="mt-7 text-3xl font-semibold">Recuperar contraseña</h1>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          Intentaremos enviar instrucciones al correo asociado a tu cuenta si el
          servicio de correo está disponible.
        </p>
        <form className="mt-8 space-y-5" onSubmit={submit}>
          <FormField
            label="Correo electrónico"
            name="email"
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          {message && (
            <p
              role="status"
              className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700"
            >
              {message}
            </p>
          )}
          {error && (
            <p
              role="alert"
              className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700"
            >
              {error}
            </p>
          )}
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? "Enviando..." : "Enviar instrucciones"}
          </Button>
        </form>
        <Link
          to="/login"
          className="mt-6 inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-950"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Volver al inicio de sesión
        </Link>
      </div>
    </main>
  );
}
