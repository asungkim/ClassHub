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
import {
  DASHBOARD_PAGE_SIZE,
  activateStudentCourseAssignment,
  approveTeacherStudentRequest,
  deactivateStudentCourseAssignment,
  fetchAssignableCourses,
  fetchAssignmentCandidates,
  fetchClinicSlots,
  fetchStudentCourseDetail,
  fetchTeacherAssistants,
  fetchTeacherCourses,
  fetchTeacherStudentDetail,
  fetchTeacherStudents,
  fetchStudentTeacherRequests,
  fetchAssistantCourses,
  createStudentCourseAssignment,
  rejectTeacherStudentRequest,
  updateStudentCourseRecord
} from "@/lib/dashboard-api";
import { formatStudentBirthDate, formatStudentGrade } from "@/utils/student";
import type {
  AssistantAssignmentResponse,
  ClinicSlotResponse,
  CourseResponse,
  CourseWithTeacherResponse,
  StudentCourseDetailResponse,
  StudentSummaryResponse,
  TeacherStudentCourseResponse,
  TeacherStudentDetailResponse,
  StudentTeacherRequestResponse,
  StudentTeacherRequestStatus
} from "@/types/dashboard";

type Role = "TEACHER" | "ASSISTANT";
type TabKey = "students" | "requests" | "assignments";

const requestStatusOptions: { value: StudentTeacherRequestStatus; label: string }[] = [
  { value: "PENDING", label: "대기" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "거절" },
  { value: "CANCELLED", label: "취소" }
];

const requestStatusBadge: Record<StudentTeacherRequestStatus, Parameters<typeof Badge>[0]["variant"]> = {
  PENDING: "secondary",
  APPROVED: "success",
  REJECTED: "destructive",
  CANCELLED: "secondary"
};

export function StudentManagementView({ role }: { role: Role }) {
  const [activeTab, setActiveTab] = useState<TabKey>("students");
  const [refreshKey, setRefreshKey] = useState(0);

  const headerMeta =
    role === "TEACHER"
      ? {
          kicker: "Student Management",
          accent: "text-indigo-500",
          title: "학생 관리",
          description: "학생 정보를 확인하고, 연결 신청을 승인하거나 거절하세요. 또한 반에 학생을 배치할 수 있습니다."
        }
      : {
          kicker: "Assistant · Students",
          accent: "text-emerald-500",
          title: "학생 지원",
          description: "연결된 선생님의 학생 현황을 파악하고, 요청 처리 탭에서 등록 신청을 돕습니다."
        };

  const handleRefresh = useCallback(() => {
    setRefreshKey((prev) => prev + 1);
  }, []);

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
          <TabsTrigger value="assignments">반 배치</TabsTrigger>
        </TabsList>

        <div hidden={activeTab !== "students"}>
          <StudentsTab role={role} refreshKey={refreshKey} />
        </div>
        <div hidden={activeTab !== "requests"}>
          <RequestsTab onRefresh={handleRefresh} />
        </div>
        <div hidden={activeTab !== "assignments"}>
          <BatchAssignmentTab role={role} refreshKey={refreshKey} />
        </div>
      </Tabs>
    </div>
  );
}

type StudentsTabProps = {
  role: Role;
  refreshKey: number;
};

function StudentsTab({ role, refreshKey }: StudentsTabProps) {
  const [courseId, setCourseId] = useState("");
  const [courseOptions, setCourseOptions] = useState<{ value: string; label: string }[]>([]);
  const [courseOptionsLoading, setCourseOptionsLoading] = useState(false);
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [students, setStudents] = useState<StudentSummaryResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [detailStudentId, setDetailStudentId] = useState<string | null>(null);
  const [detail, setDetail] = useState<TeacherStudentDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const { showToast } = useToast();
  const canViewDetail = true;

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
            label: buildCourseLabel(course)
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
            label: buildCourseLabel(course)
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

  const loadStudents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchTeacherStudents({
        courseId: courseId || undefined,
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
  }, [appliedKeyword, courseId, page, showToast]);

  useEffect(() => {
    void loadStudents();
  }, [loadStudents, refreshKey]);

  useEffect(() => {
    if (!detailStudentId || !canViewDetail) {
      return;
    }
    setDetail(null);
    setDetailError(null);
    setDetailLoading(true);
    fetchTeacherStudentDetail(detailStudentId)
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
    if (courseId) {
      return "선택한 반에 배치된 학생이 없습니다.";
    }
    return "연결된 학생이 없습니다. 검색 조건을 바꿔보세요.";
  }, [courseId]);

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
      <Card title="학생 필터" description="반과 검색어로 원하는 학생을 빠르게 찾으세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Select
            label="반"
            className="lg:w-64"
            value={courseId}
            onChange={(event) => {
              setCourseId(event.target.value);
              setPage(0);
            }}
            disabled={loading || courseOptionsLoading}
          >
            <option value="">전체</option>
            {courseOptions.map((option) => (
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
            조교는 학생 기록을 수정할 수 없습니다. (조회 및 상태 확인은 가능합니다.)
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
                  <TableHead>학교/학년</TableHead>
                  <TableHead>나이</TableHead>
                  <TableHead>연락처(학생,학부모)</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {students.map((student, index) => {
                  const memberId = student.memberId ?? `student-${index}`;
                  return (
                    <TableRow
                      key={memberId}
                      className={clsx(canViewDetail ? "cursor-pointer hover:bg-slate-50" : "cursor-default")}
                      onClick={() => {
                        const studentId = student.memberId;
                        if (!canViewDetail || !studentId) return;
                        setDetailStudentId(studentId);
                      }}
                    >
                      <TableCell>
                        <div className="flex flex-col text-sm">
                          <span className="text-base font-semibold text-slate-900">{student.name ?? "-"}</span>
                          <span className="text-xs text-slate-500">{student.email ?? "-"}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm text-slate-600">
                        {student.schoolName ?? "-"}({formatStudentGrade(student.grade)})
                      </TableCell>
                      <TableCell className="text-sm text-slate-600">{student.age ?? "-"}</TableCell>
                      <TableCell>
                        <div className="flex flex-col text-sm text-slate-600">
                          <span>{student.phoneNumber ?? "-"}</span>
                          <span className="text-xs text-slate-400">{student.parentPhone ?? "-"}</span>
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

function RequestsTab({ onRefresh }: { onRefresh: () => void }) {
  const [statuses, setStatuses] = useState<StudentTeacherRequestStatus[]>(["PENDING"]);
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [requests, setRequests] = useState<StudentTeacherRequestResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [confirmState, setConfirmState] = useState<{ type: "APPROVE" | "REJECT"; ids: string[] } | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [detail, setDetail] = useState<StudentTeacherRequestResponse | null>(null);
  const { showToast } = useToast();

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchStudentTeacherRequests({
        statuses: statuses.length > 0 ? statuses : undefined,
        keyword: appliedKeyword.trim() ? appliedKeyword.trim() : undefined,
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
      const message = err instanceof Error ? err.message : "요청 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  }, [appliedKeyword, statuses, page, showToast]);

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

  const toggleStatus = (value: StudentTeacherRequestStatus, checked: boolean) => {
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
    setStatuses(["PENDING"]);
    setKeywordInput("");
    setAppliedKeyword("");
    setPage(0);
  };

  const applyKeywordFilter = () => {
    setAppliedKeyword(keywordInput);
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
          ? confirmState.ids.map((id) => approveTeacherStudentRequest(id))
          : confirmState.ids.map((id) => rejectTeacherStudentRequest(id));
      await Promise.all(actions);
      showToast("success", `${confirmState.ids.length}건의 신청을 ${confirmState.type === "APPROVE" ? "승인" : "거절"}했습니다.`);
      setConfirmState(null);
      setSelectedIds(new Set());
      await loadRequests();
      onRefresh();
    } catch (err) {
      const message = err instanceof Error ? err.message : "신청을 처리하지 못했습니다.";
      showToast("error", message);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card title="신청 필터" description="상태와 학생 이름으로 요청 대상을 좁혀보세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Field label="학생 이름" className="lg:w-60">
            <Input
              placeholder="검색어"
              value={keywordInput}
              onChange={(event) => {
                setKeywordInput(event.target.value);
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  applyKeywordFilter();
                }
              }}
            />
          </Field>
          <div className="flex items-end gap-3">
            <Button variant="secondary" onClick={resetFilters} disabled={loading}>
              초기화
            </Button>
            <Button onClick={applyKeywordFilter} disabled={loading}>
              검색
            </Button>
          </div>
        </div>

        <div className="mt-5 rounded-2xl border border-slate-200/80 bg-slate-50/80 px-4 py-4">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold text-slate-700">상태 필터</p>
              <p className="text-xs text-slate-500">
                현재 선택: {statuses.length ? statuses.map((status) => requestStatusToLabel(status)).join(", ") : "전체"}
              </p>
            </div>
            <Button variant="ghost" onClick={() => setStatuses([])} disabled={loading}>
              전체 선택 해제
            </Button>
          </div>
          <div className="mt-3 flex flex-wrap gap-4">
            {requestStatusOptions.map((option) => (
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
                  <TableHead>학교/학년</TableHead>
                  <TableHead>연락처</TableHead>
                  <TableHead>요청 메시지</TableHead>
                  <TableHead>상태</TableHead>
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
                      <TableCell className="text-sm text-slate-600">
                        {request.student?.schoolName ?? "-"} ({formatStudentGrade(request.student?.grade)})
                      </TableCell>
                      <TableCell className="text-sm text-slate-600">{request.student?.phoneNumber ?? "-"}</TableCell>
                      <TableCell className="max-w-xs truncate text-sm text-slate-600">{request.message ?? "-"}</TableCell>
                      <TableCell>
                        <Badge variant={requestStatusBadge[request.status ?? "PENDING"]}>
                          {requestStatusToLabel(request.status ?? "PENDING")}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right" onClick={(event) => event.stopPropagation()}>
                        {isPending ? (
                          <div className="flex justify-end gap-2">
                            <Button
                              className="h-9 px-3 text-xs"
                              disabled={loading || !request.requestId}
                              onClick={() => openConfirm("APPROVE", request.requestId ? [request.requestId] : undefined)}
                            >
                              승인
                            </Button>
                            <Button
                              className="h-9 px-3 text-xs"
                              variant="ghost"
                              disabled={loading || !request.requestId}
                              onClick={() => openConfirm("REJECT", request.requestId ? [request.requestId] : undefined)}
                            >
                              거절
                            </Button>
                          </div>
                        ) : (
                          <span className="text-sm text-slate-400">-</span>
                        )}
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
        title={confirmState?.type === "APPROVE" ? "학생 신청 승인" : "학생 신청 거절"}
        message={
          confirmState
            ? confirmState.type === "APPROVE"
              ? `선택한 ${confirmState.ids.length}건의 학생 신청을 승인하시겠습니까?\n승인 시 해당 학생들이 학생 목록에 추가되고 반 배치가 가능해집니다.`
              : `선택한 ${confirmState.ids.length}건의 학생 신청을 거절하시겠습니까?\n거절된 신청은 복구할 수 없습니다.`
            : ""
        }
        confirmText={confirmState?.type === "APPROVE" ? "승인" : "거절"}
        cancelText="취소"
      />
    </div>
  );
}

type BatchAssignmentTabProps = {
  role: Role;
  refreshKey: number;
};

function BatchAssignmentTab({ role, refreshKey }: BatchAssignmentTabProps) {
  const [courseOptions, setCourseOptions] = useState<CourseResponse[]>([]);
  const [courseId, setCourseId] = useState("");
  const [courseLoading, setCourseLoading] = useState(false);
  const [courseError, setCourseError] = useState<string | null>(null);
  const [studentKeywordInput, setStudentKeywordInput] = useState("");
  const [studentKeyword, setStudentKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [students, setStudents] = useState<StudentSummaryResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [assigning, setAssigning] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const { showToast } = useToast();

  const loadCourses = useCallback(async () => {
    setCourseLoading(true);
    setCourseError(null);
    try {
      const result = await fetchAssignableCourses({
        page: 0,
        size: 100
      });
      setCourseOptions(result.items);
    } catch (err) {
      const message = err instanceof Error ? err.message : "배치 가능한 반을 불러오지 못했습니다.";
      setCourseError(message);
      showToast("error", message);
    } finally {
      setCourseLoading(false);
    }
  }, [showToast]);

  const loadCandidates = useCallback(async () => {
    if (!courseId) {
      setStudents([]);
      setTotal(0);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAssignmentCandidates({
        courseId,
        keyword: studentKeyword.trim() ? studentKeyword.trim() : undefined,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setStudents(result.items);
      setTotal(result.totalElements);
      setSelectedIds((prev) => {
        const next = new Set<string>();
        result.items.forEach((item) => {
          if (item.memberId && prev.has(item.memberId)) {
            next.add(item.memberId);
          }
        });
        return next;
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : "배치 후보 학생을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setLoading(false);
    }
  }, [courseId, page, studentKeyword, showToast]);

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  useEffect(() => {
    void loadCandidates();
  }, [loadCandidates, refreshKey]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);
  const selectedCount = selectedIds.size;
  const candidateIds = useMemo(
    () => students.map((student) => student.memberId).filter((id): id is string => Boolean(id)),
    [students]
  );
  const allSelected = candidateIds.length > 0 && candidateIds.every((id) => selectedIds.has(id));
  const selectedCourse = courseOptions.find((course) => course.courseId === courseId);

  const applyStudentKeyword = () => {
    setStudentKeyword(studentKeywordInput);
    setPage(0);
  };

  const toggleSelectAll = () => {
    setSelectedIds((prev) => {
      if (allSelected) {
        return new Set();
      }
      return new Set(candidateIds);
    });
  };

  const toggleSelection = (studentId?: string) => {
    if (!studentId) {
      return;
    }
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(studentId)) {
        next.delete(studentId);
      } else {
        next.add(studentId);
      }
      return next;
    });
  };

  const openAssignConfirm = () => {
    if (!courseId) {
      showToast("error", "반을 먼저 선택해 주세요.");
      return;
    }
    if (selectedIds.size === 0) {
      showToast("error", "배치할 학생을 선택해 주세요.");
      return;
    }
    setConfirmOpen(true);
  };

  const executeAssign = async () => {
    setConfirmOpen(false);
    setAssigning(true);
    const failures: string[] = [];
    for (const studentId of selectedIds) {
      try {
        await createStudentCourseAssignment({ courseId, studentId });
      } catch (err) {
        const message = err instanceof Error ? err.message : "배치 처리에 실패했습니다.";
        failures.push(message);
      }
    }
    setAssigning(false);
    if (failures.length === 0) {
      showToast("success", `${selectedIds.size}명의 학생을 배치했습니다.`);
    } else {
      showToast("error", `${failures.length}건의 배치가 실패했습니다.`);
    }
    setSelectedIds(new Set());
    await loadCandidates();
  };

  return (
    <div className="space-y-6">
      <Card title="반 선택" description="학생을 배치할 반을 먼저 선택해주세요.">
        <div className="flex flex-col gap-4">
          <Select
            label="반 목록"
            value={courseId}
            onChange={(event) => {
              setCourseId(event.target.value);
              setPage(0);
              setSelectedIds(new Set());
            }}
            className="lg:w-96"
            disabled={courseLoading}
          >
            <option value="">반을 선택하세요</option>
            {courseOptions.map((course) => (
              <option key={course.courseId ?? course.name} value={course.courseId ?? ""}>
                {buildCourseLabel(course)}
              </option>
            ))}
          </Select>
          {courseError && <InlineError message={courseError} />}
          {role === "ASSISTANT" && (
            <p className="rounded-2xl bg-slate-50 px-4 py-3 text-xs text-slate-500">
              조교는 배치 가능한 반만 선택할 수 있습니다.
            </p>
          )}
        </div>
      </Card>

      {!courseId && (
        <Card>
          <EmptyState
            message="반을 선택해주세요"
            description="상단 드롭다운에서 학생을 배치할 반을 먼저 선택해주세요."
          />
        </Card>
      )}

      {courseId && selectedCourse && (
        <Card>
          <div className="rounded-2xl border-2 border-indigo-200 bg-indigo-50/50 px-6 py-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-indigo-900">선택된 반</p>
                <p className="mt-1 text-base font-bold text-indigo-700">{buildCourseLabel(selectedCourse)}</p>
              </div>
              <Button
                variant="ghost"
                className="h-9 px-4 text-xs"
                onClick={() => {
                  setCourseId("");
                  setSelectedIds(new Set());
                }}
              >
                선택 취소
              </Button>
            </div>
            <p className="mt-3 text-sm text-indigo-600">
              이 반에 학생을 배치하시겠습니까? 아래에서 배치할 학생을 선택해주세요.
            </p>
          </div>
        </Card>
      )}

      {courseId && (
        <Card title="배치 후보 학생" description="선택한 반에 아직 배치되지 않은 학생 목록입니다.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Field label="학생 이름 검색" className="lg:w-60">
            <Input
              placeholder="학생 이름"
              value={studentKeywordInput}
              onChange={(event) => setStudentKeywordInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  applyStudentKeyword();
                }
              }}
              disabled={!courseId || loading}
            />
          </Field>
          <div className="flex items-end gap-3">
            <Button onClick={applyStudentKeyword} disabled={!courseId || loading}>
              검색
            </Button>
          </div>
        </div>

          <div className="mt-4 flex flex-wrap items-center gap-3 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm">
            <div className="text-slate-600">선택 {selectedCount}건</div>
            <Button
              variant="secondary"
              className="h-10 px-4 text-xs"
              onClick={toggleSelectAll}
              disabled={candidateIds.length === 0}
            >
              {allSelected ? "선택 해제" : "전체 선택"}
            </Button>
            <Button
              className="h-10 px-4 text-xs"
              onClick={openAssignConfirm}
              disabled={selectedCount === 0 || assigning}
            >
              {assigning ? "배치 중..." : "선택 배치"}
            </Button>
          </div>

          {loading && <LoadingState message="학생 목록을 불러오는 중입니다." />}
          {!loading && error && <InlineError message={error} className="mt-4" />}
          {!loading && students.length === 0 && (
            <EmptyState message="배치 후보 없음" description="선택한 반에 배치 가능한 학생이 없습니다." />
          )}

          {!loading && students.length > 0 && (
            <div className="mt-4 overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">선택</TableHead>
                    <TableHead>학생</TableHead>
                    <TableHead>학교/학년</TableHead>
                    <TableHead>연락처</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {students.map((student, index) => {
                    const rowKey = student.memberId ?? `candidate-${index}`;
                    const isSelected = student.memberId ? selectedIds.has(student.memberId) : false;
                    return (
                      <TableRow
                        key={rowKey}
                        className="cursor-pointer transition hover:bg-slate-50"
                        onClick={() => toggleSelection(student.memberId)}
                      >
                        <TableCell onClick={(event) => event.stopPropagation()}>
                          <input
                            type="checkbox"
                            className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-400"
                            checked={isSelected}
                            onChange={() => toggleSelection(student.memberId)}
                            disabled={!student.memberId}
                          />
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-col text-sm">
                            <span className="font-semibold text-slate-900">{student.name ?? "-"}</span>
                            <span className="text-xs text-slate-500">{student.email ?? "-"}</span>
                          </div>
                        </TableCell>
                        <TableCell className="text-sm text-slate-600">
                          {student.schoolName ?? "-"} ({formatStudentGrade(student.grade)})
                        </TableCell>
                        <TableCell className="text-sm text-slate-600">
                          {student.phoneNumber ?? "-"}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}

          {totalPages > 1 && (
            <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading || assigning} />
          )}
        </Card>
      )}

      <ConfirmDialog
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={executeAssign}
        isLoading={assigning}
        title="학생 배치 확인"
        message={
          selectedCourse
            ? `${buildCourseLabel(selectedCourse)} 반에 ${selectedCount}명의 학생을 배치하시겠습니까?`
            : `${selectedCount}명의 학생을 배치하시겠습니까?`
        }
        confirmText="배치"
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
  detail: TeacherStudentDetailResponse | null;
  error: string | null;
  onClose: () => void;
  onDetailChange: (detail: TeacherStudentDetailResponse) => void;
  onUpdated?: () => void;
}) {
  const { showToast } = useToast();
  const [expandedCourseIds, setExpandedCourseIds] = useState<Set<string>>(new Set());
  const [recordCache, setRecordCache] = useState<Record<string, StudentCourseDetailResponse>>({});
  const [recordLoadingIds, setRecordLoadingIds] = useState<Set<string>>(new Set());
  const [recordErrorMap, setRecordErrorMap] = useState<Record<string, string>>({});
  const [editingCourseId, setEditingCourseId] = useState<string | null>(null);
  const [assistantInput, setAssistantInput] = useState("");
  const [clinicInput, setClinicInput] = useState("");
  const [notesInput, setNotesInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [assignmentActionId, setAssignmentActionId] = useState<string | null>(null);
  const [assistantOptions, setAssistantOptions] = useState<AssistantAssignmentResponse[]>([]);
  const [assistantLoading, setAssistantLoading] = useState(false);
  const [assistantError, setAssistantError] = useState<string | null>(null);
  const [slotOptions, setSlotOptions] = useState<ClinicSlotResponse[]>([]);
  const [slotLoading, setSlotLoading] = useState(false);
  const [slotError, setSlotError] = useState<string | null>(null);

  const courses = detail?.courses ?? [];
  const summaryLabel = courses.length === 0 ? "배치 전" : `배치된 반 ${courses.length}개`;
  const summaryVariant = "secondary";
  const isTeacher = role === "TEACHER";

  useEffect(() => {
    // 학생이 변경되거나 모달이 닫힐 때 상태 리셋
    setExpandedCourseIds(new Set());
    setEditingCourseId(null);
  }, [detail?.student?.memberId, open]);

  useEffect(() => {
    if (!detail?.student?.memberId) {
      setRecordCache({});
      setRecordErrorMap({});
      setRecordLoadingIds(new Set());
      return;
    }
    setRecordCache({});
    setRecordErrorMap({});
    setRecordLoadingIds(new Set());
  }, [detail?.student?.memberId]);

  useEffect(() => {
    if (!open || !detail || courses.length === 0) {
      return;
    }
    const recordIds = courses
      .map((course) => course.recordId)
      .filter((recordId): recordId is string => Boolean(recordId));
    if (recordIds.length === 0) {
      return;
    }
    const missing = recordIds.filter((recordId) => !recordCache[recordId] && !recordLoadingIds.has(recordId));
    if (missing.length === 0) {
      return;
    }
    setRecordLoadingIds((prev) => {
      const next = new Set(prev);
      missing.forEach((recordId) => next.add(recordId));
      return next;
    });
    Promise.allSettled(missing.map((recordId) => fetchStudentCourseDetail(recordId)))
      .then((results) => {
        setRecordCache((prev) => {
          const next = { ...prev };
          results.forEach((result, index) => {
            if (result.status === "fulfilled") {
              next[missing[index]] = result.value;
            }
          });
          return next;
        });
        setRecordErrorMap((prev) => {
          const next = { ...prev };
          results.forEach((result, index) => {
            const recordId = missing[index];
            if (result.status === "rejected") {
              const message =
                result.reason instanceof Error ? result.reason.message : "수업 기록을 불러오지 못했습니다.";
              next[recordId] = message;
            } else {
              delete next[recordId];
            }
          });
          return next;
        });
      })
      .finally(() => {
        setRecordLoadingIds((prev) => {
          const next = new Set(prev);
          missing.forEach((recordId) => next.delete(recordId));
          return next;
        });
      });
  }, [open, detail, courses, recordCache, recordLoadingIds]);

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
    if (!open || !isTeacher || !editingCourseId) {
      return;
    }
    // editingCourseId가 변경될 때만 실행되도록 의존성 최소화
    const course = courses.find((c) => c.courseId === editingCourseId);
    const recordId = course?.recordId;
    const record = recordId ? recordCache[recordId] : null;
    const branchId = record?.course?.branchId;

    if (!branchId) {
      setSlotOptions([]);
      return;
    }

    setSlotLoading(true);
    setSlotError(null);
    fetchClinicSlots({ branchId })
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, isTeacher, editingCourseId]);

  useEffect(() => {
    if (!editingCourseId) {
      setAssistantInput("");
      setClinicInput("");
      setNotesInput("");
      return;
    }
    const course = courses.find((c) => c.courseId === editingCourseId);
    const recordId = course?.recordId;
    const record = recordId ? recordCache[recordId] : null;
    if (!record) {
      setAssistantInput("");
      setClinicInput("");
      setNotesInput("");
      return;
    }
    setAssistantInput(record.assistantMemberId ?? "");
    setClinicInput(record.defaultClinicSlotId ?? "");
    setNotesInput(record.teacherNotes ?? "");
  }, [editingCourseId, courses, recordCache]);

  const getAssistantLabel = (assistantMemberId?: string | null) => {
    if (!assistantMemberId) {
      return "미지정";
    }
    const assignment = assistantOptions.find((option) => option.assistant?.memberId === assistantMemberId);
    if (!assignment?.assistant) {
      return assistantMemberId;
    }
    const name = assignment.assistant.name ?? "이름 없음";
    const email = assignment.assistant.email ?? "이메일 없음";
    return `${name} (${email})`;
  };

  const getClinicSlotLabel = (slotId?: string | null) => {
    if (!slotId) {
      return "미지정";
    }
    const slot = slotOptions.find((option) => option.slotId === slotId);
    if (!slot) {
      return slotId;
    }
    return formatClinicSlotLabel(slot);
  };

  const resetForm = (courseId: string) => {
    const course = courses.find((c) => c.courseId === courseId);
    const recordId = course?.recordId;
    const record = recordId ? recordCache[recordId] : null;
    setAssistantInput(record?.assistantMemberId ?? "");
    setClinicInput(record?.defaultClinicSlotId ?? "");
    setNotesInput(record?.teacherNotes ?? "");
  };

  const handleSave = async (courseId: string) => {
    const course = courses.find((c) => c.courseId === courseId);
    const recordId = course?.recordId;
    const record = recordId ? recordCache[recordId] : null;
    if (!record?.recordId) {
      return;
    }
    setSaving(true);
    try {
      const nextAssistant = assistantInput.trim();
      const currentAssistant = record.assistantMemberId ?? "";
      const nextSlot = clinicInput.trim();
      const currentSlot = record.defaultClinicSlotId ?? "";
      const nextNotes = notesInput.trim();
      const payload = {
        assistantMemberId:
          nextAssistant && nextAssistant !== currentAssistant ? nextAssistant : undefined,
        defaultClinicSlotId: nextSlot && nextSlot !== currentSlot ? nextSlot : undefined,
        teacherNotes: nextNotes ? notesInput : undefined
      };
      const updated = await updateStudentCourseRecord(record.recordId, payload);
      const updatedRecordId = updated.recordId ?? record.recordId;
      if (updatedRecordId) {
        setRecordCache((prev) => ({
          ...prev,
          [updatedRecordId]: updated
        }));
      }
      onUpdated?.();
      showToast("success", "수업 기록을 수정했습니다.");
      setEditingCourseId(null);
    } catch (err) {
      const message = err instanceof Error ? err.message : "수업 기록을 수정하지 못했습니다.";
      showToast("error", message);
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = (courseId: string) => {
    resetForm(courseId);
    setEditingCourseId(null);
  };

  const toggleCourseExpand = (courseId: string) => {
    setExpandedCourseIds((prev) => {
      const next = new Set(prev);
      if (next.has(courseId)) {
        next.delete(courseId);
        // 접을 때 편집 중이면 편집도 취소
        if (editingCourseId === courseId) {
          setEditingCourseId(null);
        }
      } else {
        next.add(courseId);
      }
      return next;
    });
  };

  const handleToggleAssignment = async (course: TeacherStudentCourseResponse, nextActive: boolean) => {
    if (!course.assignmentId || !detail) {
      return;
    }
    setAssignmentActionId(course.assignmentId);
    try {
      const response = nextActive
        ? await activateStudentCourseAssignment(course.assignmentId)
        : await deactivateStudentCourseAssignment(course.assignmentId);
      const nextCourses = (detail.courses ?? []).map((item) => {
        if (item.courseId !== course.courseId) {
          return item;
        }
        return {
          ...item,
          assignmentActive: response.active
        };
      });
      onDetailChange({ ...detail, courses: nextCourses });
      onUpdated?.();
      showToast("success", nextActive ? "재원 처리했습니다." : "휴원 처리했습니다.");
    } catch (err) {
      const message = err instanceof Error ? err.message : "상태를 변경하지 못했습니다.";
      showToast("error", message);
    } finally {
      setAssignmentActionId(null);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="학생 상세 정보" size="lg">
      {loading && <LoadingState message="학생 정보를 불러오는 중입니다." />}
      {!loading && error && <InlineError message={error} />}
      {!loading && !error && detail && (
        <div className="space-y-6">
          <section>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">배치 상태</p>
            <div className="mt-2 flex flex-wrap items-center gap-3">
              <Badge variant={summaryVariant}>{summaryLabel}</Badge>
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
              <p className="text-sm font-semibold text-slate-900">수강 반 목록</p>
              <p className="mt-1 text-xs text-slate-500">반을 클릭하여 수업 기록과 휴원/재원 처리를 확인하세요.</p>
            </div>
            {courses.length === 0 ? (
              <EmptyState message="배치 전" description="아직 배치된 반이 없습니다." />
            ) : (
              <div className="space-y-3">
                {courses.map((course, index) => {
                  const courseKey = course.courseId ?? `course-${index}`;
                  const courseId = course.courseId ?? "";
                  const isExpanded = courseId && expandedCourseIds.has(courseId);
                  const courseActive = course.active ?? true;
                  const assignmentActive = course.assignmentActive ?? false;
                  const progressState = getCourseProgressState(course.startDate, course.endDate);
                  const ended = progressState === "ENDED";
                  const hasAssignment = Boolean(course.assignmentId);
                  const recordId = course.recordId;
                  const record = recordId ? recordCache[recordId] : null;
                  const isEditing = editingCourseId === courseId;
                  const canEditRecord = isTeacher && Boolean(record?.recordId);
                  const showToggle = !ended && hasAssignment && courseActive;
                  const toggleDisabled = assignmentActionId === course.assignmentId;

                  return (
                    <div key={courseKey} className="rounded-2xl border border-slate-200 bg-white shadow-sm">
                      {/* Header - 클릭 가능한 영역 */}
                      <div
                        className="cursor-pointer px-4 py-4 transition hover:bg-slate-50"
                        onClick={() => courseId && toggleCourseExpand(courseId)}
                      >
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex-1">
                            <div className="flex items-center gap-3">
                              <span className="text-sm font-semibold text-slate-500">#{index + 1}</span>
                              <h4 className="text-base font-semibold text-slate-900">{course.name ?? "-"}</h4>
                            </div>
                            <p className="mt-1 text-sm text-slate-600">{formatCoursePeriod(course.startDate, course.endDate)}</p>
                            <div className="mt-2 flex flex-wrap gap-2">
                              {progressState && <Badge variant="secondary">{courseProgressLabel(progressState)}</Badge>}
                              {!ended && !courseActive && <Badge variant="secondary">보관</Badge>}
                              {!ended && hasAssignment && (
                                <Badge variant={assignmentActive ? "success" : "secondary"}>
                                  {assignmentActive ? "재원" : "휴원"}
                                </Badge>
                              )}
                              {!ended && !hasAssignment && <Badge variant="secondary">배치 전</Badge>}
                            </div>
                          </div>
                          <div className="text-slate-400">
                            {isExpanded ? "▲" : "▼"}
                          </div>
                        </div>
                      </div>

                      {/* Expanded Content */}
                      {isExpanded && (
                        <div className="border-t border-slate-100 px-4 py-4">
                          <div className="space-y-4">
                            {/* 재원/휴원 버튼 */}
                            {showToggle && (
                              <div className="flex gap-2">
                                <Button
                                  variant={assignmentActive ? "primary" : "secondary"}
                                  className="h-10 px-4 text-sm"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    handleToggleAssignment(course, !assignmentActive);
                                  }}
                                  disabled={toggleDisabled}
                                >
                                  {assignmentActive ? "휴원 처리" : "재원 처리"}
                                </Button>
                              </div>
                            )}
                            {!ended && !hasAssignment && (
                              <p className="text-sm text-slate-500">배치 후에만 상태 변경이 가능합니다.</p>
                            )}
                            {ended && <p className="text-sm text-slate-500">종료된 반은 상태 변경이 없습니다.</p>}
                            {!ended && !courseActive && <p className="text-sm text-slate-500">보관된 반은 읽기 전용입니다.</p>}

                            {/* 수업 기록 */}
                            <div className="rounded-2xl border border-slate-200/70 bg-slate-50/50 px-4 py-4">
                              <div className="mb-3 flex items-center justify-between">
                                <p className="text-sm font-semibold text-slate-800">수업 기록</p>
                                {canEditRecord && (
                                  isEditing ? (
                                    <div className="flex gap-2">
                                      <Button
                                        variant="ghost"
                                        className="h-9 px-3 text-xs"
                                        onClick={() => handleCancelEdit(courseId)}
                                        disabled={saving}
                                      >
                                        취소
                                      </Button>
                                      <Button
                                        className="h-9 px-4 text-xs"
                                        onClick={() => void handleSave(courseId)}
                                        disabled={saving}
                                      >
                                        {saving ? "저장 중..." : "저장"}
                                      </Button>
                                    </div>
                                  ) : (
                                    <Button
                                      variant="secondary"
                                      className="h-9 px-4 text-xs"
                                      onClick={() => setEditingCourseId(courseId)}
                                      disabled={saving}
                                    >
                                      수정
                                    </Button>
                                  )
                                )}
                              </div>

                              {!recordId && <p className="text-sm text-slate-500">해당 반에 수업 기록이 없습니다.</p>}
                              {recordId && recordLoadingIds.has(recordId) && (
                                <LoadingState message="수업 기록을 불러오는 중입니다." />
                              )}
                              {recordId && recordErrorMap[recordId] && <InlineError message={recordErrorMap[recordId]} />}

                              {record && !isEditing && (
                                <dl className="space-y-2 text-sm text-slate-600">
                                  <div className="flex justify-between gap-4">
                                    <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">담당 조교</dt>
                                    <dd className="text-right">{getAssistantLabel(record.assistantMemberId)}</dd>
                                  </div>
                                  <div className="flex justify-between gap-4">
                                    <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                                      기본 클리닉 슬롯
                                    </dt>
                                    <dd className="text-right">{getClinicSlotLabel(record.defaultClinicSlotId)}</dd>
                                  </div>
                                  <div>
                                    <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">노트</dt>
                                    <dd className="mt-1 whitespace-pre-wrap text-right">
                                      {record.teacherNotes ?? "등록된 노트가 없습니다."}
                                    </dd>
                                  </div>
                                </dl>
                              )}

                              {record && isEditing && (
                                <div className="space-y-3">
                                  <Field label="담당 조교">
                                    <Select
                                      value={assistantInput}
                                      onChange={(event) => setAssistantInput(event.target.value)}
                                      disabled={saving || assistantLoading || !isTeacher}
                                    >
                                      {!assistantInput && <option value="">미지정</option>}
                                      {assistantOptions.map((option, assistantIndex) => {
                                        const assistantId = option.assistant?.memberId ?? `assistant-${assistantIndex}`;
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
                                      {!clinicInput && <option value="">미지정</option>}
                                      {slotOptions.map((slot, slotIndex) => {
                                        const slotId = slot.slotId ?? `slot-${slotIndex}`;
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
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
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
  request: StudentTeacherRequestResponse | null;
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
          <Badge variant={requestStatusBadge[status]}>{requestStatusToLabel(status)}</Badge>
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
              { label: "학교(학년)", value: `${request.student?.schoolName ?? "-"}(${formatStudentGrade(request.student?.grade)})`.trim() }
            ]}
          />
        </div>
        <section className="rounded-2xl border border-slate-200/60 bg-slate-50 px-4 py-4">
          <p className="text-sm font-semibold text-slate-900">요청 메시지</p>
          <p className="mt-2 text-sm text-slate-600">{request.message ?? "남긴 메시지가 없습니다."}</p>
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

function requestStatusToLabel(status: StudentTeacherRequestStatus) {
  return requestStatusOptions.find((option) => option.value === status)?.label ?? status;
}

function buildCourseLabel(course: CourseResponse | CourseWithTeacherResponse) {
  const academy = [course.companyName, course.branchName].filter(Boolean).join(" ");
  const teacherPart = "teacherName" in course && course.teacherName ? ` · ${course.teacherName} 선생님` : "";
  return `${course.name ?? "이름 없는 반"}${academy ? ` (${academy})` : ""}${teacherPart}`;
}

type CourseProgressState = "UPCOMING" | "ONGOING" | "ENDED";

function getCourseProgressState(startDate?: string | null, endDate?: string | null): CourseProgressState | null {
  const todayKey = toDateKey(new Date());
  if (todayKey === null) {
    return null;
  }
  const startKey = toDateKey(startDate ? new Date(startDate) : null);
  const endKey = toDateKey(endDate ? new Date(endDate) : null);

  if (endKey !== null && endKey < todayKey) {
    return "ENDED";
  }
  if (startKey !== null && startKey > todayKey) {
    return "UPCOMING";
  }
  if (startKey !== null || endKey !== null) {
    return "ONGOING";
  }
  return null;
}

function courseProgressLabel(state: CourseProgressState) {
  if (state === "UPCOMING") {
    return "진행예정";
  }
  if (state === "ONGOING") {
    return "진행중";
  }
  return "종료";
}

function toDateKey(date: Date | null) {
  if (!date || Number.isNaN(date.getTime())) {
    return null;
  }
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}
