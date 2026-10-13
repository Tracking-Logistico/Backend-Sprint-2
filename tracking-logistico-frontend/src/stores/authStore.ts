import { create } from "zustand";
import { persist } from "zustand/middleware";
import { api } from "@/lib/api";
import type { LoginResponse, Role } from "@/types/api";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  role: Role | null;
  panel: string | null;
  userId: number | null;
  requiresPasswordChange: boolean;
  accessTokenExpiresAt: string | null;
  isHydrated: boolean;
  setHydrated: (hydrated: boolean) => void;
  signIn: (email: string, password: string) => Promise<LoginResponse>;
  signOut: () => Promise<void>;
  applySession: (session: LoginResponse) => void;
  clearSession: () => void;
}

const empty = {
  accessToken: null,
  refreshToken: null,
  role: null,
  panel: null,
  userId: null,
  requiresPasswordChange: false,
  accessTokenExpiresAt: null,
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      ...empty,
      isHydrated: false,
      setHydrated: (isHydrated) => set({ isHydrated }),
      applySession: (session) =>
        set({
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          role: session.rol,
          panel: session.panel,
          userId: session.usuarioId,
          requiresPasswordChange: session.requiereCambioPassword,
          accessTokenExpiresAt: session.accessTokenExpiresAt,
        }),
      signIn: async (email, password) => {
        const session = await api.login(email, password);
        get().applySession(session);
        return session;
      },
      signOut: async () => {
        const token = get().accessToken;
        try {
          if (token) await api.logout(token);
        } finally {
          set(empty);
        }
      },
      clearSession: () => set(empty),
    }),
    {
      name: "logistrack-session",
      onRehydrateStorage: () => (state) => state?.setHydrated(true),
    },
  ),
);
