import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

interface DeactivateAccountDialogProps {
  onConfirm: () => void;
  loading: boolean;
}

export function DeactivateAccountDialog({
  onConfirm,
  loading,
}: DeactivateAccountDialogProps) {
  return (
    <AlertDialog>
      <AlertDialogTrigger
        render={
          <Button variant="destructive" disabled={loading}>
            Desactivar cuenta
          </Button>
        }
      />
      <AlertDialogContent>
        <AlertDialogTitle>Desactivar cuenta</AlertDialogTitle>
        <AlertDialogDescription>
          ¿Seguro que quieres desactivar tu cuenta?
        </AlertDialogDescription>
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <AlertDialogCancel
            render={<Button variant="outline">Cancelar</Button>}
          />
          <AlertDialogAction
            render={
              <Button
                variant="destructive"
                disabled={loading}
                onClick={onConfirm}
              >
                Confirmar
              </Button>
            }
          />
        </div>
      </AlertDialogContent>
    </AlertDialog>
  );
}
