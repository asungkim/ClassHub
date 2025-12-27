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
  CourseStudentResponse,
  CourseStatusFilter,
  CourseStatusUpdateRequest,
  CourseUpdateRequest,
  CourseWithTeacherResponse,
  AssistantAssignmentResponse,
  AssistantAssignmentPage,
  TeacherSearchResponse,
  ClinicSlotResponse,
  StudentCourseAssignmentCreateRequest,
  StudentCourseDetailResponse,
  StudentCourseAssignmentResponse,
  StudentCourseRecordUpdateRequest,
  StudentMyCourseResponse,
  StudentSummaryResponse,
  TeacherStudentDetailResponse,
  StudentTeacherRequestCreateRequest,
  StudentTeacherRequestResponse,
  StudentTeacherRequestStatus,
  TeacherAssignmentFilter,
  TeacherBranchAssignment,
  TeacherBranchAssignmentCreateRequest,
  TeacherBranchAssignmentStatusUpdateRequest,
  VerificationFilter,
  FeedbackCreateRequest,
  FeedbackResponse,
  FeedbackFilter
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

export async function createFeedback(body: FeedbackCreateRequest): Promise<FeedbackResponse> {
  const response = await api.POST("/api/v1/feedback", { body });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "피드백을 등록하지 못했습니다."));
  }
  return response.data.data as FeedbackResponse;
}

export async function fetchMyFeedbacks(params: {
  status?: FeedbackFilter;
  page: number;
  size?: number;
}): Promise<ListResult<FeedbackResponse>> {
  const { status, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/feedback/me", {
    params: {
      query: {
        status: status === "ALL" ? undefined : status,
        page,
        size
      }
    }
  });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "피드백 목록을 불러오지 못했습니다."));
  }
  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as FeedbackResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchAdminFeedbacks(params: {
  status: FeedbackFilter;
  page: number;
  size?: number;
}): Promise<ListResult<FeedbackResponse>> {
  const { status, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/feedback", {
    params: {
      query: {
        status: status === "ALL" ? undefined : status,
        page,
        size
      }
    }
  });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "피드백 목록을 불러오지 못했습니다."));
  }
  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as FeedbackResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function resolveFeedback(feedbackId: string): Promise<FeedbackResponse> {
  const response = await api.PATCH("/api/v1/feedback/{feedbackId}/resolve", {
    params: { path: { feedbackId } }
  });
  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "피드백 상태를 변경하지 못했습니다."));
  }
  return response.data.data as FeedbackResponse;
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

export async function fetchAssignableCourses(params: {
  branchId?: string;
  keyword?: string;
  page: number;
  size?: number;
}): Promise<ListResult<CourseResponse>> {
  const { branchId, keyword, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/courses/assignable", {
    params: {
      query: {
        branchId: branchId && branchId.trim().length > 0 ? branchId.trim() : undefined,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "배치 가능한 반 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as CourseResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchAssignmentCandidates(params: {
  courseId: string;
  keyword?: string;
  page: number;
  size?: number;
}): Promise<ListResult<StudentSummaryResponse>> {
  const { courseId, keyword, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/courses/{courseId}/assignment-candidates", {
    params: {
      path: { courseId },
      query: {
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "배치 후보 학생을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as StudentSummaryResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchTeacherAssistants(params: {
  status: TeacherAssignmentFilter;
  page: number;
  size?: number;
}): Promise<ListResult<AssistantAssignmentResponse>> {
  const { status, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/teachers/me/assistants", {
    params: { query: { status, page, size } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "조교 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data as AssistantAssignmentPage;
  return {
    items: (pageData?.content ?? []) as AssistantAssignmentResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchClinicSlots(params: {
  branchId?: string;
  teacherId?: string;
  courseId?: string;
}): Promise<ClinicSlotResponse[]> {
  const { branchId, teacherId, courseId } = params;
  const response = await api.GET("/api/v1/clinic-slots", {
    params: {
      query: {
        branchId: branchId || undefined,
        teacherId: teacherId || undefined,
        courseId: courseId || undefined
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "클리닉 슬롯을 불러오지 못했습니다."));
  }

  return (response.data.data ?? []) as ClinicSlotResponse[];
}

export async function fetchCourseStudents(params: {
  courseId: string;
  page: number;
  size?: number;
}): Promise<ListResult<CourseStudentResponse>> {
  const { courseId, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/courses/{courseId}/students", {
    params: {
      path: { courseId },
      query: { page, size }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학생 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as CourseStudentResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchTeacherStudents(params: {
  courseId?: string;
  keyword?: string;
  page: number;
  size?: number;
}): Promise<ListResult<StudentSummaryResponse>> {
  const { courseId, keyword, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/teacher-students", {
    params: {
      query: {
        courseId: courseId && courseId.trim().length > 0 ? courseId.trim() : undefined,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학생 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as StudentSummaryResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchStudentCourseDetail(recordId: string) {
  const response = await api.GET("/api/v1/student-courses/{recordId}", {
    params: { path: { recordId } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학생 상세 정보를 불러오지 못했습니다."));
  }

  return response.data.data as StudentCourseDetailResponse;
}

export async function fetchTeacherStudentDetail(studentId: string) {
  const response = await api.GET("/api/v1/teacher-students/{studentId}", {
    params: { path: { studentId } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학생 상세 정보를 불러오지 못했습니다."));
  }

  return response.data.data as TeacherStudentDetailResponse;
}

export async function updateStudentCourseRecord(recordId: string, payload: StudentCourseRecordUpdateRequest) {
  const response = await api.PATCH("/api/v1/student-courses/{recordId}", {
    params: { path: { recordId } },
    body: payload
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학생 정보를 수정하지 못했습니다."));
  }

  return response.data.data as StudentCourseDetailResponse;
}

export async function createStudentCourseAssignment(body: StudentCourseAssignmentCreateRequest) {
  const response = await api.POST("/api/v1/student-course-assignments", { body });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "학생을 배치하지 못했습니다."));
  }

  return response.data.data as StudentCourseAssignmentResponse;
}

export async function activateStudentCourseAssignment(assignmentId: string) {
  const response = await api.PATCH("/api/v1/student-course-assignments/{assignmentId}/activate", {
    params: { path: { assignmentId } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "재원 처리를 하지 못했습니다."));
  }

  return response.data.data as StudentCourseAssignmentResponse;
}

export async function deactivateStudentCourseAssignment(assignmentId: string) {
  const response = await api.PATCH("/api/v1/student-course-assignments/{assignmentId}/deactivate", {
    params: { path: { assignmentId } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "휴원 처리를 하지 못했습니다."));
  }

  return response.data.data as StudentCourseAssignmentResponse;
}

export async function fetchStudentMyCourses(params: {
  page: number;
  size?: number;
}): Promise<ListResult<StudentMyCourseResponse>> {
  const { page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/students/me/courses", {
    params: {
      query: {
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "내 수업 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as StudentMyCourseResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchTeacherSearch(params: {
  keyword?: string;
  companyId?: string;
  branchId?: string;
  page: number;
  size?: number;
}): Promise<ListResult<TeacherSearchResponse>> {
  const { keyword, companyId, branchId, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/teachers", {
    params: {
      query: {
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        companyId: companyId || undefined,
        branchId: branchId || undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "선생님 목록을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as TeacherSearchResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function fetchStudentTeacherRequests(params: {
  statuses?: StudentTeacherRequestStatus[];
  keyword?: string;
  page: number;
  size?: number;
}): Promise<ListResult<StudentTeacherRequestResponse>> {
  const { statuses, keyword, page, size = DASHBOARD_PAGE_SIZE } = params;
  const response = await api.GET("/api/v1/teacher-student-requests", {
    params: {
      query: {
        status: statuses && statuses.length > 0 ? statuses : undefined,
        keyword: keyword && keyword.trim().length > 0 ? keyword.trim() : undefined,
        page,
        size
      }
    }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "신청 내역을 불러오지 못했습니다."));
  }

  const pageData = response.data.data;
  return {
    items: (pageData?.content ?? []) as StudentTeacherRequestResponse[],
    totalElements: pageData?.totalElements ?? 0
  };
}

export async function createStudentTeacherRequest(body: StudentTeacherRequestCreateRequest) {
  const response = await api.POST("/api/v1/teacher-student-requests", {
    body
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "선생님 요청을 생성하지 못했습니다."));
  }

  return response.data.data as StudentTeacherRequestResponse;
}

export async function cancelStudentTeacherRequest(requestId: string) {
  const response = await api.PATCH("/api/v1/teacher-student-requests/{requestId}/cancel", {
    params: { path: { requestId } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "요청을 취소하지 못했습니다."));
  }

  return response.data.data as StudentTeacherRequestResponse;
}

export async function approveTeacherStudentRequest(requestId: string) {
  const response = await api.PATCH("/api/v1/teacher-student-requests/{requestId}/approve", {
    params: { path: { requestId } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "요청을 승인하지 못했습니다."));
  }

  return response.data.data as StudentTeacherRequestResponse;
}

export async function rejectTeacherStudentRequest(requestId: string) {
  const response = await api.PATCH("/api/v1/teacher-student-requests/{requestId}/reject", {
    params: { path: { requestId } }
  });

  if (response.error || !response.data?.data) {
    throw new Error(getApiErrorMessage(response.error, "요청을 거절하지 못했습니다."));
  }

  return response.data.data as StudentTeacherRequestResponse;
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
