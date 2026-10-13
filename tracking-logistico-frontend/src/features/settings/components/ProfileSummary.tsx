import { MapPin, Phone, UserRound } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";

interface Perfil {
  nombre: string;
  telefono: string;
  direccion: string;
}

interface ProfileSummaryProps {
  profile: Perfil;
  role: string | null;
  onEdit?: () => void;
}

export function ProfileSummary({ profile, role, onEdit }: ProfileSummaryProps) {
  return (
    <Card>
      <CardHeader className="flex-row items-center gap-4">
        <Avatar>
          <AvatarFallback>
            <UserRound className="size-7" />
          </AvatarFallback>
        </Avatar>
        <div>
          <CardTitle>{profile.nombre || "Tu perfil"}</CardTitle>
          <p className="text-sm text-muted-foreground">{role ?? "Usuario"}</p>
        </div>
      </CardHeader>
      <CardContent>
        <Separator className="mb-4" />
        <div className="grid gap-3 text-sm sm:grid-cols-2">
          <p className="flex items-center gap-2 text-muted-foreground">
            <Phone className="size-4" />
            {profile.telefono || "Sin teléfono"}
          </p>
          <p className="flex items-center gap-2 text-muted-foreground">
            <MapPin className="size-4" />
            {profile.direccion || "Sin dirección"}
          </p>
        </div>
        {onEdit && (
          <button
            type="button"
            onClick={onEdit}
            className="mt-5 text-sm font-medium text-emerald-700 hover:text-emerald-800"
          >
            Editar datos
          </button>
        )}
      </CardContent>
    </Card>
  );
}
