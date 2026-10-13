import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { CheckCircle2, CircleAlert, LoaderCircle } from "lucide-react";
import { api, getApiError } from "@/lib/api";
import { usePageMeta } from "@/hooks/usePageMeta";

export function VerifyPage() {
  usePageMeta("Verificar cuenta", "Verifica tu cuenta de cliente.");
  const [params] = useSearchParams();
  const token = params.get("token");
  const [state, setState] = useState<"loading" | "success" | "error">(
    token ? "loading" : "error",
  );
  const [message, setMessage] = useState(
    token
      ? ""
      : "El enlace de verificación no contiene un token válido. Puedes iniciar sesión sin verificar tu correo.",
  );
  const verifiedTokenRef = useRef<string | null>(null);
  useEffect(() => {
    if (!token || verifiedTokenRef.current === token) return;
    verifiedTokenRef.current = token;
    api
      .verifyClient(token)
      .then((response) => {
        setState("success");
        setMessage(response);
      })
      .catch((error: unknown) => {
        setState("error");
        setMessage(
          `${getApiError(error, "El enlace no es válido o ya expiró.")} Puedes iniciar sesión sin verificar tu correo.`,
        );
      });
  }, [token]);
  return (
    <main className="grid min-h-[calc(100vh-4.5rem)] place-items-center px-5">
      <div className="max-w-md text-center">
        {state === "loading" && (
          <LoaderCircle className="mx-auto size-12 animate-spin text-emerald-600" />
        )}
        {state === "success" && (
          <CheckCircle2 className="mx-auto size-14 text-emerald-600" />
        )}
        {state === "error" && (
          <CircleAlert className="mx-auto size-14 text-rose-600" />
        )}
        <h1 className="mt-6 text-3xl font-semibold">
          {state === "success"
            ? "Cuenta verificada"
            : state === "error"
              ? "No pudimos verificarla"
              : "Verificando tu cuenta"}
        </h1>
        <p className="mt-3 leading-7 text-slate-600">{message}</p>
        {state !== "loading" && (
          <Link
            to="/login"
            className="mt-8 inline-flex rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-medium text-white"
          >
            Ir a iniciar sesión
          </Link>
        )}
      </div>
    </main>
  );
}
