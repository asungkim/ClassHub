import createClient from "openapi-fetch";
import { env } from "@/lib/env";
import type { components, paths } from "@/types/openapi";

const fetchWithCredentials: typeof fetch = (input, init) =>
  fetch(input, {
    ...init,
    credentials: "include"
  });

const apiClient = createClient<paths>({
  baseUrl: env.apiBaseUrl,
  fetch: fetchWithCredentials
});

export const api = apiClient;

let currentToken: string | null = null;
let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;
const MAX_REFRESH_ATTEMPTS = 3;
let refreshAttempts = 0;
const retryRequestStore = new WeakMap<Request, Request>();

type LogoutRequest = components["schemas"]["LogoutRequest"];

/**
 * 요청 헤더에 Authorization 토큰을 자동으로 주입한다.
 */
export function setAuthToken(token: string | null) {
  currentToken = token;
}

/**
 * 현재 저장된 토큰을 반환한다.
 */
export function getAuthToken(): string | null {
  return currentToken;
}

/**
 * 중간에 주입된 토큰을 제거한다.
 */
export function clearAuthToken() {
  currentToken = null;
}

/**
 * Refresh 토큰으로 새로운 access 토큰을 발급받는다.
 * 실패 시 null을 반환한다.
 */
export async function tryRefreshToken(): Promise<string | null> {
  if (refreshAttempts >= MAX_REFRESH_ATTEMPTS) {
    refreshAttempts = 0;
    return null;
  }
  try {
    refreshAttempts += 1;
    const response = await api.POST("/api/v1/auth/refresh", {});
    const token = response.data?.data?.accessToken;
    if (token) {
      refreshAttempts = 0;
      return token;
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * 로그아웃 처리: 토큰 제거 + 로그인 페이지로 이동
 */
export async function forceLogout() {
  try {
    const body: LogoutRequest = { logoutAll: false };
    await api.POST("/api/v1/auth/logout", { body });
  } catch {
    // ignore
  } finally {
    clearAuthToken();
    if (typeof window !== "undefined") {
      window.location.href = "/";
    }
  }
}

// Request Interceptor: 모든 요청에 토큰을 자동으로 주입
api.use({
  onRequest({ request }) {
    if (request.body && !retryRequestStore.has(request)) {
      retryRequestStore.set(request, request.clone());
    }
    if (currentToken) {
      request.headers.set("Authorization", `Bearer ${currentToken}`);
    }
    return request;
  }
});

// Response Interceptor: 401 에러 시 자동으로 토큰 갱신 후 재시도
api.use({
  async onResponse({ response, request }) {
    // 401 Unauthorized 에러 감지
    if (response.status === 401) {
      // /auth/refresh, /auth/login 요청은 재시도하지 않음
      const url = new URL(request.url);
      if (
        url.pathname.includes("/auth/refresh") ||
        url.pathname.includes("/auth/login") ||
        url.pathname.includes("/auth/logout")
      ) {
        return response;
      }

      // 동시 요청들이 중복으로 refresh 하지 않도록 Promise 공유
      if (!isRefreshing) {
        isRefreshing = true;
        refreshPromise = tryRefreshToken();
      }

      const newToken = await refreshPromise;
      isRefreshing = false;
      refreshPromise = null;

      if (newToken) {
        // 새 토큰으로 업데이트
        setAuthToken(newToken);

        // 원래 요청을 새 토큰으로 재시도
        const retryRequest = retryRequestStore.get(request) ?? request;
        retryRequestStore.delete(request);
        const newHeaders = new Headers(retryRequest.headers);
        newHeaders.set("Authorization", `Bearer ${newToken}`);

        return fetch(retryRequest.url, {
          method: retryRequest.method,
          headers: newHeaders,
          body: retryRequest.body ?? undefined,
          credentials: "include"
        });
      } else {
        // Refresh 토큰도 만료됨 → 강제 로그아웃
        await forceLogout();
      }
    }

    return response;
  }
});
