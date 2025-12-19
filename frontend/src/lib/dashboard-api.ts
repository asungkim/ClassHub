import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import type {
  BranchResponse,
  BranchVerifiedStatusRequest,
  CompanyResponse,
  CompanyVerifiedStatusRequest,
  TeacherAssignmentFilter,
  TeacherBranchAssignment,
  TeacherBranchAssignmentCreateRequest,
  TeacherBranchAssignmentStatusUpdateRequest,
  VerificationFilter
} from "@/types/dashboard";

type ListResult<T> = {
  items: T[];
  totalElements: number;
};

export const DASHBOARD_PAGE_SIZE = 10;

export async function fetchTeacherBranchAssignments(params: {
  status: TeacherAssignmentFilter;
  page: number;
  size?: number;
}): Promise<ListResult<TeacherBranchAssignment>> {
  const { status, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/teachers/me/branches", {
    params: { query: { status, page, size } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "지점 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as TeacherBranchAssignment[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function createTeacherBranchAssignment(body: TeacherBranchAssignmentCreateRequest) {
  const response = await api.POST("/api/v1/teachers/me/branches", {
    body
  });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "지점 연결을 생성하지 못했습니다."));
  }
  return response.data.data as TeacherBranchAssignment;
}

export async function updateTeacherBranchAssignmentStatus(params: {
  assignmentId: string;
  body: TeacherBranchAssignmentStatusUpdateRequest;
}) {
  const { assignmentId, body } = params;
  const response = await api.PATCH("/api/v1/teachers/me/branches/{assignmentId}", {
    params: { path: { assignmentId } },
    body
  });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "지점 연결 상태를 변경하지 못했습니다."));
  }
  return response.data.data as TeacherBranchAssignment;
}

export async function searchTeacherCompanies(params?: {
  keyword?: string;
  page?: number;
  size?: number;
}): Promise<ListResult<CompanyResponse>> {
  const { keyword, page = 0, size = 20 } = params ?? {};
  const response = await api.GET("/api/v1/companies", {
    params: {
      query: {
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학원 목록을 검색하지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as CompanyResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function searchBranches(params?: {
  companyId?: string;
  keyword?: string;
  page?: number;
  size?: number;
}): Promise<ListResult<BranchResponse>> {
  const { companyId, keyword, page = 0, size = 20 } = params ?? {};
  const response = await api.GET("/api/v1/branches", {
    params: {
      query: {
        companyId,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "지점 목록을 검색하지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as BranchResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchAdminCompanies(params: {
  status: VerificationFilter;
  page: number;
  size?: number;
}): Promise<ListResult<CompanyResponse>> {
  const { status, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/admin/companies", {
    params: {
      query: {
        status: status === "ALL" ? undefined : status,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "회사 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as CompanyResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function updateAdminCompanyStatus(companyId: string, payload: CompanyVerifiedStatusRequest) {
  const response = await api.PATCH("/api/v1/admin/companies/{companyId}/verified-status", {
    params: { path: { companyId } },
    body: payload
  });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "회사 검증 상태를 변경하지 못했습니다."));
  }
  return response.data.data as CompanyResponse;
}

export async function fetchAdminBranches(params: {
  status: VerificationFilter;
  page: number;
  size?: number;
}): Promise<ListResult<BranchResponse>> {
  const { status, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/branches", {
    params: {
      query: {
        status: status === "ALL" ? undefined : status,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "지점 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as BranchResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function updateAdminBranchStatus(branchId: string, payload: BranchVerifiedStatusRequest) {
  const response = await api.PATCH("/api/v1/branches/{branchId}/verified-status", {
    params: { path: { branchId } },
    body: payload
  });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "지점 검증 상태를 변경하지 못했습니다."));
  }
  return response.data.data as BranchResponse;
}
