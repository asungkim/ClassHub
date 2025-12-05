import createClient from "openapi-fetch";
import { env } from "@/lib/env";
import type { paths } from "@/types/openapi";

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

// 모든 요청에 토큰을 자동으로 주입하는 interceptor
api.use({
  onRequest({ request }) {
    if (currentToken) {
      request.headers.set("Authorization", `Bearer ${currentToken}`);
    }
    return request;
  }
});
