import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { api, getApiError } from "@/lib/api";
import { toast } from "sonner";

interface RegistrationSuccessProps {
  email: string;
}

export function RegistrationSuccess({ email }: RegistrationSuccessProps) {
  const [resendMessage, setResendMessage] = useState("");
  const [resending, setResending] = useState(false);
  const navigate = useNavigate();

  const handleResend = async () => {
    setResending(true);
    try {
      const msg = await api.resendVerification(email);
      setResendMessage(msg);
      toast.success("Correo de verificación reenviado.");
    } catch (e) {
      const errorMsg = getApiError(e);
      setResendMessage(errorMsg);
      toast.error(errorMsg);
    } finally {
      setResending(false);
    }
  };

  return (
    <Card className="w-full max-w-xl border-slate-200 bg-white shadow-sm">
      <CardHeader className="text-center pt-8">
        <CheckCircle2
          className="mx-auto size-14 text-emerald-600"
          aria-hidden="true"
        />
        <h1 className="mt-4 text-3xl font-semibold tracking-tight text-slate-950">
          Tu cuenta está lista
        </h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Ya puedes iniciar sesión y gestionar tus envíos. La verificación por
          correo es opcional y no necesitas esperar un mensaje para utilizar tu
          cuenta.
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        {resendMessage && (
          <Alert
            role="status"
            className="border-emerald-200 bg-emerald-50 text-emerald-800"
          >
            <AlertDescription className="text-emerald-800">
              {resendMessage}
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
      <CardFooter className="flex flex-col gap-3 pb-8 sm:flex-row sm:justify-center">
        <Button
          variant="outline"
          onClick={handleResend}
          disabled={resending}
          className="w-full sm:w-auto"
        >
          {resending ? "Enviando..." : "Reenviar correo opcional"}
        </Button>
        <Button
          onClick={() => navigate("/login")}
          className="w-full sm:w-auto gap-2 bg-slate-950 text-white hover:bg-slate-800"
        >
          Ir a iniciar sesión
          <ArrowRight className="size-4" aria-hidden="true" />
        </Button>
      </CardFooter>
    </Card>
  );
}
