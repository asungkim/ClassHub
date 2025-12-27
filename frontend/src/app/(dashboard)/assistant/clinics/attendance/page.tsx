"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicSessions } from "@/hooks/clinic/use-clinic-sessions";
import { fetchAssistantCourses, fetchCourseStudents } from "@/lib/dashboard-api";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { TextField } from "@/components/ui/text-field";
import { EmptyState } from "@/components/shared/empty-state";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import type { components } from "@/types/openapi";
import type { CourseStudentResponse, CourseWithTeacherResponse } from "@/types/dashboard";

const ATTENDANCE_LOCK_MINUTES = 10;

const TIME_PLACEHOLDER = "--:--";

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);

const getSessionStartAt = (date?: string, time?: string): Date | null => {
  if (!date || !time) {
    return null;
  }
  const [year, month, day] = date.split("-").map(Number);
  const [hour, minute] = time.split(":").map(Number);
  if (!year || !month || !day || Number.isNaN(hour) || Number.isNaN(minute)) {
    return null;
  }
  return new Date(year, month - 1, day, hour, minute);
};

const isAttendanceLocked = (date?: string, time?: string) => {
  const startAt = getSessionStartAt(date, time);
  if (!startAt) {
    return false;
  }
  const diffMs = startAt.getTime() - Date.now();
  return diffMs <= ATTENDANCE_LOCK_MINUTES * 60 * 1000;
};

type ClinicSessionResponse = components["schemas"]["ClinicSessionResponse"];
type ClinicAttendanceDetailResponse = components["schemas"]["ClinicAttendanceDetailResponse"];
type ClinicAttendanceCreateRequest = components["schemas"]["ClinicAttendanceCreateRequest"];
type ClinicRecordResponse = components["schemas"]["ClinicRecordResponse"];
type ClinicRecordCreateRequest = components["schemas"]["ClinicRecordCreateRequest"];
type ClinicRecordUpdateRequest = components["schemas"]["ClinicRecordUpdateRequest"];

type RecordFormState = {
  title: string;
  content: string;
  homeworkProgress: string;
};

const DEFAULT_RECORD_FORM: RecordFormState = {
  title: "",
  content: "",
  homeworkProgress: ""
};

type AssistantContextOption = {
  key: string;
  teacherId: string;
  teacherName: string;
  branchId: string;
  branchName: string;
  companyName: string;
  courses: CourseWithTeacherResponse[];
};

export default function AssistantClinicAttendancePage() {
  const { canRender, fallback } = useRoleGuard("ASSISTANT");
  const { showToast } = useToast();
  const [contexts, setContexts] = useState<AssistantContextOption[]>([]);
  const [contextLoading, setContextLoading] = useState(false);
  const [contextError, setContextError] = useState<string | null>(null);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);

  const [selectedSession, setSelectedSession] = useState<ClinicSessionResponse | null>(null);
  const [attendances, setAttendances] = useState<ClinicAttendanceDetailResponse[]>([]);
  const [attendanceLoading, setAttendanceLoading] = useState(false);
  const [attendanceError, setAttendanceError] = useState<string | null>(null);

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [studentKeyword, setStudentKeyword] = useState("");
  const [studentList, setStudentList] = useState<CourseStudentResponse[]>([]);
  const [studentLoading, setStudentLoading] = useState(false);
  const [selectedRecordId, setSelectedRecordId] = useState<string | null>(null);

  const [recordModalOpen, setRecordModalOpen] = useState(false);
  const [recordForm, setRecordForm] = useState<RecordFormState>(DEFAULT_RECORD_FORM);
  const [recordLoading, setRecordLoading] = useState(false);
  const [recordError, setRecordError] = useState<string | null>(null);
  const [recordTarget, setRecordTarget] = useState<ClinicAttendanceDetailResponse | null>(null);
  const [recordId, setRecordId] = useState<string | null>(null);
  const [recordDeleteConfirm, setRecordDeleteConfirm] = useState(false);

  const [deleteAttendanceTarget, setDeleteAttendanceTarget] = useState<ClinicAttendanceDetailResponse | null>(null);
  const [isDeletingAttendance, setIsDeletingAttendance] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const todayRange = useMemo(() => {
    const today = new Date();
    return formatDateRange(today, today);
  }, []);

  const loadContexts = useCallback(async () => {
    setContextLoading(true);
    setContextError(null);
    try {
      const result = await fetchAssistantCourses({ status: "ACTIVE", page: 0, size: 200 });
      const map = new Map<string, AssistantContextOption>();
      result.items.forEach((course) => {
        if (!course.teacherId || !course.branchId) {
          return;
        }
        const key = `${course.teacherId}:${course.branchId}`;
        const entry = map.get(key);
        if (entry) {
          entry.courses.push(course);
        } else {
          map.set(key, {
            key,
            teacherId: course.teacherId,
            teacherName: course.teacherName ?? "선생님",
            branchId: course.branchId,
            branchName: course.branchName ?? "지점",
            companyName: course.companyName ?? "학원",
            courses: [course]
          });
        }
      });
      setContexts(Array.from(map.values()));
    } catch (err) {
      const message = err instanceof Error ? err.message : "선생님/지점 목록을 불러오지 못했습니다.";
      setContextError(message);
    } finally {
      setContextLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadContexts();
  }, [loadContexts]);

  useEffect(() => {
    if (contexts.length === 0) {
      setSelectedKey(null);
      return;
    }
    if (selectedKey && contexts.some((context) => context.key === selectedKey)) {
      return;
    }
    setSelectedKey(contexts[0]?.key ?? null);
  }, [contexts, selectedKey]);

  const selectedContext = useMemo(
    () => contexts.find((context) => context.key === selectedKey) ?? null,
    [contexts, selectedKey]
  );
  const selectedCourseName = useMemo(
    () => selectedContext?.courses.find((course) => course.courseId === selectedCourseId)?.name ?? "",
    [selectedContext, selectedCourseId]
  );

  const {
    sessions,
    isLoading: sessionsLoading,
    error: sessionsError,
    refresh: refreshSessions
  } = useClinicSessions(
    {
      dateRange: todayRange,
      branchId: selectedContext?.branchId,
      teacherId: selectedContext?.teacherId
    },
    Boolean(selectedContext)
  );

  const loadAttendances = useCallback(async () => {
    if (!selectedSession?.sessionId) {
      setAttendances([]);
      return;
    }
    setAttendanceLoading(true);
    setAttendanceError(null);
    try {
      const response = await api.GET("/api/v1/clinic-attendances", {
        params: { query: { clinicSessionId: selectedSession.sessionId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "출석 명단을 불러오지 못했습니다."));
      }
      setAttendances((response.data?.data ?? []) as ClinicAttendanceDetailResponse[]);
    } catch (err) {
      const message = err instanceof Error ? err.message : "출석 명단을 불러오지 못했습니다.";
      setAttendanceError(message);
    } finally {
      setAttendanceLoading(false);
    }
  }, [selectedSession?.sessionId]);

  useEffect(() => {
    void loadAttendances();
  }, [loadAttendances]);

  useEffect(() => {
    if (!selectedSession?.sessionId) {
      setSelectedSession(sessions[0] ?? null);
    } else if (!sessions.some((session) => session.sessionId === selectedSession.sessionId)) {
      setSelectedSession(sessions[0] ?? null);
    }
  }, [sessions, selectedSession?.sessionId]);

  const attendanceLocked = useMemo(
    () => isAttendanceLocked(selectedSession?.date, selectedSession?.startTime),
    [selectedSession?.date, selectedSession?.startTime]
  );

  const openAddModal = () => {
    setIsAddModalOpen(true);
    setSelectedRecordId(null);
    setStudentKeyword("");
    setStudentList([]);
  };

  const closeAddModal = () => {
    if (isSubmitting) {
      return;
    }
    setIsAddModalOpen(false);
  };

  useEffect(() => {
    if (!isAddModalOpen) {
      return;
    }
    if (!selectedContext || selectedContext.courses.length === 0) {
      setSelectedCourseId(null);
      return;
    }
    if (selectedCourseId && selectedContext.courses.some((course) => course.courseId === selectedCourseId)) {
      return;
    }
    setSelectedCourseId(selectedContext.courses[0]?.courseId ?? null);
  }, [isAddModalOpen, selectedContext, selectedCourseId]);

  const loadStudents = useCallback(async () => {
    if (!selectedCourseId) {
      setStudentList([]);
      return;
    }
    setStudentLoading(true);
    try {
      const result = await fetchCourseStudents({ courseId: selectedCourseId, page: 0, size: 50 });
      const keyword = studentKeyword.trim();
      const items = result.items ?? [];
      if (!keyword) {
        setStudentList(items);
      } else {
        const filtered = items.filter((item) => {
          const name = item.student?.name ?? "";
          const phone = item.student?.phoneNumber ?? "";
          const parentPhone = item.student?.parentPhone ?? "";
          return [name, phone, parentPhone].some((value) => value.includes(keyword));
        });
        setStudentList(filtered);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "학생 목록을 불러오지 못했습니다.";
      showToast("error", message);
    } finally {
      setStudentLoading(false);
    }
  }, [selectedCourseId, showToast, studentKeyword]);

  useEffect(() => {
    if (!isAddModalOpen || !selectedCourseId) {
      return;
    }
    void loadStudents();
  }, [isAddModalOpen, loadStudents, selectedCourseId]);

  const handleAddAttendance = async () => {
    if (!selectedSession?.sessionId) {
      showToast("error", "세션을 먼저 선택해주세요.");
      return;
    }
    if (!selectedRecordId) {
      showToast("error", "학생을 선택해주세요.");
      return;
    }
    setIsSubmitting(true);
    try {
      const body: ClinicAttendanceCreateRequest = { studentCourseRecordId: selectedRecordId };
      const response = await api.POST("/api/v1/clinic-sessions/{sessionId}/attendances", {
        params: { path: { sessionId: selectedSession.sessionId } },
        body
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "출석을 추가하지 못했습니다."));
      }
      showToast("success", "출석을 추가했습니다.");
      closeAddModal();
      await loadAttendances();
    } catch (err) {
      const message = err instanceof Error ? err.message : "출석을 추가하지 못했습니다.";
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteAttendance = async () => {
    if (!deleteAttendanceTarget?.attendanceId) {
      return;
    }
    setIsDeletingAttendance(true);
    try {
      const response = await api.DELETE("/api/v1/clinic-attendances/{attendanceId}", {
        params: { path: { attendanceId: deleteAttendanceTarget.attendanceId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "출석을 삭제하지 못했습니다."));
      }
      showToast("success", "출석을 삭제했습니다.");
      setDeleteAttendanceTarget(null);
      await loadAttendances();
    } catch (err) {
      const message = err instanceof Error ? err.message : "출석을 삭제하지 못했습니다.";
      showToast("error", message);
    } finally {
      setIsDeletingAttendance(false);
    }
  };

  const openRecordModal = async (attendance: ClinicAttendanceDetailResponse) => {
    setRecordTarget(attendance);
    setRecordForm(DEFAULT_RECORD_FORM);
    setRecordError(null);
    setRecordId(null);
    setRecordModalOpen(true);
    setRecordLoading(true);

    try {
      if (!attendance.attendanceId) {
        throw new Error("출석 정보를 확인할 수 없습니다.");
      }
      const response = await api.GET("/api/v1/clinic-records", {
        params: { query: { clinicAttendanceId: attendance.attendanceId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        if (fetchError.status === 404) {
          setRecordLoading(false);
          return;
        }
        throw new Error(getApiErrorMessage(fetchError, "기록을 불러오지 못했습니다."));
      }
      const record = response.data?.data as ClinicRecordResponse | undefined;
      if (record) {
        setRecordId(record.recordId ?? null);
        setRecordForm({
          title: record.title ?? "",
          content: record.content ?? "",
          homeworkProgress: record.homeworkProgress ?? ""
        });
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "기록을 불러오지 못했습니다.";
      setRecordError(message);
    } finally {
      setRecordLoading(false);
    }
  };

  const closeRecordModal = () => {
    if (isSubmitting) {
      return;
    }
    setRecordModalOpen(false);
    setRecordTarget(null);
  };

  const saveRecord = async () => {
    if (!recordTarget?.attendanceId) {
      return;
    }
    if (!recordForm.title.trim() || !recordForm.content.trim()) {
      setRecordError("제목과 내용을 입력해주세요.");
      return;
    }
    setIsSubmitting(true);
    setRecordError(null);
    try {
      if (recordId) {
        const body: ClinicRecordUpdateRequest = {
          title: recordForm.title,
          content: recordForm.content,
          homeworkProgress: recordForm.homeworkProgress || undefined
        };
        const response = await api.PATCH("/api/v1/clinic-records/{recordId}", {
          params: { path: { recordId } },
          body
        });
        const fetchError = getFetchError(response);
        if (fetchError) {
          throw new Error(getApiErrorMessage(fetchError, "기록을 수정하지 못했습니다."));
        }
        showToast("success", "기록이 수정되었습니다.");
      } else {
        const body: ClinicRecordCreateRequest = {
          clinicAttendanceId: recordTarget.attendanceId,
          title: recordForm.title,
          content: recordForm.content,
          homeworkProgress: recordForm.homeworkProgress || undefined
        };
        const response = await api.POST("/api/v1/clinic-records", { body });
        const fetchError = getFetchError(response);
        if (fetchError) {
          throw new Error(getApiErrorMessage(fetchError, "기록을 생성하지 못했습니다."));
        }
        const data = response.data?.data as ClinicRecordResponse | undefined;
        setRecordId(data?.recordId ?? null);
        showToast("success", "기록이 생성되었습니다.");
      }
      await loadAttendances();
      closeRecordModal();
    } catch (err) {
      const message = err instanceof Error ? err.message : "기록 저장에 실패했습니다.";
      setRecordError(message);
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const deleteRecord = async () => {
    if (!recordId) {
      return;
    }
    setIsSubmitting(true);
    try {
      const response = await api.DELETE("/api/v1/clinic-records/{recordId}", {
        params: { path: { recordId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "기록을 삭제하지 못했습니다."));
      }
      showToast("success", "기록이 삭제되었습니다.");
      setRecordDeleteConfirm(false);
      await loadAttendances();
      closeRecordModal();
    } catch (err) {
      const message = err instanceof Error ? err.message : "기록을 삭제하지 못했습니다.";
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="오늘의 출석부" description="선택한 세션 기준으로 출석 명단을 관리합니다.">
        <div className="space-y-6">
          <div>
            <p className="text-sm font-semibold text-slate-700">선생님/지점 선택</p>
            <p className="text-xs text-slate-500">오늘 세션을 조회할 선생님을 선택하세요.</p>
          </div>

          {contextError && (
            <div className="space-y-3">
              <InlineError message={contextError} />
              <Button variant="secondary" onClick={() => void loadContexts()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {contextLoading && <Skeleton className="h-12 w-full max-w-sm" />}

          {!contextLoading && contexts.length === 0 && (
            <EmptyState message="배정된 반이 없습니다." description="선생님 배정 후 출석부를 관리할 수 있습니다." />
          )}

          {!contextLoading && contexts.length > 0 && (
            <Select
              label="선생님/지점"
              value={selectedKey ?? ""}
              onChange={(event) => setSelectedKey(event.target.value)}
              className="md:w-96"
            >
              {contexts.map((context) => (
                <option key={context.key} value={context.key}>
                  {context.teacherName} · {context.branchName} ({context.companyName})
                </option>
              ))}
            </Select>
          )}
        </div>
      </Card>

      <Card title="오늘 세션" description="오늘 진행되는 세션을 선택하세요.">
        <div className="space-y-4">
          {sessionsError && (
            <div className="space-y-3">
              <InlineError message={sessionsError} />
              <Button variant="secondary" onClick={() => void refreshSessions()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {sessionsLoading && (
            <div className="grid gap-3 md:grid-cols-2">
              <Skeleton className="h-16" />
              <Skeleton className="h-16" />
            </div>
          )}

          {!sessionsLoading && !selectedContext && (
            <EmptyState message="선생님/지점을 먼저 선택해 주세요." description="선택 후 오늘 세션이 표시됩니다." />
          )}

          {!sessionsLoading && selectedContext && sessions.length === 0 && (
            <EmptyState message="오늘 세션이 없습니다." description="오늘 일정이 없는 선생님입니다." />
          )}

          {!sessionsLoading && selectedContext && sessions.length > 0 && (
            <div className="grid gap-3 md:grid-cols-2">
              {sessions.map((session) => {
                const isSelected = session.sessionId === selectedSession?.sessionId;
                return (
                  <button
                    key={session.sessionId}
                    type="button"
                    onClick={() => setSelectedSession(session)}
                    className={
                      isSelected
                        ? "rounded-xl border border-blue-200 bg-blue-50/70 p-4 text-left"
                        : "rounded-xl border border-slate-200 bg-white p-4 text-left hover:border-slate-300"
                    }
                  >
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-semibold text-slate-900">
                        {formatTime(session.startTime)} - {formatTime(session.endTime)}
                      </p>
                      {session.isCanceled && <Badge variant="secondary">취소됨</Badge>}
                    </div>
                    <p className="text-xs text-slate-500">정원 {session.capacity ?? "-"}</p>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </Card>

      <Card title="출석 명단" description="세션별 출석을 확인하고 기록을 남깁니다.">
        <div className="space-y-4">
          {attendanceError && <InlineError message={attendanceError} />}

          {attendanceLoading && <Skeleton className="h-40" />}

          {!attendanceLoading && !selectedSession && (
            <EmptyState message="세션을 먼저 선택해 주세요." description="세션 선택 후 출석 명단이 표시됩니다." />
          )}

          {!attendanceLoading && selectedSession && (
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="text-xs text-slate-500">
                {attendanceLocked
                  ? "세션 시작 10분 전부터 출석 추가/삭제가 제한됩니다."
                  : "출석 추가/삭제 후 기록을 바로 작성할 수 있습니다."}
              </div>
              <Button onClick={openAddModal} disabled={attendanceLocked}>
                학생 추가
              </Button>
            </div>
          )}

          {!attendanceLoading && selectedSession && attendances.length === 0 && !attendanceError && (
            <EmptyState message="출석 명단이 비어 있습니다." description="학생 추가 버튼으로 출석을 등록하세요." />
          )}

          {!attendanceLoading && selectedSession && attendances.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>학생</TableHead>
                  <TableHead>연락처</TableHead>
                  <TableHead>학교</TableHead>
                  <TableHead>학년</TableHead>
                  <TableHead>기록</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {attendances.map((attendance) => (
                  <TableRow key={attendance.attendanceId}>
                    <TableCell>
                      <div className="font-medium text-slate-900">{attendance.studentName ?? "-"}</div>
                      <div className="text-xs text-slate-500">{attendance.studentMemberId ?? ""}</div>
                    </TableCell>
                    <TableCell>{attendance.phoneNumber ?? "-"}</TableCell>
                    <TableCell>{attendance.schoolName ?? "-"}</TableCell>
                    <TableCell>{attendance.grade ?? "-"}</TableCell>
                    <TableCell>
                      {attendance.recordId ? <Badge>기록 있음</Badge> : <Badge variant="secondary">미작성</Badge>}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Button variant="ghost" onClick={() => void openRecordModal(attendance)}>
                          기록
                        </Button>
                        <Button
                          variant="ghost"
                          onClick={() => setDeleteAttendanceTarget(attendance)}
                          disabled={attendanceLocked}
                        >
                          삭제
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </Card>

      <Modal open={isAddModalOpen} onClose={closeAddModal} title="출석 추가" size="md">
        <div className="space-y-4">
          {!selectedContext && (
            <EmptyState message="선생님/지점을 먼저 선택해주세요." description="선택 후 학생을 추가할 수 있습니다." />
          )}

          {selectedContext && selectedContext.courses.length === 0 && (
            <EmptyState message="반 정보가 없습니다." description="반이 등록되어야 출석을 추가할 수 있습니다." />
          )}

          {selectedContext && selectedContext.courses.length > 0 && (
            <div className="space-y-4">
              {selectedContext.courses.length > 1 ? (
                <Select
                  label="반 선택"
                  value={selectedCourseId ?? ""}
                  onChange={(event) => setSelectedCourseId(event.target.value)}
                >
                  {selectedContext.courses.map((course) => (
                    <option key={course.courseId} value={course.courseId}>
                      {course.name ?? "반"}
                    </option>
                  ))}
                </Select>
              ) : (
                <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
                  반: {selectedContext.courses[0]?.name ?? "반"}
                </div>
              )}

              <TextField
                label="학생 검색"
                placeholder="이름/연락처 검색"
                value={studentKeyword}
                onChange={(event) => setStudentKeyword(event.target.value)}
              />

              <div className="flex justify-end">
                <Button variant="secondary" onClick={() => void loadStudents()} disabled={!selectedCourseId}>
                  검색
                </Button>
              </div>

              {studentLoading && <Skeleton className="h-32" />}

              {!studentLoading && selectedCourseId && studentList.length === 0 && (
                <EmptyState message="학생을 찾지 못했습니다." description="검색어를 바꿔 다시 시도하세요." />
              )}

              {!studentLoading && selectedCourseId && studentList.length > 0 && (
                <div className="max-h-64 overflow-y-auto rounded-xl border border-slate-200">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>선택</TableHead>
                        <TableHead>학생</TableHead>
                        <TableHead>연락처</TableHead>
                        <TableHead>상태</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {studentList.map((student) => (
                        <TableRow key={student.recordId} data-state={selectedRecordId === student.recordId ? "selected" : ""}>
                          <TableCell>
                            <input
                              type="radio"
                              checked={selectedRecordId === student.recordId}
                              onChange={() => setSelectedRecordId(student.recordId ?? null)}
                            />
                          </TableCell>
                          <TableCell>
                            <div className="font-medium text-slate-900">{student.student?.name ?? "-"}</div>
                            <div className="text-xs text-slate-500">{selectedCourseName}</div>
                          </TableCell>
                          <TableCell>{student.student?.phoneNumber ?? "-"}</TableCell>
                          <TableCell>{student.assignmentActive ? "수강 중" : "비활성"}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </div>
          )}

          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={closeAddModal} disabled={isSubmitting}>
              취소
            </Button>
            <Button onClick={() => void handleAddAttendance()} disabled={isSubmitting || attendanceLocked}>
              {isSubmitting ? "추가 중..." : "추가"}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal open={recordModalOpen} onClose={closeRecordModal} title="클리닉 기록" size="md">
        <div className="space-y-4">
          {recordLoading && <Skeleton className="h-40" />}
          {!recordLoading && (
            <>
              <TextField
                label="제목"
                value={recordForm.title}
                onChange={(event) => setRecordForm((prev) => ({ ...prev, title: event.target.value }))}
              />
              <label className="flex flex-col gap-2 text-sm text-slate-700">
                내용
                <textarea
                  value={recordForm.content}
                  onChange={(event) => setRecordForm((prev) => ({ ...prev, content: event.target.value }))}
                  className="min-h-[140px] rounded-xl border border-slate-200 px-3 py-2 text-sm"
                />
              </label>
              <TextField
                label="과제 진행"
                value={recordForm.homeworkProgress}
                onChange={(event) => setRecordForm((prev) => ({ ...prev, homeworkProgress: event.target.value }))}
              />
              {recordError && <InlineError message={recordError} />}
              <div className="flex flex-wrap justify-between gap-2">
                {recordId && (
                  <Button variant="ghost" onClick={() => setRecordDeleteConfirm(true)} disabled={isSubmitting}>
                    기록 삭제
                  </Button>
                )}
                <div className="flex gap-2">
                  <Button variant="secondary" onClick={closeRecordModal} disabled={isSubmitting}>
                    취소
                  </Button>
                  <Button onClick={() => void saveRecord()} disabled={isSubmitting}>
                    {isSubmitting ? "저장 중..." : "저장"}
                  </Button>
                </div>
              </div>
            </>
          )}
        </div>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteAttendanceTarget)}
        onClose={() => setDeleteAttendanceTarget(null)}
        onConfirm={() => void handleDeleteAttendance()}
        title="출석을 삭제할까요?"
        message="삭제된 출석은 복구할 수 없습니다."
        confirmText={isDeletingAttendance ? "삭제 중..." : "삭제"}
        isLoading={isDeletingAttendance}
      />

      <ConfirmDialog
        open={recordDeleteConfirm}
        onClose={() => setRecordDeleteConfirm(false)}
        onConfirm={() => void deleteRecord()}
        title="기록을 삭제할까요?"
        message="삭제된 기록은 복구할 수 없습니다."
        confirmText={isSubmitting ? "삭제 중..." : "삭제"}
        isLoading={isSubmitting}
      />
    </div>
  );
}

function formatDateRange(start: Date, end: Date) {
  return `${formatDate(start)},${formatDate(end)}`;
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}
