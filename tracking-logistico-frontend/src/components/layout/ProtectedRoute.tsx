import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";
import type { Role } from "@/types/api";

interface ProtectedRouteProps {
  roles?: Role[];
}

export function ProtectedRoute({ roles }: ProtectedRouteProps) {
  const location = useLocation();
  const { accessToken, role, isHydrated, requiresPasswordChange } =
    useAuthStore();

  if (!isHydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-sidebar text-sm text-sidebar-foreground">
        Cargando sesión...
      </div>
    );
  }
  if (!accessToken)
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (requiresPasswordChange && location.pathname !== "/panel/configuracion")
    return <Navigate to="/panel/configuracion" replace />;
  if (roles && role && !roles.includes(role))
    return <Navigate to="/panel" replace />;
  return <Outlet />;
}
