import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import type {
  BranchResponse,
  BranchVerifiedStatusRequest,
  CompanyResponse,
  CompanyVerifiedStatusRequest,
  CourseCreateRequest,
  CourseResponse,
  CourseScheduleRequest,
  CourseStatusFilter,
  CourseStatusUpdateRequest,
  CourseUpdateRequest,
  CourseWithTeacherResponse,
  PublicCourseResponse,
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

export async function fetchAdminCourses(params: {
  teacherId?: string;
  branchId?: string;
  companyId?: string;
  status: CourseStatusFilter;
  keyword?: string;
  page: number;
  size?: number;
}): Promise<ListResult<CourseResponse>> {
  const { teacherId, branchId, companyId, status, keyword, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/admin/courses", {
    params: {
      query: {
        teacherId: teacherId || undefined,
        branchId: branchId || undefined,
        companyId: companyId || undefined,
        status,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "반 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as CourseResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function deleteAdminCourse(courseId: string) {
  const response = (await api.DELETE("/api/v1/admin/courses/{courseId}", {
    params: { path: { courseId } }
  })) as { error?: unknown };

  if (response.error) {
    throw new Error(getApiErrorMessage(response.error, "반을 삭제하지 못했습니다."));
  }
}

export async function fetchAssistantCourses(params: {
  teacherId?: string;
  status: CourseStatusFilter;
  keyword?: string;
  page: number;
  size?: number;
}): Promise<ListResult<CourseWithTeacherResponse>> {
  const { teacherId, status, keyword, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/assistants/me/courses", {
    params: {
      query: {
        teacherId: teacherId || undefined,
        status,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "배정된 반을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as CourseWithTeacherResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchPublicCourses(params: {
  companyId?: string;
  branchId?: string;
  teacherId?: string;
  keyword?: string;
  onlyVerified?: boolean;
  page: number;
  size?: number;
}): Promise<ListResult<PublicCourseResponse>> {
  const {
    companyId,
    branchId,
    teacherId,
    keyword,
    onlyVerified = true,
    page,
    size = DASHBOARD_PAGE_SIZE
  } = params;

  const response = await api.GET("/api/v1/courses/public", {
    params: {
      query: {
        companyId: companyId || undefined,
        branchId: branchId || undefined,
        teacherId: teacherId || undefined,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        onlyVerified,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "공개 반을 검색하지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as PublicCourseResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchTeacherCourses(params: {
  status: CourseStatusFilter;
  branchId?: string;
  keyword?: string;
  page: number;
  size?: number;
}): Promise<ListResult<CourseResponse>> {
  const { status, branchId, keyword, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/courses", {
    params: {
      query: {
        status,
        branchId,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "반 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as CourseResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchCourseCalendar(params: { startDate: string; endDate: string }) {
  const response = await api.GET("/api/v1/courses/schedule", {
    params: {
      query: {
        startDate: params.startDate,
        endDate: params.endDate
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "캘린더 데이터를 불러오지 못했습니다."));
  }

  return (response.data.data ?? []) as CourseResponse[];
}

export async function createCourse(payload: CourseCreateRequest) {
  const response = await api.POST("/api/v1/courses", {
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "반을 생성하지 못했습니다."));
  }

  return response.data.data as CourseResponse;
}

export async function updateCourse(courseId: string, payload: CourseUpdateRequest) {
  const response = await api.PATCH("/api/v1/courses/{courseId}", {
    params: { path: { courseId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "반 정보를 수정하지 못했습니다."));
  }

  return response.data.data as CourseResponse;
}

export async function updateCourseStatus(courseId: string, payload: CourseStatusUpdateRequest) {
  const response = await api.PATCH("/api/v1/courses/{courseId}/status", {
    params: { path: { courseId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "반 상태를 변경하지 못했습니다."));
  }

  return response.data.data as CourseResponse;
}
