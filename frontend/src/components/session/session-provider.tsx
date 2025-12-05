"use client";

import { ReactNode, createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, clearAuthToken, setAuthToken } from "@/lib/api";
import { getFetchError } from "@/lib/api-error";
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
  error: string | null;
  refreshSession: () => Promise<boolean>;
  setToken: (token: string | null) => void;
  logout: () => Promise<void>;
};

const SessionContext = createContext<SessionContextValue | undefined>(undefined);
const SESSION_QUERY_KEY = ["session", "current"] as const;

async function fetchSession(): Promise<MemberSummary | null> {
  const response = await api.GET("/api/v1/auth/me");
  const fetchError = getFetchError(response);
  if (fetchError) {
    if (fetchError.status === 401 || fetchError.status === 419) {
      return null;
    }
    throw fetchError;
  }

  const payload = response.data?.data;
  if (!payload) {
    return null;
  }

  return {
    memberId: payload.memberId ?? "",
    email: payload.email ?? "",
    name: payload.name ?? "",
    role: payload.role ?? ""
  };
}

async function tryRefreshToken(): Promise<string | null> {
  try {
    const response = await api.POST("/api/v1/auth/refresh", {});
    const token = response.data?.data?.accessToken;
    return token ?? null;
  } catch {
    return null;
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<SessionStatus>("loading");
  const [member, setMember] = useState<MemberSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 초기화: refresh 토큰으로 access 토큰 발급 → 사용자 정보 조회
  useEffect(() => {
    let isMounted = true;

    async function initializeSession() {
      try {
        setStatus("loading");
        setError(null);

        // Mock 토큰이 있으면 사용
        if (env.mockToken) {
          setAuthToken(env.mockToken);
        } else {
          // Refresh 토큰으로 access 토큰 발급
          const accessToken = await tryRefreshToken();
          if (!accessToken) {
            if (isMounted) {
              setStatus("unauthenticated");
            }
            return;
          }
          setAuthToken(accessToken);
        }

        // 사용자 정보 조회
        const memberData = await fetchSession();
        if (isMounted) {
          if (memberData) {
            setMember(memberData);
            setStatus("authenticated");
          } else {
            clearAuthToken();
            setStatus("unauthenticated");
          }
        }
      } catch (err) {
        if (isMounted) {
          clearAuthToken();
          setError(err instanceof Error ? err.message : "세션 초기화 중 오류가 발생했습니다.");
          setStatus("unauthenticated");
        }
      }
    }

    void initializeSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const refreshSession = useCallback(async () => {
    try {
      const accessToken = await tryRefreshToken();
      if (!accessToken) {
        clearAuthToken();
        setMember(null);
        setStatus("unauthenticated");
        await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
        return false;
      }

      setAuthToken(accessToken);
      const memberData = await fetchSession();
      if (memberData) {
        setMember(memberData);
        setStatus("authenticated");
        await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
        return true;
      }

      clearAuthToken();
      setMember(null);
      setStatus("unauthenticated");
      return false;
    } catch {
      clearAuthToken();
      setMember(null);
      setStatus("unauthenticated");
      return false;
    }
  }, [queryClient]);

  const setToken = useCallback(
    async (token: string | null) => {
      try {
        if (token) {
          setAuthToken(token);
          const memberData = await fetchSession();
          if (memberData) {
            setMember(memberData);
            setStatus("authenticated");
          } else {
            clearAuthToken();
            setMember(null);
            setStatus("unauthenticated");
          }
        } else {
          clearAuthToken();
          setMember(null);
          setStatus("unauthenticated");
        }
        await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
      } catch {
        clearAuthToken();
        setMember(null);
        setStatus("unauthenticated");
      }
    },
    [queryClient]
  );

  const logout = useCallback(async () => {
    try {
      await api.POST("/api/v1/auth/logout", {});
    } catch {
      // ignore
    } finally {
      clearAuthToken();
      setMember(null);
      setStatus("unauthenticated");
      await queryClient.resetQueries({ queryKey: SESSION_QUERY_KEY });
    }
  }, [queryClient]);

  const value = useMemo<SessionContextValue>(() => {
    return {
      status,
      member,
      error,
      refreshSession,
      setToken,
      logout
    };
  }, [status, member, error, refreshSession, setToken, logout]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession는 SessionProvider 내부에서만 사용할 수 있습니다.");
  }
  return context;
}
