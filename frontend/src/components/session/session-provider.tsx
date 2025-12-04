"use client";

import { ReactNode, createContext, useCallback, useContext, useEffect, useMemo } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, clearAuthToken, setAuthToken } from "@/lib/api";
import { env } from "@/lib/env";

type SessionStatus = "loading" | "authenticated" | "unauthenticated";

type MemberSummary = {
  memberId: string;
  email: string;
  name: string;
  role: string;
};

type SessionContextValue = {
  status: SessionStatus;
  member: MemberSummary | null;
  error: unknown;
  refresh: () => Promise<void>;
  setToken: (token: string | null) => void;
};

type FetchErrorLike = {
  status?: number;
};

const SessionContext = createContext<SessionContextValue | undefined>(undefined);
const SESSION_QUERY_KEY = ["session", "current"] as const;

async function fetchSession(): Promise<MemberSummary | null> {
  const response = await api.GET("/auth/me" as any);
  if (response.error) {
    const status = (response.error as FetchErrorLike).status;
    if (status === 401 || status === 419) {
      return null;
    }
    throw response.error;
  }

  const payload = response.data?.data;
  if (!payload) {
    return null;
  }

  return {
    memberId: payload.memberId,
    email: payload.email,
    name: payload.name,
    role: payload.role
  };
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (env.mockToken) {
      setAuthToken(env.mockToken);
    }
  }, []);

  const sessionQuery = useQuery({
    queryKey: SESSION_QUERY_KEY,
    queryFn: fetchSession,
    retry: false,
    refetchOnWindowFocus: false
  });

  const refresh = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
  }, [queryClient]);

  const setToken = useCallback(
    (token: string | null) => {
      if (token) {
        setAuthToken(token);
      } else {
        clearAuthToken();
      }
      void queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
    },
    [queryClient]
  );

  const value = useMemo<SessionContextValue>(() => {
    const status: SessionStatus =
      sessionQuery.status === "pending"
        ? "loading"
        : sessionQuery.data
          ? "authenticated"
          : "unauthenticated";

    return {
      status,
      member: sessionQuery.data ?? null,
      error: sessionQuery.error,
      refresh,
      setToken
    };
  }, [sessionQuery.status, sessionQuery.data, sessionQuery.error, refresh, setToken]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession는 SessionProvider 내부에서만 사용할 수 있습니다.");
  }
  return context;
}
