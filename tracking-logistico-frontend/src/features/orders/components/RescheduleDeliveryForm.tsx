import { useState, type FormEvent } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";

interface RescheduleDeliveryFormProps {
  desde: string;
  hasta: string;
  loading: boolean;
  error: string;
  onSubmit: (fecha: string) => Promise<void>;
}

export function RescheduleDeliveryForm({
  desde,
  hasta,
  loading,
  error,
  onSubmit,
}: RescheduleDeliveryFormProps) {
  const [fecha, setFecha] = useState("");
  const [validationError, setValidationError] = useState("");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!fecha) {
      setValidationError("Selecciona una fecha para la nueva entrega.");
      return;
    }
    if (fecha < desde || fecha > hasta) {
      setValidationError(`Elige una fecha entre ${desde} y ${hasta}.`);
      return;
    }
    setValidationError("");
    void onSubmit(fecha);
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Solicitar nueva fecha de entrega</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="grid gap-4" onSubmit={submit}>
          <div className="grid gap-2">
            <Label htmlFor="reschedule-date">Nueva fecha</Label>
            <Input
              id="reschedule-date"
              type="date"
              min={desde}
              max={hasta}
              value={fecha}
              onChange={(event) => {
                setFecha(event.target.value);
                setValidationError("");
              }}
              disabled={loading}
              required
            />
            <p className="text-sm text-muted-foreground">
              Puedes elegir una fecha entre {desde} y {hasta}.
            </p>
          </div>
          {(validationError || error) && (
            <Alert variant="destructive">
              <AlertTitle>No pudimos solicitar la reprogramación</AlertTitle>
              <AlertDescription>{validationError || error}</AlertDescription>
            </Alert>
          )}
          <Button
            type="submit"
            className="min-h-11 w-full sm:w-auto"
            disabled={loading}
          >
            {loading ? "Solicitando..." : "Confirmar nueva fecha"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
