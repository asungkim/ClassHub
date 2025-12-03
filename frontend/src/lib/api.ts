/**
 * OpenAPI 기반 타입 안전 API 클라이언트
 *
 * 사용법:
 * 1. 백엔드 실행: cd backend && ./gradlew bootRun
 * 2. OpenAPI 스펙 다운로드 및 타입 생성: npm run openapi
 * 3. 이 클라이언트를 import하여 사용
 */

import createClient from "openapi-fetch";
import type { paths } from "@/src/types/openapi";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

/**
 * OpenAPI 스펙 기반 타입 안전 API 클라이언트
 *
 * @example
 * // GET 요청
 * const { data, error } = await api.GET("/auth/me", {
 *   headers: { Authorization: `Bearer ${token}` }
 * });
 *
 * // POST 요청
 * const { data, error } = await api.POST("/auth/login", {
 *   body: { email: "test@test.com", password: "1234" }
 * });
 */
export const api = createClient<paths>({ baseUrl: BASE_URL });

/**
 * JWT 토큰을 설정하는 헬퍼 함수
 *
 * @example
 * setAuthToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
 *
 * // 이후 모든 요청에 자동으로 Authorization 헤더 추가됨
 * const { data } = await api.GET("/auth/me");
 */
export function setAuthToken(token: string | null) {
  if (token) {
    api.use({
      onRequest({ request }) {
        request.headers.set("Authorization", `Bearer ${token}`);
        return request;
      }
    });
  }
}

/**
 * 인증 토큰을 제거하는 헬퍼 함수
 */
export function clearAuthToken() {
  // 새 클라이언트 인스턴스를 생성하여 미들웨어 초기화
  Object.assign(api, createClient<paths>({ baseUrl: BASE_URL }));
}
