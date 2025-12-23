"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import clsx from "clsx";
import { Card } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/ui/modal";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useToast } from "@/components/ui/toast";
import { useDebounce } from "@/hooks/use-debounce";
import {
  DASHBOARD_PAGE_SIZE,
  approveEnrollmentRequest,
  fetchAssistantCourses,
  fetchClinicSlots,
  fetchTeacherAssistants,
  fetchStudentStudentDetail,
  fetchStudentStudents,
  fetchTeacherCourses,
  fetchTeacherEnrollmentRequests,
  rejectEnrollmentRequest,
  updateStudentCourseRecord
} from "@/lib/dashboard-api";
import { formatStudentBirthDate, formatStudentGrade } from "@/utils/student";
import type {
  EnrollmentStatus,
  AssistantAssignmentResponse,
  ClinicSlotResponse,
  StudentCourseStatusFilter,
  StudentStudentDetailResponse,
  StudentStudentListItemResponse,
  TeacherEnrollmentRequestResponse
} from "@/types/dashboard";

type Role = "TEACHER" | "ASSISTANT";
type TabKey = "students" | "requests";
type CourseOption = { value: string; label: string };

const studentStatusOptions: { value: StudentCourseStatusFilter; label: string }[] = [
  { value: "ACTIVE", label: "재원" },
  { value: "INACTIVE", label: "휴원" },
  { value: "ALL", label: "전체" }
];

const enrollmentStatusOptions: { value: EnrollmentStatus; label: string }[] = [
  { value: "PENDING", label: "대기" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "거절" },
  { value: "CANCELED", label: "취소" }
];

const enrollmentStatusBadge: Record<EnrollmentStatus, Parameters<typeof Badge>[0]["variant"]> = {
  PENDING: "secondary",
  APPROVED: "success",
  REJECTED: "destructive",
  CANCELED: "secondary"
};

export function StudentManagementView({ role }: { role: Role }) {
  const [activeTab, setActiveTab] = useState<TabKey>("students");
  const [courseOptions, setCourseOptions] = useState<CourseOption[]>([]);
  const [courseOptionsLoading, setCourseOptionsLoading] = useState(false);
  const { showToast } = useToast();

  const loadCourseOptions = useCallback(async () => {
    setCourseOptionsLoading(true);
    try {
      if (role === "TEACHER") {
        const result = await fetchTeacherCourses({
          status: "ALL",
          page: 0,
          size: 100
        });
        const options = result.items
          .filter((course) => Boolean(course.courseId))
          .map((course) => ({
            value: course.courseId as string,
            label: buildCourseLabel(course.name, course.companyName, course.branchName)
          }));
        setCourseOptions(options);
      } else {
        const result = await fetchAssistantCourses({
          status: "ALL",
          page: 0,
          size: 100
        });
        const options = result.items
          .filter((course) => Boolean(course.courseId))
          .map((course) => ({
            value: course.courseId as string,
            label: buildCourseLabel(course.name, course.companyName, course.branchName, course.teacherName)
          }));
        setCourseOptions(options);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "반 정보를 불러오지 못했습니다.";
      showToast("error", message);
    } finally {
      setCourseOptionsLoading(false);
    }
  }, [role, showToast]);

  useEffect(() => {
    void loadCourseOptions();
  }, [loadCourseOptions]);

  const headerMeta =
    role === "TEACHER"
      ? {
          kicker: "Teacher · Students",
          accent: "text-indigo-500",
          title: "학생 관리",
          description: "수업에 참여 중인 학생 정보를 확인하고, 새로 들어오는 신청을 승인하거나 거절하세요."
        }
      : {
          kicker: "Assistant · Students",
          accent: "text-emerald-500",
          title: "학생 지원",
          description: "연결된 선생님의 학생 현황을 파악하고, 요청 처리 탭에서 등록 신청을 돕습니다."
        };

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className={clsx("text-xs font-semibold uppercase tracking-wide", headerMeta.accent)}>{headerMeta.kicker}</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">{headerMeta.title}</h1>
        <p className="mt-2 text-sm text-slate-500">{headerMeta.description}</p>
      </header>

      <Tabs
        defaultValue="students"
        value={activeTab}
        onValueChange={(value) => setActiveTab(value as TabKey)}
        className="space-y-4"
      >
        <TabsList>
          <TabsTrigger value="students">학생 목록</TabsTrigger>
          <TabsTrigger value="requests">신청 처리</TabsTrigger>
        </TabsList>

        <div hidden={activeTab !== "students"}>
          <StudentsTab role={role} />
        </div>
        <div hidden={activeTab !== "requests"}>
          <RequestsTab role={role} courseOptions={courseOptions} courseOptionsLoading={courseOptionsLoading} />
        </div>
      </Tabs>
    </div>
  );
}

type StudentsTabProps = {
  role: Role;
};

function StudentsTab({ role }: StudentsTabProps) {
  const [status, setStatus] = useState<StudentCourseStatusFilter>("ACTIVE");
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [students, setStudents] = useState<StudentStudentListItemResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [detailStudentId, setDetailStudentId] = useState<string | null>(null);
  const [detail, setDetail] = useState<StudentStudentDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const { showToast } = useToast();
  const canViewDetail = true;

  const loadStudents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchStudentStudents({
        status,
        keyword: appliedKeyword.trim() ? appliedKeyword.trim() : undefined,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setStudents(result.items);
      setTotal(result.totalElements);
    } catch (err) {
      const message = err instanceof Error ? err.message : "학생 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  }, [appliedKeyword, status, page, showToast]);

  useEffect(() => {
    void loadStudents();
  }, [loadStudents]);

  useEffect(() => {
    if (!detailStudentId || !canViewDetail) {
      return;
    }
    setDetail(null);
    setDetailError(null);
    setDetailLoading(true);
    fetchStudentStudentDetail(detailStudentId)
      .then((response) => {
        setDetail(response);
      })
      .catch((err) => {
        const message = err instanceof Error ? err.message : "학생 상세 정보를 불러오지 못했습니다.";
        setDetailError(message);
        showToast("error", message);
      })
      .finally(() => {
        setDetailLoading(false);
      });
  }, [detailStudentId, canViewDetail, showToast]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);
  const emptyDescription = useMemo(() => {
    switch (status) {
      case "ACTIVE":
        return "현재 수강 중인 학생이 없습니다.";
      case "INACTIVE":
        return "비활성화된 학생 기록이 없습니다.";
      default:
        return "표시할 학생이 없습니다. 필터를 조정해 보세요.";
    }
  }, [status]);

  const applyKeywordFilter = () => {
    setAppliedKeyword(keywordInput);
    setPage(0);
  };

  const closeDetail = () => {
    setDetailStudentId(null);
    setDetail(null);
    setDetailError(null);
  };

  return (
    <div className="space-y-6">
      <Card title="학생 필터" description="상태와 검색어로 원하는 학생을 빠르게 찾으세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Select
            label="상태"
            className="lg:w-40"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as StudentCourseStatusFilter);
              setPage(0);
            }}
            disabled={loading}
          >
            {studentStatusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>

          <Field label="학생 이름 검색" className="lg:flex-1">
            <Input
              placeholder="이름 검색"
              value={keywordInput}
              onChange={(event) => {
                setKeywordInput(event.target.value);
              }}
              disabled={loading}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  applyKeywordFilter();
                }
              }}
            />
          </Field>

          <div className="flex items-end gap-3">
            <Button onClick={applyKeywordFilter} disabled={loading}>
              검색
            </Button>
          </div>
        </div>
        {role === "ASSISTANT" && (
          <p className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-xs text-slate-500">
            조교는 학생 기록을 수정할 수 없으며, 상세 정보는 조회만 가능합니다.
          </p>
        )}
        {error && <InlineError message={error} className="mt-4" />}
      </Card>

      <Card title="학생 목록" description="필터 조건에 맞는 학생들이 최신 순으로 정렬됩니다.">
        {loading && <LoadingState message="학생 목록을 불러오는 중입니다." />}
        {!loading && students.length === 0 && <EmptyState message="학생 없음" description={emptyDescription} />}
        {!loading && students.length > 0 && (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>학생</TableHead>
                  <TableHead>학생/학부모</TableHead>
                  <TableHead>학교/학년</TableHead>
                  <TableHead>나이</TableHead>
                  <TableHead>현재 수강 반</TableHead>
                  <TableHead>상태</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {students.map((student, index) => {
                  const memberId = student.student?.memberId ?? `student-${index}`;
                  const active = student.active ?? false;
                  const activeCourseNames = student.activeCourseNames ?? [];
                  const courseSummary = activeCourseNames.length > 0 ? activeCourseNames.join(", ") : "-";
                  return (
                    <TableRow
                      key={memberId}
                      className={clsx(canViewDetail ? "cursor-pointer hover:bg-slate-50" : "cursor-default")}
                      onClick={() => {
                        const studentId = student.student?.memberId;
                        if (!canViewDetail || !studentId) return;
                        setDetailStudentId(studentId);
                      }}
                    >
                      <TableCell>
                        <div className="flex flex-col text-sm">
                          <span className="text-base font-semibold text-slate-900">{student.student?.name ?? "-"}</span>
                          <span className="text-xs text-slate-500">{student.student?.email ?? "-"}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-col text-sm text-slate-600">
                          <span>{student.student?.phoneNumber ?? "-"}</span>
                          <span className="text-xs text-slate-400">{student.student?.parentPhone ?? "-"}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm text-slate-600">
                        {student.student?.schoolName ?? "-"}({formatStudentGrade(student.student?.grade)})
                      </TableCell>
                      <TableCell className="text-sm text-slate-600">{student.student?.age ?? "-"}</TableCell>
                      <TableCell>
                        <div className="flex flex-col text-sm">
                          <span className="font-semibold text-slate-900">{courseSummary}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={active ? "success" : "secondary"}>{active ? "재원" : "휴원"}</Badge>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>

      <StudentDetailModal
        role={role}
        open={Boolean(detailStudentId && canViewDetail)}
        loading={detailLoading}
        detail={detail}
        error={detailError}
        onClose={closeDetail}
        onDetailChange={(updated) => setDetail(updated)}
        onUpdated={() => {
          void loadStudents();
        }}
      />
    </div>
  );
}

type RequestsTabProps = {
  role: Role;
  courseOptions: CourseOption[];
  courseOptionsLoading: boolean;
};

function RequestsTab({ role, courseOptions, courseOptionsLoading }: RequestsTabProps) {
  const [courseId, setCourseId] = useState("");
  const [statuses, setStatuses] = useState<EnrollmentStatus[]>(["PENDING"]);
  const [studentNameInput, setStudentNameInput] = useState("");
  const studentName = useDebounce(studentNameInput.trim(), 300);
  const [page, setPage] = useState(0);
  const [requests, setRequests] = useState<TeacherEnrollmentRequestResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [confirmState, setConfirmState] = useState<{ type: "APPROVE" | "REJECT"; ids: string[] } | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [detail, setDetail] = useState<TeacherEnrollmentRequestResponse | null>(null);
  const { showToast } = useToast();

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchTeacherEnrollmentRequests({
        courseId: courseId || undefined,
        statuses: statuses.length > 0 ? statuses : undefined,
        studentName: studentName || undefined,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setRequests(result.items);
      setTotal(result.totalElements);
      setSelectedIds((prev) => {
        const next = new Set<string>();
        result.items.forEach((item) => {
          if (item.requestId && prev.has(item.requestId) && item.status === "PENDING") {
            next.add(item.requestId);
          }
        });
        return next;
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : "학생 신청 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  }, [courseId, statuses, studentName, page, showToast]);

  useEffect(() => {
    void loadRequests();
  }, [loadRequests]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);
  const pendingIds = useMemo(
    () =>
      requests
        .filter((request) => request.requestId && request.status === "PENDING")
        .map((request) => request.requestId as string),
    [requests]
  );
  const selectedCount = Array.from(selectedIds).length;
  const allPendingSelected = pendingIds.length > 0 && pendingIds.every((id) => selectedIds.has(id));

  const toggleStatus = (value: EnrollmentStatus, checked: boolean) => {
    setStatuses((prev) => {
      const set = new Set(prev);
      if (checked) {
        set.add(value);
      } else {
        set.delete(value);
      }
      return Array.from(set);
    });
    setPage(0);
  };

  const resetFilters = () => {
    setCourseId("");
    setStatuses(["PENDING"]);
    setStudentNameInput("");
    setPage(0);
  };

  const toggleSelectAll = () => {
    setSelectedIds((prev) => {
      if (allPendingSelected) {
        return new Set();
      }
      return new Set(pendingIds);
    });
  };

  const toggleRowSelection = (requestId?: string, selectable?: boolean) => {
    if (!requestId || !selectable) return;
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(requestId)) {
        next.delete(requestId);
      } else {
        next.add(requestId);
      }
      return next;
    });
  };

  const openConfirm = (type: "APPROVE" | "REJECT", ids?: string[]) => {
    const targetIds = ids ?? Array.from(selectedIds);
    const pendingOnly = targetIds.filter((id) => pendingIds.includes(id));
    if (pendingOnly.length === 0) {
      showToast("error", "처리할 대기 상태의 신청을 선택해 주세요.");
      return;
    }
    setConfirmState({ type, ids: pendingOnly });
  };

  const handleConfirm = async () => {
    if (!confirmState) {
      return;
    }
    setActionLoading(true);
    try {
      const actions =
        confirmState.type === "APPROVE"
          ? confirmState.ids.map((id) => approveEnrollmentRequest(id))
          : confirmState.ids.map((id) => rejectEnrollmentRequest(id));
      await Promise.all(actions);
      showToast("success", `${confirmState.ids.length}건의 신청을 ${confirmState.type === "APPROVE" ? "승인" : "거절"}했습니다.`);
      setConfirmState(null);
      setSelectedIds(new Set());
      await loadRequests();
    } catch (err) {
      const message = err instanceof Error ? err.message : "신청을 처리하지 못했습니다.";
      showToast("error", message);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card title="신청 필터" description="Course/상태/학생 이름으로 요청 대상을 좁혀보세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Select
            label="Course"
            value={courseId}
            onChange={(event) => {
              setCourseId(event.target.value);
              setPage(0);
            }}
            className="lg:w-64"
            disabled={loading || courseOptionsLoading || courseOptions.length === 0}
          >
            <option value="">전체</option>
            {courseOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
          <Field label="학생 이름" className="lg:w-60">
            <Input
              placeholder="검색어"
              value={studentNameInput}
              onChange={(event) => {
                setStudentNameInput(event.target.value);
                setPage(0);
              }}
              disabled={loading}
            />
          </Field>
          <div className="flex items-end gap-3">
            <Button variant="secondary" onClick={resetFilters} disabled={loading}>
              초기화
            </Button>
            <Button onClick={() => void loadRequests()} disabled={loading}>
              새로고침
            </Button>
          </div>
        </div>

        <div className="mt-5 rounded-2xl border border-slate-200/80 bg-slate-50/80 px-4 py-4">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold text-slate-700">상태 필터</p>
              <p className="text-xs text-slate-500">
                현재 선택: {statuses.length ? statuses.map((status) => statusToLabel(status)).join(", ") : "전체"}
              </p>
            </div>
            <Button variant="ghost" onClick={() => setStatuses([])} disabled={loading}>
              전체 선택 해제
            </Button>
          </div>
          <div className="mt-3 flex flex-wrap gap-4">
            {enrollmentStatusOptions.map((option) => (
              <Checkbox
                key={option.value}
                label={option.label}
                checked={statuses.includes(option.value)}
                onChange={(event) => toggleStatus(option.value, event.target.checked)}
                disabled={loading}
              />
            ))}
          </div>
        </div>
        {error && <InlineError message={error} className="mt-4" />}
      </Card>

      <Card title="신청 처리" description="체크박스로 여러 신청을 선택한 뒤 승인/거절을 실행하세요.">
        <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm">
          <div className="text-slate-600">선택 {selectedCount}건</div>
          <Button
            variant="secondary"
            className="h-10 px-4 text-xs"
            onClick={toggleSelectAll}
            disabled={loading || pendingIds.length === 0}
          >
            {allPendingSelected ? "선택 해제" : "대기 전체 선택"}
          </Button>
          <div className="flex gap-2">
            <Button
              className="h-10 px-4 text-xs"
              onClick={() => openConfirm("APPROVE")}
              disabled={selectedCount === 0 || loading}
            >
              선택 승인
            </Button>
            <Button
              variant="ghost"
              className="h-10 px-4 text-xs"
              onClick={() => openConfirm("REJECT")}
              disabled={selectedCount === 0 || loading}
            >
              선택 거절
            </Button>
          </div>
        </div>

        {loading && <LoadingState message="신청 목록을 불러오는 중입니다." />}
        {!loading && requests.length === 0 && (
          <EmptyState message="표시할 신청이 없습니다." description="필터를 조정하거나 새로고침해 보세요." />
        )}

        {!loading && requests.length > 0 && (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-10">선택</TableHead>
                  <TableHead>신청일</TableHead>
                  <TableHead>학생</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead>상태</TableHead>
                  <TableHead>메시지</TableHead>
                  <TableHead className="text-right">동작</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {requests.map((request, index) => {
                  const requestId = request.requestId;
                  const rowKey = requestId ?? `req-${index}`;
                  const isPending = request.status === "PENDING";
                  const isSelected = requestId ? selectedIds.has(requestId) : false;
                  return (
                    <TableRow
                      key={rowKey}
                      className="cursor-pointer transition hover:bg-slate-50"
                      onClick={() => setDetail(request)}
                    >
                      <TableCell onClick={(event) => event.stopPropagation()}>
                        <input
                          type="checkbox"
                          className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-400"
                          checked={isSelected}
                          onChange={() => toggleRowSelection(requestId, isPending)}
                          disabled={!isPending || loading || !requestId}
                        />
                      </TableCell>
                      <TableCell className="text-sm text-slate-600">{formatDateTime(request.createdAt)}</TableCell>
                      <TableCell>
                        <div className="flex flex-col text-sm">
                          <span className="font-semibold text-slate-900">{request.student?.name ?? "-"}</span>
                          <span className="text-xs text-slate-500">{request.student?.email ?? request.student?.phoneNumber ?? "-"}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-col text-sm">
                          <span className="font-semibold text-slate-900">{request.course?.name ?? "-"}</span>
                          <span className="text-xs text-slate-500">
                            {request.course?.companyName ?? ""} {request.course?.branchName ?? ""}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={enrollmentStatusBadge[request.status ?? "PENDING"]}>
                          {statusToLabel(request.status ?? "PENDING")}
                        </Badge>
                      </TableCell>
                      <TableCell className="max-w-xs truncate text-sm text-slate-600">{request.studentMessage ?? "-"}</TableCell>
                      <TableCell className="text-right" onClick={(event) => event.stopPropagation()}>
                        <div className="flex justify-end gap-2">
                          <Button
                            className="h-9 px-3 text-xs"
                            disabled={!isPending || loading || !request.requestId}
                            onClick={() => openConfirm("APPROVE", request.requestId ? [request.requestId] : undefined)}
                          >
                            승인
                          </Button>
                          <Button
                            className="h-9 px-3 text-xs"
                            variant="ghost"
                            disabled={!isPending || loading || !request.requestId}
                            onClick={() => openConfirm("REJECT", request.requestId ? [request.requestId] : undefined)}
                          >
                            거절
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}

        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>

      <RequestDetailModal open={Boolean(detail)} request={detail} onClose={() => setDetail(null)} />

      <ConfirmDialog
        open={Boolean(confirmState)}
        onClose={() => setConfirmState(null)}
        onConfirm={handleConfirm}
        isLoading={actionLoading}
        title={confirmState?.type === "APPROVE" ? "선택 승인" : "선택 거절"}
        message={
          confirmState
            ? `${confirmState.ids.length}건의 신청을 ${confirmState.type === "APPROVE" ? "승인" : "거절"}하시겠습니까?`
            : ""
        }
        confirmText={confirmState?.type === "APPROVE" ? "승인" : "거절"}
        cancelText="취소"
      />
    </div>
  );
}

function StudentDetailModal({
  role,
  open,
  loading,
  detail,
  error,
  onClose,
  onDetailChange,
  onUpdated
}: {
  role: Role;
  open: boolean;
  loading: boolean;
  detail: StudentStudentDetailResponse | null;
  error: string | null;
  onClose: () => void;
  onDetailChange: (detail: StudentStudentDetailResponse) => void;
  onUpdated?: () => void;
}) {
  const { showToast } = useToast();
  const [editing, setEditing] = useState(false);
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [assistantInput, setAssistantInput] = useState("");
  const [clinicInput, setClinicInput] = useState("");
  const [notesInput, setNotesInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [assistantOptions, setAssistantOptions] = useState<AssistantAssignmentResponse[]>([]);
  const [assistantLoading, setAssistantLoading] = useState(false);
  const [assistantError, setAssistantError] = useState<string | null>(null);
  const [slotOptions, setSlotOptions] = useState<ClinicSlotResponse[]>([]);
  const [slotLoading, setSlotLoading] = useState(false);
  const [slotError, setSlotError] = useState<string | null>(null);

  const courses = detail?.courses ?? [];
  const records = detail?.records ?? [];
  const selectedCourse = courses.find((course) => course.courseId === selectedCourseId) ?? null;
  const selectedRecord =
    records.find((record) => record.courseId === selectedCourseId) ?? null;
  const activeCourseExists = courses.some((course) => Boolean(course.active));
  const canEdit = role === "TEACHER" && Boolean(selectedRecord?.recordId);
  const isTeacher = role === "TEACHER";

  useEffect(() => {
    if (!detail || courses.length === 0) {
      setSelectedCourseId(null);
      setAssistantInput("");
      setClinicInput("");
      setNotesInput("");
      setEditing(false);
      return;
    }
    const activeCourse = courses.find((course) => course.active);
    setSelectedCourseId(activeCourse?.courseId ?? courses[0]?.courseId ?? null);
  }, [detail, courses]);

  useEffect(() => {
    if (!open || !isTeacher) {
      return;
    }
    setAssistantLoading(true);
    setAssistantError(null);
    fetchTeacherAssistants({ status: "ACTIVE", page: 0, size: 100 })
      .then((result) => {
        setAssistantOptions(result.items ?? []);
      })
      .catch((err) => {
        const message = err instanceof Error ? err.message : "조교 목록을 불러오지 못했습니다.";
        setAssistantError(message);
      })
      .finally(() => {
        setAssistantLoading(false);
      });
  }, [open, isTeacher]);

  useEffect(() => {
    if (!open || !isTeacher) {
      return;
    }
    if (!selectedCourse?.branchId) {
      setSlotOptions([]);
      return;
    }
    setSlotLoading(true);
    setSlotError(null);
    fetchClinicSlots({ branchId: selectedCourse.branchId })
      .then((result) => {
        setSlotOptions(result);
      })
      .catch((err) => {
        const message = err instanceof Error ? err.message : "클리닉 슬롯을 불러오지 못했습니다.";
        setSlotError(message);
      })
      .finally(() => {
        setSlotLoading(false);
      });
  }, [open, isTeacher, selectedCourse?.branchId]);

  useEffect(() => {
    if (!selectedRecord) {
      setAssistantInput("");
      setClinicInput("");
      setNotesInput("");
      setEditing(false);
      return;
    }
    setAssistantInput(selectedRecord.assistantMemberId ?? "");
    setClinicInput(selectedRecord.defaultClinicSlotId ?? "");
    setNotesInput(selectedRecord.teacherNotes ?? "");
    setEditing(false);
  }, [selectedRecord]);

  const assistantLabel = useMemo(() => {
    if (!selectedRecord?.assistantMemberId) {
      return "미지정";
    }
    const assignment = assistantOptions.find(
      (option) => option.assistant?.memberId === selectedRecord.assistantMemberId
    );
    if (!assignment?.assistant) {
      return selectedRecord.assistantMemberId;
    }
    const name = assignment.assistant.name ?? "이름 없음";
    const email = assignment.assistant.email ?? "이메일 없음";
    return `${name} (${email})`;
  }, [assistantOptions, selectedRecord?.assistantMemberId]);

  const clinicSlotLabel = useMemo(() => {
    if (!selectedRecord?.defaultClinicSlotId) {
      return "미지정";
    }
    const slot = slotOptions.find((option) => option.slotId === selectedRecord.defaultClinicSlotId);
    if (!slot) {
      return selectedRecord.defaultClinicSlotId;
    }
    return formatClinicSlotLabel(slot);
  }, [selectedRecord?.defaultClinicSlotId, slotOptions]);

  const resetForm = () => {
    setAssistantInput(selectedRecord?.assistantMemberId ?? "");
    setClinicInput(selectedRecord?.defaultClinicSlotId ?? "");
    setNotesInput(selectedRecord?.teacherNotes ?? "");
  };

  const handleSave = async () => {
    if (!selectedRecord?.recordId || !detail) {
      return;
    }
    setSaving(true);
    try {
      const payload = {
        assistantMemberId: assistantInput.trim() ? assistantInput.trim() : undefined,
        defaultClinicSlotId: clinicInput.trim() ? clinicInput.trim() : undefined,
        teacherNotes: notesInput.trim() ? notesInput : undefined
      };
      const updated = await updateStudentCourseRecord(selectedRecord.recordId, payload);
      const nextRecords = detail.records?.map((record) => {
        if (record.recordId !== updated.recordId) {
          return record;
        }
        return {
          ...record,
          assistantMemberId: updated.assistantMemberId,
          defaultClinicSlotId: updated.defaultClinicSlotId,
          teacherNotes: updated.teacherNotes,
          active: updated.active
        };
      }) ?? [];
      onDetailChange({ ...detail, records: nextRecords });
      onUpdated?.();
      showToast("success", "수업 기록을 수정했습니다.");
      setEditing(false);
    } catch (err) {
      const message = err instanceof Error ? err.message : "수업 기록을 수정하지 못했습니다.";
      showToast("error", message);
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = () => {
    resetForm();
    setEditing(false);
  };

  return (
    <Modal open={open} onClose={onClose} title="학생 상세 정보" size="lg">
      {loading && <LoadingState message="학생 정보를 불러오는 중입니다." />}
      {!loading && error && <InlineError message={error} />}
      {!loading && !error && detail && (
        <div className="space-y-6">
          <section>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">상태</p>
            <div className="mt-2 flex flex-wrap items-center gap-3">
              <Badge variant={activeCourseExists ? "success" : "secondary"}>
                {activeCourseExists ? "재원" : "휴원"}
              </Badge>
            </div>
          </section>

          <InfoCard
            title="학생 정보"
            items={[
              { label: "이름", value: detail.student?.name ?? "-" },
              { label: "이메일", value: detail.student?.email ?? "-" },
              { label: "연락처", value: detail.student?.phoneNumber ?? "-" },
              {
                label: "학교/학년",
                value: `${detail.student?.schoolName ?? "-"} ${formatStudentGrade(detail.student?.grade)}`.trim()
              },
              { label: "생년월일", value: formatStudentBirthDate(detail.student?.birthDate) },
              { label: "나이", value: detail.student?.age ? `${detail.student?.age}세` : "-" },
              { label: "학부모 연락처", value: detail.student?.parentPhone ?? "-" }
            ]}
          />

          <section className="space-y-4">
            <div>
              <p className="text-sm font-semibold text-slate-900">수강 반</p>
              <p className="mt-1 text-xs text-slate-500">선택한 반의 기록을 아래에서 수정할 수 있습니다.</p>
            </div>
            {courses.length === 0 ? (
              <EmptyState message="수강 반 없음" description="등록된 반 정보가 없습니다." />
            ) : (
              <div className="flex flex-wrap gap-2">
                {courses.map((course, index) => {
                  const courseKey = course.courseId ?? `course-${index}`;
                  const isSelected = course.courseId === selectedCourseId;
                  return (
                    <button
                      key={courseKey}
                      type="button"
                      onClick={() => setSelectedCourseId(course.courseId ?? null)}
                      className={clsx(
                        "flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-semibold transition",
                        isSelected
                          ? "border-indigo-200 bg-indigo-50 text-indigo-700"
                          : "border-slate-200 bg-white text-slate-600 hover:border-indigo-200"
                      )}
                    >
                      <span>{course.name ?? "-"}</span>
                      <Badge variant={course.active ? "success" : "secondary"} className="text-[10px]">
                        {course.active ? "활성" : "비활성"}
                      </Badge>
                    </button>
                  );
                })}
              </div>
            )}

            {selectedCourse && (
              <InfoCard
                title="선택한 반 정보"
                items={[
                  { label: "반 이름", value: selectedCourse.name ?? "-" },
                  { label: "지점", value: selectedCourse.branchName ?? "-" },
                  { label: "회사", value: selectedCourse.companyName ?? "-" },
                  { label: "기간", value: formatCoursePeriod(selectedCourse.startDate, selectedCourse.endDate) }
                ]}
              />
            )}
          </section>

          <section className="rounded-2xl border border-slate-200/60 bg-slate-50 px-4 py-4 text-sm text-slate-600">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-slate-900">수업 기록</p>
              {canEdit && (
                editing ? (
                  <div className="flex gap-2">
                    <Button
                      variant="ghost"
                      className="h-9 px-3 text-xs"
                      onClick={handleCancelEdit}
                      disabled={saving}
                    >
                      취소
                    </Button>
                    <Button className="h-9 px-4 text-xs" onClick={handleSave} disabled={saving}>
                      {saving ? "저장 중..." : "저장"}
                    </Button>
                  </div>
                ) : (
                  <Button
                    variant="secondary"
                    className="h-9 px-4 text-xs"
                    onClick={() => setEditing(true)}
                    disabled={saving}
                  >
                    수정
                  </Button>
                )
              )}
            </div>

            {!selectedRecord && (
              <p className="mt-3 text-sm text-slate-500">선택한 반에 기록이 없습니다.</p>
            )}
            {selectedRecord && !editing && (
              <dl className="mt-3 space-y-2">
                <div className="flex justify-between gap-4">
                  <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">담당 조교</dt>
                  <dd className="text-right">{assistantLabel}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">기본 클리닉 슬롯</dt>
                  <dd className="text-right">{clinicSlotLabel}</dd>
                </div>
                <div>
                  <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">노트</dt>
                  <dd className="mt-1 whitespace-pre-wrap text-right">
                    {selectedRecord.teacherNotes ?? "등록된 노트가 없습니다."}
                  </dd>
                </div>
              </dl>
            )}
            {selectedRecord && editing && (
              <div className="mt-4 space-y-3">
                <Field label="담당 조교">
                  <Select
                    value={assistantInput}
                    onChange={(event) => setAssistantInput(event.target.value)}
                    disabled={saving || assistantLoading || !isTeacher}
                  >
                    <option value="">미지정</option>
                    {assistantOptions.map((option, index) => {
                      const assistantId = option.assistant?.memberId ?? `assistant-${index}`;
                      const name = option.assistant?.name ?? "이름 없음";
                      const email = option.assistant?.email ?? "이메일 없음";
                      return (
                        <option key={assistantId} value={option.assistant?.memberId ?? ""}>
                          {name} ({email})
                        </option>
                      );
                    })}
                  </Select>
                  {assistantError && <InlineError message={assistantError} className="mt-2" />}
                </Field>
                <Field label="기본 클리닉 슬롯">
                  <Select
                    value={clinicInput}
                    onChange={(event) => setClinicInput(event.target.value)}
                    disabled={saving || slotLoading || !isTeacher}
                  >
                    <option value="">미지정</option>
                    {slotOptions.map((slot, index) => {
                      const slotId = slot.slotId ?? `slot-${index}`;
                      return (
                        <option key={slotId} value={slot.slotId ?? ""}>
                          {formatClinicSlotLabel(slot)}
                        </option>
                      );
                    })}
                  </Select>
                  {slotError && <InlineError message={slotError} className="mt-2" />}
                </Field>
                <Field label="Teacher Notes">
                  <textarea
                    className="h-28 w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-900 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-200 disabled:cursor-not-allowed disabled:opacity-50"
                    placeholder="학생에게 남길 메모를 입력하세요."
                    value={notesInput}
                    onChange={(event) => setNotesInput(event.target.value)}
                    disabled={saving}
                  />
                </Field>
              </div>
            )}
          </section>

          <div className="flex justify-end">
            <Button onClick={onClose}>닫기</Button>
          </div>
        </div>
      )}
    </Modal>
  );
}

function RequestDetailModal({
  open,
  request,
  onClose
}: {
  open: boolean;
  request: TeacherEnrollmentRequestResponse | null;
  onClose: () => void;
}) {
  if (!request) {
    return (
      <Modal open={open} onClose={onClose} title="신청 상세" size="lg">
        <LoadingState message="신청 정보를 불러오는 중입니다." />
      </Modal>
    );
  }
  const status = request.status ?? "PENDING";
  return (
    <Modal open={open} onClose={onClose} title="신청 상세" size="lg">
      <div className="space-y-6">
        <section className="flex flex-wrap items-center gap-3">
          <Badge variant={enrollmentStatusBadge[status]}>{statusToLabel(status)}</Badge>
          <span className="text-sm text-slate-500">
            신청일 {formatDateTime(request.createdAt)} / 처리일 {formatDateTime(request.processedAt)}
          </span>
        </section>
        <div className="grid gap-4 md:grid-cols-2">
          <InfoCard
            title="학생 정보"
            items={[
              { label: "이름", value: request.student?.name ?? "-" },
              { label: "이메일", value: request.student?.email ?? "-" },
              { label: "연락처", value: request.student?.phoneNumber ?? "-" },
              { label: "학교/학년", value: `${request.student?.schoolName ?? "-"} ${request.student?.grade ?? ""}`.trim() }
            ]}
          />
          <InfoCard
            title="Course 정보"
            items={[
              { label: "반 이름", value: request.course?.name ?? "-" },
              { label: "지점", value: request.course?.branchName ?? "-" },
              { label: "회사", value: request.course?.companyName ?? "-" },
              { label: "기간", value: formatCoursePeriod(request.course?.startDate, request.course?.endDate) }
            ]}
          />
        </div>
        <section className="rounded-2xl border border-slate-200/60 bg-slate-50 px-4 py-4">
          <p className="text-sm font-semibold text-slate-900">학생 메시지</p>
          <p className="mt-2 text-sm text-slate-600">{request.studentMessage ?? "남긴 메시지가 없습니다."}</p>
        </section>
        <div className="flex justify-end">
          <Button onClick={onClose}>닫기</Button>
        </div>
      </div>
    </Modal>
  );
}

function LoadingState({ message }: { message: string }) {
  return <p className="py-12 text-center text-sm text-slate-500">{message}</p>;
}

function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  disabled
}: {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="mt-6 flex items-center justify-between rounded-2xl border border-slate-100 bg-slate-50/60 px-4 py-3">
      <p className="text-xs font-medium text-slate-500">
        페이지 {currentPage + 1} / {totalPages}
      </p>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          className="h-9 px-4 text-xs"
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
          disabled={disabled || currentPage === 0}
        >
          이전
        </Button>
        <Button
          variant="secondary"
          className="h-9 px-4 text-xs"
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
          disabled={disabled || currentPage >= totalPages - 1}
        >
          다음
        </Button>
      </div>
    </div>
  );
}

function Field({
  label,
  className,
  children
}: {
  label: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <label className={clsx("flex w-full flex-col gap-1 text-sm font-semibold text-slate-700", className)}>
      <span>{label}</span>
      {children}
    </label>
  );
}

function InfoCard({ title, items }: { title: string; items: { label: string; value: ReactNode }[] }) {
  return (
    <div className="rounded-2xl border border-slate-200/70 bg-white px-4 py-4 shadow-sm">
      <p className="text-sm font-semibold text-slate-800">{title}</p>
      <dl className="mt-3 space-y-2 text-sm text-slate-600">
        {items.map((item) => (
          <div key={item.label} className="flex justify-between gap-4">
            <dt className="w-28 text-xs font-semibold uppercase tracking-wide text-slate-500">{item.label}</dt>
            <dd className="flex-1 text-right">{item.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  try {
    return new Intl.DateTimeFormat("ko", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
  } catch {
    return value;
  }
}

function formatCoursePeriod(start?: string | null, end?: string | null) {
  if (!start && !end) {
    return "-";
  }
  const formatDate = (value?: string | null) => {
    if (!value) return "?";
    try {
      return new Intl.DateTimeFormat("ko", { dateStyle: "medium" }).format(new Date(value));
    } catch {
      return value;
    }
  };
  return `${formatDate(start)} ~ ${formatDate(end)}`;
}

const DAY_LABELS: Record<string, string> = {
  MONDAY: "월",
  TUESDAY: "화",
  WEDNESDAY: "수",
  THURSDAY: "목",
  FRIDAY: "금",
  SATURDAY: "토",
  SUNDAY: "일"
};

function formatTimeLabel(value?: string | null) {
  if (!value) return "--:--";
  return value.length >= 5 ? value.slice(0, 5) : value;
}

function formatClinicSlotLabel(slot: ClinicSlotResponse) {
  const day = slot.dayOfWeek ? DAY_LABELS[slot.dayOfWeek] ?? slot.dayOfWeek : "-";
  const startTime = formatTimeLabel(slot.startTime);
  const endTime = formatTimeLabel(slot.endTime);
  return `${day} ${startTime}~${endTime}`;
}

function statusToLabel(status: EnrollmentStatus) {
  return enrollmentStatusOptions.find((option) => option.value === status)?.label ?? status;
}

function buildCourseLabel(
  courseName?: string | null,
  companyName?: string | null,
  branchName?: string | null,
  teacherName?: string | null
) {
  const academy = [companyName, branchName].filter(Boolean).join(" ");
  const teacherPart = teacherName ? ` · ${teacherName} 선생님` : "";
  return `${courseName ?? "이름 없는 반"}${academy ? ` (${academy})` : ""}${teacherPart}`;
}
