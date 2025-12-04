import createClient from "openapi-fetch";
import { env } from "@/lib/env";
import type { paths } from "@/types/openapi";

const apiClient = createClient<paths>({
  baseUrl: env.apiBaseUrl
});

export const api = apiClient;

/**
 * 요청 헤더에 Authorization 토큰을 자동으로 주입한다.
 */
export function setAuthToken(token: string | null) {
  if (!token) return;

  api.use({
    onRequest({ request }) {
      request.headers.set("Authorization", `Bearer ${token}`);
      return request;
    }
  });
}

/**
 * 중간에 주입된 토큰 interceptors를 제거한다.
 */
export function clearAuthToken() {
  Object.assign(api, createClient<paths>({ baseUrl: env.apiBaseUrl }));
}
