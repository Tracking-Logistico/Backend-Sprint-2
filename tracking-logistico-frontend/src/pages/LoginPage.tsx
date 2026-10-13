import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  Eye,
  EyeOff,
  PackageCheck,
  ShieldCheck,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/shared/FormField";
import { useAuthStore } from "@/stores/authStore";
import { getApiError } from "@/lib/api";
import { usePageMeta } from "@/hooks/usePageMeta";

export function LoginPage() {
  usePageMeta("Iniciar sesión", "Accede a tu centro de seguimiento logístico.");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [expired] = useState(() => {
    const flagged =
      window.sessionStorage.getItem("logistrack-session-expired") === "1";
    window.sessionStorage.removeItem("logistrack-session-expired");
    return flagged;
  });
  const [loading, setLoading] = useState(false);
  const signIn = useAuthStore((state) => state.signIn);
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/panel";

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      const session = await signIn(email, password);
      const destination =
        from === "/panel" || !from.startsWith("/panel/") ? session.panel : from;
      navigate(
        session.requiereCambioPassword ? "/panel/configuracion" : destination,
        { replace: true },
      );
    } catch (err) {
      setError(
        getApiError(err, "Revisa tus credenciales e inténtalo de nuevo."),
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="grid min-h-[calc(100vh-4.5rem)] lg:grid-cols-[0.9fr_1.1fr]">
      <section className="hidden bg-slate-950 px-10 py-16 text-white lg:flex lg:flex-col lg:justify-between">
        <div>
          <Link to="/" className="inline-flex items-center gap-2 font-semibold">
            <PackageCheck className="size-5 text-emerald-400" />
            LogisTrack
          </Link>
          <div className="mt-24 max-w-md">
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-300">
              Visibilidad operativa
            </p>
            <h1 className="mt-5 text-5xl font-semibold leading-[1.05] tracking-tight">
              Toda la operación en una misma dirección.
            </h1>
            <p className="mt-6 text-lg leading-8 text-slate-300">
              Consulta pedidos, coordina envíos y mantén el control de cada
              entrega.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 text-sm text-slate-400">
          <ShieldCheck className="size-4 text-emerald-400" />
          Sesiones protegidas con tokens de acceso
        </div>
      </section>
      <section className="flex items-center justify-center px-5 py-14 sm:px-10">
        <div className="w-full max-w-md">
          <div className="mb-10 lg:hidden">
            <Link
              to="/"
              className="inline-flex items-center gap-2 font-semibold"
            >
              <PackageCheck className="size-5 text-emerald-600" />
              LogisTrack
            </Link>
          </div>
          <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-700">
            Bienvenido de vuelta
          </p>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight">
            Iniciar sesión
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-500">
            Usa las credenciales entregadas por tu equipo o las de tu cuenta de
            cliente.
          </p>
          {expired && (
            <p
              role="status"
              className="mt-5 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900"
            >
              Tu sesión se cerró por inactividad. Inicia sesión nuevamente.
            </p>
          )}
          <form className="mt-8 space-y-5" onSubmit={submit}>
            <FormField
              label="Correo electrónico"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <div className="relative">
              <FormField
                label="Contraseña"
                name="password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <button
                type="button"
                className="absolute right-3 top-8 text-slate-500 hover:text-slate-950"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={
                  showPassword ? "Ocultar contraseña" : "Mostrar contraseña"
                }
              >
                {showPassword ? (
                  <EyeOff className="size-4" aria-hidden="true" />
                ) : (
                  <Eye className="size-4" aria-hidden="true" />
                )}
              </button>
            </div>
            {error && (
              <p
                role="alert"
                className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700"
              >
                {error}
              </p>
            )}
            <Button
              type="submit"
              className="h-11 w-full gap-2"
              disabled={loading}
            >
              {loading ? "Validando..." : "Entrar al panel"}
              <ArrowRight className="size-4" />
            </Button>
          </form>
          <div className="mt-6 flex justify-between text-sm">
            <Link
              to="/recuperar-password"
              className="font-medium text-emerald-700 hover:underline"
            >
              ¿Olvidaste tu contraseña?
            </Link>
            <Link
              to="/registro"
              className="font-medium text-slate-600 hover:text-slate-950"
            >
              Crear cuenta
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
