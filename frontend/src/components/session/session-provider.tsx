"use client";

import { ReactNode, createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
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
  refreshSession: () => Promise<boolean>;
  setToken: (token: string | null) => void;
  logout: () => Promise<void>;
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
  const [hasAttemptedRefresh, setHasAttemptedRefresh] = useState(Boolean(env.mockToken));

  useEffect(() => {
    if (env.mockToken) {
      setAuthToken(env.mockToken);
      setHasAttemptedRefresh(true);
    }
  }, []);

  const performRefresh = useCallback(async () => {
    try {
      const response = await api.POST("/auth/refresh" as any, {});
      const token = response.data?.data?.accessToken;
      if (token) {
        setAuthToken(token);
        return true;
      }
      clearAuthToken();
      return false;
    } catch {
      clearAuthToken();
      return false;
    }
  }, []);

  useEffect(() => {
    if (env.mockToken) {
      return;
    }
    let isMounted = true;
    const run = async () => {
      await performRefresh();
      if (isMounted) {
        setHasAttemptedRefresh(true);
      }
    };
    void run();
    return () => {
      isMounted = false;
    };
  }, [performRefresh]);

  const sessionQuery = useQuery({
    queryKey: SESSION_QUERY_KEY,
    queryFn: fetchSession,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: hasAttemptedRefresh
  });

  const refreshSession = useCallback(async () => {
    const success = await performRefresh();
    await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
    return success;
  }, [performRefresh, queryClient]);

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

  const logout = useCallback(async () => {
    try {
      await api.POST("/auth/logout" as any, {});
    } catch {
      // ignore
    } finally {
      clearAuthToken();
      await queryClient.resetQueries({ queryKey: SESSION_QUERY_KEY });
    }
  }, [queryClient]);

  const value = useMemo<SessionContextValue>(() => {
    const status: SessionStatus =
      !hasAttemptedRefresh || sessionQuery.status === "pending"
        ? "loading"
        : sessionQuery.data
          ? "authenticated"
          : "unauthenticated";

    return {
      status,
      member: sessionQuery.data ?? null,
      error: sessionQuery.error,
      refreshSession,
      setToken,
      logout
    };
  }, [hasAttemptedRefresh, sessionQuery.status, sessionQuery.data, sessionQuery.error, refreshSession, setToken, logout]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession는 SessionProvider 내부에서만 사용할 수 있습니다.");
  }
  return context;
}
