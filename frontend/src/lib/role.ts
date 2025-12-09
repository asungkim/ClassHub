export const Role = {
  TEACHER: "TEACHER",
  ASSISTANT: "ASSISTANT",
  STUDENT: "STUDENT",
  SUPERADMIN: "SUPERADMIN",
} as const;

export type Role = (typeof Role)[keyof typeof Role];
