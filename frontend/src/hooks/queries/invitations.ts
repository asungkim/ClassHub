"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type InvitationResponse = components["schemas"]["InvitationResponse"];
type StudentCandidateResponse = components["schemas"]["StudentCandidateResponse"];
type InvitationStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

type InvitationFilters = {
  status?: InvitationStatus;
};

type StudentCandidateFilters = {
  name?: string;
};

type CreateStudentInvitationsInput = {
  studentProfileIds: string[];
};

// ============================================================
// 조교 초대 목록 조회
// ============================================================
export function useAssistantInvitations(filters: InvitationFilters) {
  const { status } = filters;

  return useQuery({
    queryKey: ["assistant-invitations", filters],
    queryFn: async () => {
      const response = await api.GET("/api/v1/invitations/assistant", {
        params: {
          query: {
            status: status || undefined
          }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return (response.data?.data ?? []) as InvitationResponse[];
    }
  });
}

// ============================================================
// 조교 초대 링크 생성/회전
// ============================================================
export function useCreateAssistantLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const response = await api.POST("/api/v1/invitations/assistant/link", {});

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return response.data?.data as InvitationResponse;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["assistant-invitations"] });
      toast.success("새로운 조교 초대 링크가 생성되었습니다.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "조교 초대 링크 생성에 실패했습니다."));
    }
  });
}

// ============================================================
// 학생 초대 후보 목록 조회
// ============================================================
export function useStudentCandidates(filters: StudentCandidateFilters) {
  const { name } = filters;

  return useQuery({
    queryKey: ["student-candidates", filters],
    queryFn: async () => {
      const response = await api.GET("/api/v1/invitations/student/candidates", {
        params: {
          query: {
            name: name || undefined
          }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return (response.data?.data ?? []) as StudentCandidateResponse[];
    }
  });
}

// ============================================================
// 학생 일괄 초대 생성
// ============================================================
export function useCreateStudentInvitations() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: CreateStudentInvitationsInput) => {
      const response = await api.POST("/api/v1/invitations/student", {
        body: {
          studentProfileIds: input.studentProfileIds
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return (response.data?.data ?? []) as InvitationResponse[];
    },
    onSuccess: (data) => {
      void queryClient.invalidateQueries({ queryKey: ["student-candidates"] });
      void queryClient.invalidateQueries({ queryKey: ["student-invitations"] });
      const count = data.length;
      toast.success(`${count}명의 학생에게 초대가 생성되었습니다.`);
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "학생 초대 생성에 실패했습니다."));
    }
  });
}

// ============================================================
// 학생 초대 목록 조회
// ============================================================
export function useStudentInvitations(filters: InvitationFilters) {
  const { status } = filters;

  return useQuery({
    queryKey: ["student-invitations", filters],
    queryFn: async () => {
      const response = await api.GET("/api/v1/invitations/student", {
        params: {
          query: {
            status: status || undefined
          }
        }
      });

      const fetchError = getFetchError(response);
      if (fetchError) {
        throw fetchError;
      }

      return (response.data?.data ?? []) as InvitationResponse[];
    }
  });
}
