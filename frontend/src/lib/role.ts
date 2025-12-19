import type { components } from "@/types/openapi";

// OpenAPI 스키마에서 역할 타입 가져오기
type MeResponse = components["schemas"]["MeResponse"];
export type MemberRole = NonNullable<MeResponse["role"]>;

// 역할 상수 (필요 시 사용)
export const ROLES = {
  TEACHER: "TEACHER" as const,
  ASSISTANT: "ASSISTANT" as const,
  STUDENT: "STUDENT" as const,
  ADMIN: "ADMIN" as const,
  SUPER_ADMIN: "SUPER_ADMIN" as const
} satisfies Record<string, MemberRole>;
