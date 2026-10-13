import { useEffect } from "react";
import { useAuthStore } from "@/stores/authStore";

export function SessionIdleGuard() {
  const token = useAuthStore((state) => state.accessToken);
  const role = useAuthStore((state) => state.role);
  const clearSession = useAuthStore((state) => state.clearSession);

  useEffect(() => {
    if (!token || !role) return;
    const limitMs = (role === "CLIENTE" ? 30 : 15) * 60 * 1000;
    let lastActivity = Date.now();
    let timer: ReturnType<typeof setTimeout>;

    const expireIfInactive = () => {
      const remaining = limitMs - (Date.now() - lastActivity);
      if (remaining <= 0) {
        window.sessionStorage.setItem("logistrack-session-expired", "1");
        clearSession();
        return;
      }
      timer = setTimeout(expireIfInactive, remaining);
    };
    const activity = () => {
      lastActivity = Date.now();
      clearTimeout(timer);
      timer = setTimeout(expireIfInactive, limitMs);
    };
    const visibility = () => {
      if (!document.hidden) {
        clearTimeout(timer);
        expireIfInactive();
      }
    };

    timer = setTimeout(expireIfInactive, limitMs);
    window.addEventListener("pointerdown", activity, { passive: true });
    window.addEventListener("keydown", activity);
    window.addEventListener("focus", visibility);
    document.addEventListener("visibilitychange", visibility);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("pointerdown", activity);
      window.removeEventListener("keydown", activity);
      window.removeEventListener("focus", visibility);
      document.removeEventListener("visibilitychange", visibility);
    };
  }, [token, role, clearSession]);
  return null;
}
