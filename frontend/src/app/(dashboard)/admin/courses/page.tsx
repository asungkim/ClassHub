"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useDebounce } from "@/hooks/use-debounce";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/shared/empty-state";
import { InlineError } from "@/components/ui/inline-error";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import type { BranchResponse, CompanyResponse, CourseResponse, CourseStatusFilter } from "@/types/dashboard";
import {
  DASHBOARD_PAGE_SIZE,
  deleteAdminCourse,
  fetchAdminCompanies,
  fetchAdminCourses,
  searchBranches
} from "@/lib/dashboard-api";

const statusOptions: { label: string; value: CourseStatusFilter }[] = [
  { label: "전체", value: "ALL" },
  { label: "활성", value: "ACTIVE" },
  { label: "비활성", value: "INACTIVE" }
];

type Option = { value: string; label: string };

export default function AdminCourseManagementPage() {
  const { canRender, fallback } = useRoleGuard("SUPER_ADMIN");
  if (!canRender) {
    return fallback;
  }
  return <AdminCourseContent />;
}

function AdminCourseContent() {
  const { showToast } = useToast();
  const [teacherIdInput, setTeacherIdInput] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [companyId, setCompanyId] = useState<string>("");
  const [branchId, setBranchId] = useState<string>("");
  const [status, setStatus] = useState<CourseStatusFilter>("ALL");
  const [page, setPage] = useState(0);

  const teacherId = useDebounce(teacherIdInput.trim(), 400);
  const keyword = useDebounce(keywordInput.trim(), 400);

  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [companyOptions, setCompanyOptions] = useState<Option[]>([]);
  const [branchOptions, setBranchOptions] = useState<Option[]>([]);
  const [branchesLoading, setBranchesLoading] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<CourseResponse | null>(null);
  const [deleteConfirmation, setDeleteConfirmation] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const loadCourses = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAdminCourses({
        teacherId: teacherId ? teacherId : undefined,
        branchId: branchId || undefined,
        companyId: companyId || undefined,
        status,
        keyword: keyword || undefined,
        page,
        size: DASHBOARD_PAGE_SIZE
      });
      setCourses(result.items);
      setTotal(result.totalElements);
    } catch (err) {
      const message = err instanceof Error ? err.message : "반 목록을 불러오지 못했습니다.";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [teacherId, branchId, companyId, status, keyword, page]);

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  useEffect(() => {
    const loadCompanies = async () => {
      try {
        const result = await fetchAdminCompanies({ status: "ALL", page: 0, size: 100 });
        const options = result.items
          .filter((company): company is CompanyResponse & { companyId: string } => Boolean(company.companyId))
          .map((company) => ({
            value: company.companyId!,
            label: company.name ?? "이름 없음"
          }));
        setCompanyOptions(options);
      } catch (err) {
        console.error(err);
      }
    };
    void loadCompanies();
  }, []);

  useEffect(() => {
    if (!companyId) {
      setBranchOptions([]);
      setBranchId("");
      return;
    }
    const loadBranches = async () => {
      setBranchesLoading(true);
      try {
        const result = await searchBranches({ companyId, page: 0, size: 100 });
        const options = result.items
          .filter((branch): branch is BranchResponse & { branchId: string } => Boolean(branch.branchId))
          .map((branch) => ({
            value: branch.branchId!,
            label: branch.name ?? "지점"
          }));
        setBranchOptions(options);
      } catch (err) {
        console.error(err);
      } finally {
        setBranchesLoading(false);
      }
    };
    void loadBranches();
  }, [companyId]);

  const totalPages = Math.ceil(total / DASHBOARD_PAGE_SIZE);

  const handleResetFilters = () => {
    setTeacherIdInput("");
    setKeywordInput("");
    setCompanyId("");
    setBranchId("");
    setStatus("ALL");
    setPage(0);
  };

  const formatPeriod = (course: CourseResponse) => {
    if (!course.startDate || !course.endDate) return "-";
    return `${course.startDate} ~ ${course.endDate}`;
  };

  const scheduleSummary = (course: CourseResponse) => {
    if (!course.schedules || course.schedules.length === 0) {
      return "스케줄 정보 없음";
    }
    return course.schedules
      .map((schedule) => `${weekdayLabel(schedule.dayOfWeek)} ${schedule.startTime}~${schedule.endTime}`)
      .join(", ");
  };

  const openDeleteModal = (course: CourseResponse) => {
    setDeleteTarget(course);
    setDeleteConfirmation(false);
  };

  const handleDeleteCourse = async () => {
    if (!deleteTarget?.courseId) return;
    setDeleteLoading(true);
    try {
      await deleteAdminCourse(deleteTarget.courseId);
      showToast("success", "반을 삭제했습니다.");
      setDeleteTarget(null);
      await loadCourses();
    } catch (err) {
      const message = err instanceof Error ? err.message : "삭제에 실패했습니다.";
      showToast("error", message);
    } finally {
      setDeleteLoading(false);
    }
  };

  const toolbarDisabled = loading;

  return (
    <div className="space-y-6 lg:space-y-8">
      <header className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-indigo-500">Course Management · Admin</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">반 관리</h1>
        <p className="mt-2 text-sm text-slate-500">
          Teacher/회사/지점 기준으로 반을 검색하고 필요 시 잘못된 데이터를 하드 삭제할 수 있습니다.
        </p>
      </header>

      <Card title="검색 필터" description="필터를 조합해 전체 반 목록을 빠르게 탐색하세요.">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap">
          <Field label="Teacher ID" className="lg:w-60">
            <Input
              placeholder="UUID 입력"
              value={teacherIdInput}
              onChange={(event) => {
                setTeacherIdInput(event.target.value);
                setPage(0);
              }}
              disabled={toolbarDisabled}
            />
          </Field>
          <Select
            label="학원(Company)"
            value={companyId}
            onChange={(event) => {
              setCompanyId(event.target.value);
              setBranchId("");
              setPage(0);
            }}
            className="lg:w-56"
            disabled={toolbarDisabled}
          >
            <option value="">전체</option>
            {companyOptions.map((company) => (
              <option key={company.value} value={company.value}>
                {company.label}
              </option>
            ))}
          </Select>
          <Select
            label="지점(Branch)"
            value={branchId}
            onChange={(event) => {
              setBranchId(event.target.value);
              setPage(0);
            }}
            className="lg:w-56"
            disabled={!companyId || branchesLoading || toolbarDisabled}
          >
            <option value="">전체</option>
            {branchOptions.map((branch) => (
              <option key={branch.value} value={branch.value}>
                {branch.label}
              </option>
            ))}
          </Select>
          <Select
            label="상태"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as CourseStatusFilter);
              setPage(0);
            }}
            className="lg:w-40"
            disabled={toolbarDisabled}
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
          <Field label="검색어" className="lg:flex-1">
            <Input
              placeholder="반 이름 검색"
              value={keywordInput}
              onChange={(event) => {
                setKeywordInput(event.target.value);
                setPage(0);
              }}
              disabled={toolbarDisabled}
            />
          </Field>
          <div className="flex items-end gap-3">
            <Button variant="secondary" onClick={handleResetFilters} disabled={toolbarDisabled}>
              필터 초기화
            </Button>
          </div>
        </div>
        {error && <InlineError className="mt-4" message={error} />}
      </Card>

      <Card title="반 목록" description="검색 결과가 테이블로 표시됩니다. 필요 시 삭제를 수행하세요.">
        {(!courses.length && !loading) ? (
          <div className="py-10">
            <EmptyState
              message="반 데이터가 없습니다."
              description="검색 조건을 변경하거나 필터를 초기화해 다시 시도하세요."
            />
          </div>
        ) : (
          <div className="-mx-8 mt-2 overflow-x-auto px-8">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>반 정보</TableHead>
                <TableHead>학원 / 지점</TableHead>
                <TableHead>기간</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>스케줄</TableHead>
                <TableHead className="text-right">액션</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {courses.map((course) => (
                <TableRow key={course.courseId}>
                  <TableCell>
                    <div className="space-y-1">
                      <p className="font-semibold text-slate-900">{course.name}</p>
                      {course.description && (
                        <p className="text-sm text-slate-500 line-clamp-2">{course.description}</p>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <p className="text-sm font-medium text-slate-900">
                      {course.companyName ?? "학원 미지정"}
                    </p>
                    <p className="text-xs text-slate-500">{course.branchName ?? "-"}</p>
                  </TableCell>
                  <TableCell className="text-sm text-slate-600">{formatPeriod(course)}</TableCell>
                  <TableCell>
                    <Badge variant={course.active ? "success" : "secondary"}>
                      {course.active ? "활성" : "비활성"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-xs text-slate-500">{scheduleSummary(course)}</TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" className="text-rose-600" onClick={() => openDeleteModal(course)}>
                      삭제
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          </div>
        )}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} disabled={loading} />
        )}
      </Card>

      <DeleteCourseModal
        open={Boolean(deleteTarget)}
        onClose={() => {
          if (!deleteLoading) {
            setDeleteTarget(null);
          }
        }}
        onConfirm={handleDeleteCourse}
        course={deleteTarget}
        confirmChecked={deleteConfirmation}
        onConfirmCheckedChange={setDeleteConfirmation}
        loading={deleteLoading}
      />
    </div>
  );
}

type PaginationProps = {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
};

function Pagination({ currentPage, totalPages, onPageChange, disabled }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="flex items-center justify-between border-t border-slate-100 px-4 py-4 text-sm text-slate-600">
      <p>
        페이지 {currentPage + 1} / {totalPages}
      </p>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          className="h-10 px-4 text-xs"
          onClick={() => onPageChange(Math.max(0, currentPage - 1))}
          disabled={disabled || currentPage === 0}
        >
          이전
        </Button>
        <Button
          variant="secondary"
          className="h-10 px-4 text-xs"
          onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
          disabled={disabled || currentPage >= totalPages - 1}
        >
          다음
        </Button>
      </div>
    </div>
  );
}

type DeleteCourseModalProps = {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  course: CourseResponse | null;
  confirmChecked: boolean;
  onConfirmCheckedChange: (value: boolean) => void;
  loading: boolean;
};

function DeleteCourseModal({
  open,
  onClose,
  onConfirm,
  course,
  confirmChecked,
  onConfirmCheckedChange,
  loading
}: DeleteCourseModalProps) {
  return (
    <Modal open={open} onClose={onClose} title="반을 완전히 삭제하시겠어요?">
      <div className="space-y-6">
        <p className="text-sm text-slate-500">
          삭제된 반은 복구할 수 없습니다. 연결된 학생/클리닉 데이터가 있는 경우에도 모두 제거됩니다.
        </p>
        <div className="rounded-2xl bg-rose-50 p-4 text-sm text-rose-700">
          <p className="font-semibold">삭제 대상</p>
          <p className="mt-1 text-rose-600">
            {course?.name ?? "-"} · {course?.companyName ?? "-"} {course?.branchName ?? ""}
          </p>
        </div>
        <Checkbox
          checked={confirmChecked}
          onChange={(event) => onConfirmCheckedChange(event.target.checked)}
          label="정말로 해당 반을 영구 삭제합니다."
        />
        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={onClose} disabled={loading}>
            취소
          </Button>
          <Button
            variant="secondary"
            onClick={onConfirm}
            disabled={!confirmChecked || loading}
            className="border-rose-200 bg-rose-600 text-white hover:bg-rose-700"
          >
            {loading ? "삭제 중..." : "삭제"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function weekdayLabel(day?: string | null) {
  const map: Record<string, string> = {
    MONDAY: "월",
    TUESDAY: "화",
    WEDNESDAY: "수",
    THURSDAY: "목",
    FRIDAY: "금",
    SATURDAY: "토",
    SUNDAY: "일"
  };
  if (!day) return "?";
  return map[day] ?? day.slice(0, 3);
}

type FieldProps = {
  label: string;
  className?: string;
  children: ReactNode;
};

function Field({ label, className, children }: FieldProps) {
  return (
    <label className={clsx("flex w-full flex-col gap-1 text-sm font-semibold text-slate-700", className)}>
      <span>{label}</span>
      {children}
    </label>
  );
}
