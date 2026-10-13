import type { FormEvent } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Password {
  actual: string;
  nueva: string;
  confirmacion: string;
}

interface ChangePasswordDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  password: Password;
  onChange: (password: Password) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  loading: boolean;
}

export function ChangePasswordDialog({
  open,
  onOpenChange,
  password,
  onChange,
  onSubmit,
  loading,
}: ChangePasswordDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Cambiar contraseña</DialogTitle>
          <DialogDescription>
            Actualiza la contraseña de acceso a tu cuenta.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} className="grid gap-4">
          <div className="grid gap-2">
            <Label htmlFor="actual">Contraseña actual</Label>
            <Input
              id="actual"
              name="actual"
              type="password"
              required
              value={password.actual}
              onChange={(event) =>
                onChange({ ...password, actual: event.target.value })
              }
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="nueva">Nueva contraseña</Label>
            <Input
              id="nueva"
              name="nueva"
              type="password"
              required
              value={password.nueva}
              onChange={(event) =>
                onChange({ ...password, nueva: event.target.value })
              }
            />
            <p className="text-xs text-muted-foreground">
              Mínimo 8 caracteres con mayúscula, minúscula, número y símbolo.
            </p>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="confirmacion">Confirmar nueva contraseña</Label>
            <Input
              id="confirmacion"
              name="confirmacion"
              type="password"
              required
              value={password.confirmacion}
              onChange={(event) =>
                onChange({ ...password, confirmacion: event.target.value })
              }
            />
          </div>
          <DialogFooter>
            <Button type="submit" disabled={loading}>
              {loading ? "Actualizando..." : "Actualizar contraseña"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
