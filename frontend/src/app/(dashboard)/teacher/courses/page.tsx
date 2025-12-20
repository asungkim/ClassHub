"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Controller, useFieldArray, useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import clsx from "clsx";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import {
  DASHBOARD_PAGE_SIZE,
  createCourse,
  fetchCourseCalendar,
  fetchTeacherBranchAssignments,
  fetchTeacherCourses,
  updateCourse,
  updateCourseStatus
} from "@/lib/dashboard-api";
import type { CourseResponse, CourseStatusFilter, TeacherBranchAssignment } from "@/types/dashboard";

type ViewMode = "LIST" | "CALENDAR";
type BranchOption = { value: string; label: string };
type WeekRange = { start: Date; end: Date };
type CourseFormMode = "CREATE" | "EDIT";

const statusTabs: { value: CourseStatusFilter; label: string }[] = [
  { value: "ACTIVE", label: "활성" },
  { value: "INACTIVE", label: "비활성" },
  { value: "ALL", label: "전체" }
];

const viewTabs: { value: ViewMode; label: string }[] = [
  { value: "LIST", label: "목록 뷰" },
  { value: "CALENDAR", label: "캘린더 뷰" }
];

const calendarDays = [
  { key: "MONDAY", label: "월" },
  { key: "TUESDAY", label: "화" },
  { key: "WEDNESDAY", label: "수" },
  { key: "THURSDAY", label: "목" },
  { key: "FRIDAY", label: "금" },
  { key: "SATURDAY", label: "토" },
  { key: "SUNDAY", label: "일" }
] as const;

const calendarStartHour = 6;
const calendarEndHour = 22;
const TIME_SLOT_STEP_MINUTES = 30;
const calendarHourHeight = 56; // px
const courseColorPalette = [
  "bg-rose-400",
  "bg-blue-400",
  "bg-emerald-400",
  "bg-orange-400",
  "bg-purple-400",
  "bg-teal-400",
  "bg-amber-400",
  "bg-indigo-400"
];

type CalendarItem = {
  course: CourseResponse;
  schedule: NonNullable<CourseResponse["schedules"]>[number];
};

const dayOfWeekEnum = z.enum(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"] as const);
const DATE_INPUT_REGEX = /^\d{4}\/\d{2}\/\d{2}$/;

const courseScheduleSchema = z
  .object({
    dayOfWeek: dayOfWeekEnum,
    startTime: z.string().min(1, "시작 시간을 입력하세요."),
    endTime: z.string().min(1, "종료 시간을 입력하세요.")
  })
  .superRefine((value, ctx) => {
    const startMinutes = timeStringToMinutes(value.startTime);
    const endMinutes = timeStringToMinutes(value.endTime);
    const minStart = calendarStartHour * 60;
    const maxStart = calendarEndHour * 60 - TIME_SLOT_STEP_MINUTES;
    const minEnd = minStart + TIME_SLOT_STEP_MINUTES;
    const maxEnd = calendarEndHour * 60;

    if (startMinutes === null || startMinutes < minStart || startMinutes > maxStart) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["startTime"],
        message: "시작 시간은 06:00~21:30 사이에서 선택하세요."
      });
    }

    if (endMinutes === null || endMinutes < minEnd || endMinutes > maxEnd) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["endTime"],
        message: "종료 시간은 06:30~22:00 사이에서 선택하세요."
      });
    }

    if (startMinutes !== null && endMinutes !== null && startMinutes >= endMinutes) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["endTime"],
        message: "종료 시간은 시작 시간 이후여야 합니다."
      });
    }
  });

const courseFormSchema = z
  .object({
    branchId: z.string().min(1, "출강 지점을 선택하세요."),
    name: z.string().min(1, "반 이름을 입력하세요."),
    description: z.string().max(500, "설명은 500자 이하로 작성할 수 있습니다.").optional(),
    startDate: z
      .string()
      .min(1, "반 시작일을 입력하세요.")
      .regex(/^\d{4}\/\d{2}\/\d{2}$/, "YYYY/MM/DD 형식으로 입력하세요."),
    endDate: z
      .string()
      .min(1, "반 종료일을 입력하세요.")
      .regex(/^\d{4}\/\d{2}\/\d{2}$/, "YYYY/MM/DD 형식으로 입력하세요."),
    schedules: z.array(courseScheduleSchema).min(1, "최소 1개의 스케줄이 필요합니다.")
  })
  .superRefine((value, ctx) => {
    if (value.startDate && value.endDate && value.startDate > value.endDate) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["endDate"],
        message: "종료일은 시작일과 같거나 이후여야 합니다."
      });
    }
  });

type CourseFormValues = z.infer<typeof courseFormSchema>;

export default function TeacherCoursesPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");

  if (!canRender) {
    return fallback;
  }

  return <TeacherCourseManagement />;
}

function TeacherCourseManagement() {
  const { showToast } = useToast();
  const [viewMode, setViewMode] = useState<ViewMode>("LIST");
  const [status, setStatus] = useState<CourseStatusFilter>("ACTIVE");
  const [branchFilter, setBranchFilter] = useState<string>("ALL");
  const [branchOptions, setBranchOptions] = useState<BranchOption[]>([]);
  const [branchError, setBranchError] = useState<string | null>(null);

  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");

  const [page, setPage] = useState(0);
  const [courses, setCourses] = useState<CourseResponse[]>([]);
  const [totalCourses, setTotalCourses] = useState(0);
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);

  const [calendarRange, setCalendarRange] = useState<WeekRange>(() => getCurrentWeekRange());
  const [calendarCourses, setCalendarCourses] = useState<CourseResponse[]>([]);
  const [calendarLoading, setCalendarLoading] = useState(false);
  const [calendarError, setCalendarError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<CourseFormMode>("CREATE");
  const [editingCourse, setEditingCourse] = useState<CourseResponse | null>(null);
  const [mutatingCourseId, setMutatingCourseId] = useState<string | null>(null);

  // Debounce keyword to avoid chattiness
  useEffect(() => {
    const handle = window.setTimeout(() => {
      setKeyword(keywordInput.trim());
      setPage(0);
    }, 400);
    return () => window.clearTimeout(handle);
  }, [keywordInput]);

  const loadBranches = useCallback(async () => {
    try {
      const result = await fetchTeacherBranchAssignments({ status: "ACTIVE", page: 0, size: 50 });
      const options = result.items
        .filter((assignment): assignment is TeacherBranchAssignment & { branchId: string } =>
          Boolean(assignment.branchId)
        )
        .map((assignment) => ({
          value: assignment.branchId!,
          label: `${assignment.companyName ?? "학원"} ${assignment.branchName ?? ""}`.trim()
        }));
      setBranchOptions(options);
      setBranchError(null);
    } catch (err) {
      const message = err instanceof Error ? err.message : "지점 목록을 불러오지 못했습니다.";
      setBranchError(message);
    }
  }, []);

  useEffect(() => {
    void loadBranches();
  }, [loadBranches]);

  const loadCourses = useCallback(
    async (targetFilters?: { status?: CourseStatusFilter; branch?: string; keyword?: string; page?: number }) => {
      setListLoading(true);
      setListError(null);
      try {
        const result = await fetchTeacherCourses({
          status: targetFilters?.status ?? status,
          branchId: targetFilters?.branch ?? (branchFilter === "ALL" ? undefined : branchFilter),
          keyword: targetFilters?.keyword ?? keyword,
          page: targetFilters?.page ?? page
        });
        setCourses(result.items);
        setTotalCourses(result.totalElements);
      } catch (err) {
        const message = err instanceof Error ? err.message : "반 목록을 불러오지 못했습니다.";
        setListError(message);
      } finally {
        setListLoading(false);
      }
    },
    [status, branchFilter, keyword, page]
  );

  useEffect(() => {
    void loadCourses();
  }, [loadCourses]);

  const loadCalendar = useCallback(
    async (range: WeekRange) => {
      setCalendarLoading(true);
      setCalendarError(null);
      try {
        const courses = await fetchCourseCalendar({
          startDate: formatDateParam(range.start),
          endDate: formatDateParam(range.end)
        });
        setCalendarCourses(courses);
      } catch (err) {
        const message = err instanceof Error ? err.message : "캘린더 정보를 불러오지 못했습니다.";
        setCalendarError(message);
      } finally {
        setCalendarLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadCalendar(calendarRange);
  }, [calendarRange, loadCalendar]);

  const totalPages = Math.max(1, Math.ceil(totalCourses / DASHBOARD_PAGE_SIZE));

  const activeCalendarCourses = useMemo(
    () => calendarCourses.filter((course) => course.active !== false),
    [calendarCourses]
  );

  const courseColorMap = useMemo(() => {
    const map: Record<string, string> = {};
    activeCalendarCourses.forEach((course, index) => {
      const colorKey = getCourseColorKey(course);
      const paletteIndex = index % courseColorPalette.length;
      map[colorKey] = courseColorPalette[paletteIndex];
    });
    return map;
  }, [activeCalendarCourses]);

  const calendarItems = useMemo<Record<string, CalendarItem[]>>(() => {
    const base: Record<string, CalendarItem[]> = {};
    for (const day of calendarDays) {
      base[day.key] = [];
    }
    activeCalendarCourses.forEach((course) => {
      course.schedules?.forEach((schedule) => {
        if (!schedule?.dayOfWeek || !base[schedule.dayOfWeek]) {
          return;
        }
        base[schedule.dayOfWeek].push({ course, schedule });
      });
    });
    Object.values(base).forEach((items) => {
      items.sort((a, b) => (a.schedule.startTime ?? "").localeCompare(b.schedule.startTime ?? ""));
    });
    return base;
  }, [activeCalendarCourses]);

  const branchSelectOptions: BranchOption[] = useMemo(
    () => [{ value: "ALL", label: "전체 지점" }, ...branchOptions],
    [branchOptions]
  );

  const calendarWeekLabel = `${formatDisplayDate(calendarRange.start)} ~ ${formatDisplayDate(calendarRange.end)}`;

  const handleWeekChange = (offset: number) => {
    setCalendarRange((prev) => {
      const reference = addDays(prev.start, offset * 7);
      return getWeekRangeFromDate(reference);
    });
  };

  const handleOpenCreate = () => {
    setFormMode("CREATE");
    setEditingCourse(null);
    setIsFormOpen(true);
  };

  const handleEditCourse = (course: CourseResponse) => {
    setFormMode("EDIT");
    setEditingCourse(course);
    setIsFormOpen(true);
  };

  const handleCloseForm = useCallback(() => {
    setIsFormOpen(false);
    setEditingCourse(null);
  }, []);

  const handleCourseStatusChange = useCallback(
    async (course: CourseResponse, enabled: boolean) => {
      if (!course.courseId) {
        return;
      }
      setMutatingCourseId(course.courseId);
      setCourses((prev) =>
        prev.map((item) => (item.courseId === course.courseId ? { ...item, active: enabled } : item))
      );
      try {
        await updateCourseStatus(course.courseId, { enabled });
        showToast("success", enabled ? "반을 활성화했습니다." : "반을 비활성화했습니다.");
        await Promise.all([loadCourses(), loadCalendar(calendarRange)]);
      } catch (error) {
        const message = error instanceof Error ? error.message : "반 상태를 변경하지 못했습니다.";
        showToast("error", message);
        await loadCourses();
      } finally {
        setMutatingCourseId(null);
      }
    },
    [calendarRange, loadCalendar, loadCourses, showToast]
  );

  const handleCourseFormSubmit = useCallback(
    async (values: CourseFormValues) => {
      const normalizedDescription = values.description?.trim() ?? "";
      const descriptionPayload = normalizedDescription.length > 0 ? normalizedDescription : undefined;
      const startDatePayload = normalizeDateInput(values.startDate);
      const endDatePayload = normalizeDateInput(values.endDate);
      const schedulePayload = values.schedules.map((schedule) => ({
        dayOfWeek: schedule.dayOfWeek,
        startTime: schedule.startTime,
        endTime: schedule.endTime
      }));

      try {
        if (formMode === "CREATE") {
          await createCourse({
            branchId: values.branchId,
            name: values.name.trim(),
            description: descriptionPayload,
            startDate: startDatePayload,
            endDate: endDatePayload,
            schedules: schedulePayload
          });
          showToast("success", "새 반을 생성했습니다.");
          setPage(0);
          await loadCourses({ page: 0 });
        } else if (formMode === "EDIT" && editingCourse?.courseId) {
          await updateCourse(editingCourse.courseId, {
            name: values.name.trim(),
            description: descriptionPayload,
            startDate: startDatePayload,
            endDate: endDatePayload,
            schedules: schedulePayload
          });
          showToast("success", "반 정보를 수정했습니다.");
          await loadCourses();
        } else {
          throw new Error("수정할 반 정보를 찾을 수 없습니다.");
        }

        await loadCalendar(calendarRange);
        handleCloseForm();
      } catch (error) {
        const message = error instanceof Error ? error.message : "반 정보를 저장하지 못했습니다.";
        showToast("error", message);
        throw new Error(message);
      }
    },
    [calendarRange, editingCourse, formMode, handleCloseForm, loadCalendar, loadCourses, showToast]
  );

  const canCreateCourse = branchOptions.length > 0 && !branchError;
  const createButtonTitle = canCreateCourse
    ? undefined
    : branchError
      ? "지점 정보를 불러오지 못했습니다. 새로 고침 후 다시 시도하세요."
      : "출강 지점을 연결하면 반을 생성할 수 있습니다.";

  return (
    <div className="space-y-6 lg:space-y-8">
      <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-blue-500">Course Management</p>
            <h1 className="mt-2 text-3xl font-bold text-slate-900">반 관리</h1>
            <p className="mt-2 text-sm text-slate-500">
              목록/캘린더에서 반을 확인하고 곧바로 생성·수정·비활성화를 처리할 수 있습니다.
            </p>
          </div>
          <div className="flex w-full flex-col items-stretch gap-2 md:w-auto">
            <Button
              className="w-full md:w-auto"
              onClick={handleOpenCreate}
              disabled={!canCreateCourse}
              title={createButtonTitle}
            >
              반 생성
            </Button>
            {!canCreateCourse ? (
              <p className="text-center text-xs text-slate-500 md:text-right">
                출강 지점을 연결하면 반을 등록할 수 있습니다.
              </p>
            ) : null}
          </div>
        </div>
      </section>

      <Card
        title="반 조회"
        description="상태·지점·검색어로 필터링하고 필요하면 캘린더 뷰로 전환하세요."
        actions={
          <Tabs value={viewMode} onValueChange={(value) => setViewMode(value as ViewMode)} defaultValue={viewTabs[0].value}>
            <TabsList>
              {viewTabs.map((tab) => (
                <TabsTrigger key={tab.value} value={tab.value}>
                  {tab.label}
                </TabsTrigger>
              ))}
            </TabsList>
          </Tabs>
        }
      >
        {viewMode === "LIST" ? (
          <CourseListSection
            status={status}
            onStatusChange={(next) => {
              setStatus(next);
              setPage(0);
            }}
            branchFilter={branchFilter}
            onBranchChange={(value) => {
              setBranchFilter(value);
              setPage(0);
            }}
            branchOptions={branchSelectOptions}
            branchError={branchError}
            keywordInput={keywordInput}
            onKeywordInputChange={setKeywordInput}
            courses={courses}
            loading={listLoading}
            error={listError}
            totalPages={totalPages}
            currentPage={page}
            onPageChange={setPage}
            onEditCourse={handleEditCourse}
            onToggleCourse={handleCourseStatusChange}
            mutatingCourseId={mutatingCourseId}
          />
        ) : (
          <CourseCalendarSection
            rangeLabel={calendarWeekLabel}
            onPrevWeek={() => handleWeekChange(-1)}
            onNextWeek={() => handleWeekChange(1)}
            onResetWeek={() => setCalendarRange(getCurrentWeekRange())}
            calendarItems={calendarItems}
            courseColorMap={courseColorMap}
            rangeStart={calendarRange.start}
            loading={calendarLoading}
            error={calendarError}
            onCourseSelect={handleEditCourse}
          />
        )}
      </Card>

      <CourseFormModal
        open={isFormOpen}
        mode={formMode}
        branchOptions={branchOptions}
        initialCourse={editingCourse}
        onClose={handleCloseForm}
        onSubmit={handleCourseFormSubmit}
      />
    </div>
  );
}

type CourseListSectionProps = {
  status: CourseStatusFilter;
  onStatusChange: (status: CourseStatusFilter) => void;
  branchFilter: string;
  onBranchChange: (value: string) => void;
  branchOptions: BranchOption[];
  branchError: string | null;
  keywordInput: string;
  onKeywordInputChange: (value: string) => void;
  courses: CourseResponse[];
  loading: boolean;
  error: string | null;
  totalPages: number;
  currentPage: number;
  onPageChange: (page: number) => void;
  onEditCourse: (course: CourseResponse) => void;
  onToggleCourse: (course: CourseResponse, enabled: boolean) => void;
  mutatingCourseId: string | null;
};

function CourseListSection({
  status,
  onStatusChange,
  branchFilter,
  onBranchChange,
  branchOptions,
  branchError,
  keywordInput,
  onKeywordInputChange,
  courses,
  loading,
  error,
  totalPages,
  currentPage,
  onPageChange,
  onEditCourse,
  onToggleCourse,
  mutatingCourseId
}: CourseListSectionProps) {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3">
        <Tabs value={status} onValueChange={(value) => onStatusChange(value as CourseStatusFilter)} defaultValue={statusTabs[0].value}>
          <TabsList>
            {statusTabs.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value}>
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        <div className="grid gap-4 md:grid-cols-[280px_minmax(0,1fr)]">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-slate-700">지점 선택</label>
            <Select value={branchFilter} onChange={(event) => onBranchChange(event.target.value)}>
              {branchOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
            {branchError ? <InlineError message={branchError} /> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-slate-700">반 이름 검색</label>
            <Input
              value={keywordInput}
              onChange={(event) => onKeywordInputChange(event.target.value)}
              placeholder="예: 중3 수학"
            />
          </div>
        </div>
      </div>

      {loading ? (
        <div className="space-y-4">
          <Skeleton className="h-28 w-full rounded-2xl" />
          <Skeleton className="h-28 w-full rounded-2xl" />
          <Skeleton className="h-28 w-full rounded-2xl" />
        </div>
      ) : error ? (
        <InlineError message={error} />
      ) : courses.length === 0 ? (
        <EmptyState message="조건에 맞는 반이 없습니다." description="상태나 검색어를 바꿔 다시 시도해 보세요." />
      ) : (
        <div className="space-y-4">
          {courses.map((course) => (
            <CourseListCard
              key={course.courseId ?? course.name}
              course={course}
              onEdit={() => onEditCourse(course)}
              onToggle={(enabled) => onToggleCourse(course, enabled)}
              disabled={mutatingCourseId === course.courseId}
            />
          ))}
        </div>
      )}

      <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={onPageChange} disabled={loading} />
    </div>
  );
}

type CourseListCardProps = {
  course: CourseResponse;
  onEdit: () => void;
  onToggle: (enabled: boolean) => void;
  disabled?: boolean;
};

function CourseListCard({ course, onEdit, onToggle, disabled }: CourseListCardProps) {
  const scheduleSummary =
    course.schedules && course.schedules.length > 0
      ? course.schedules
          .map((schedule) => {
            const dayLabel = dayLabelMap[schedule.dayOfWeek ?? ""];
            const timeRange = formatTimeRange(schedule.startTime, schedule.endTime);
            return dayLabel && timeRange ? `${dayLabel} ${timeRange}` : null;
          })
          .filter(Boolean)
          .join(", ")
      : "등록된 스케줄이 없습니다.";
  const isActive = course.active !== false;
  const disableActions = disabled || !course.courseId;

  return (
    <div className="rounded-2xl border border-slate-200/70 bg-slate-50/80 p-5 shadow-sm">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-blue-500">
            {course.companyName ?? "학원"} • {course.branchName ?? "-"}
          </p>
          <h3 className="text-lg font-bold text-slate-900">{course.name ?? "이름 미지정"}</h3>
        </div>
        <div className="flex flex-col items-start gap-2 md:items-end">
          <CourseStatusToggle active={isActive} disabled={disableActions} onChange={(next) => onToggle(next)} />
          <button
            type="button"
            onClick={onEdit}
            disabled={disableActions}
            className={clsx(
              "rounded-xl border px-4 py-1.5 text-sm font-semibold transition",
              disableActions
                ? "cursor-not-allowed border-slate-200 text-slate-400"
                : "border-slate-300 text-slate-700 hover:border-blue-300 hover:text-blue-600"
            )}
          >
            상세 수정
          </button>
        </div>
      </div>
      <div className="mt-3 text-sm text-slate-600">
        <p>
          기간: {formatDateRange(course.startDate, course.endDate)}
        </p>
        <p className="mt-1 text-slate-500">스케줄: {scheduleSummary}</p>
      </div>
    </div>
  );
}

type CourseStatusToggleProps = {
  active: boolean;
  disabled?: boolean;
  onChange: (next: boolean) => void;
};

function CourseStatusToggle({ active, disabled, onChange }: CourseStatusToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={active}
      disabled={disabled}
      onClick={() => onChange(!active)}
      className={clsx(
        "flex items-center gap-3 rounded-full border px-3 py-1.5 text-xs font-semibold transition",
        active ? "border-emerald-200 bg-emerald-50 text-emerald-600" : "border-slate-200 bg-white text-slate-500",
        disabled && "cursor-not-allowed opacity-60"
      )}
    >
      <span>{active ? "활성" : "비활성"}</span>
      <span
        className={clsx(
          "relative inline-flex h-6 w-11 items-center rounded-full transition",
          active ? "bg-emerald-500" : "bg-slate-300"
        )}
      >
        <span
          className={clsx(
            "h-5 w-5 rounded-full bg-white shadow transition",
            active ? "translate-x-[22px]" : "translate-x-[2px]"
          )}
        />
      </span>
    </button>
  );
}

type CourseCalendarSectionProps = {
  rangeLabel: string;
  onPrevWeek: () => void;
  onNextWeek: () => void;
  onResetWeek: () => void;
  calendarItems: Record<string, CalendarItem[]>;
  courseColorMap: Record<string, string>;
  rangeStart: Date;
  loading: boolean;
  error: string | null;
  onCourseSelect: (course: CourseResponse) => void;
};

function CourseCalendarSection({
  rangeLabel,
  onPrevWeek,
  onNextWeek,
  onResetWeek,
  calendarItems,
  courseColorMap,
  rangeStart,
  loading,
  error,
  onCourseSelect
}: CourseCalendarSectionProps) {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <p className="font-semibold text-slate-900">{rangeLabel}</p>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onPrevWeek}
            className="flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 shadow-sm hover:bg-slate-50"
          >
            ◀
          </button>
          <button
            type="button"
            onClick={onResetWeek}
            className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm hover:bg-slate-50"
          >
            이번 주
          </button>
          <button
            type="button"
            onClick={onNextWeek}
            className="flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 shadow-sm hover:bg-slate-50"
          >
            ▶
          </button>
        </div>
      </div>
      {loading ? (
        <div className="space-y-3">
          <Skeleton className="h-16 w-full rounded-2xl" />
          <Skeleton className="h-[640px] w-full rounded-2xl" />
        </div>
      ) : error ? (
        <InlineError message={error} />
      ) : (
        <CalendarGrid
          calendarItems={calendarItems}
          courseColorMap={courseColorMap}
          rangeStart={rangeStart}
          onCourseSelect={onCourseSelect}
        />
      )}
    </div>
  );
}

type CalendarGridProps = {
  calendarItems: Record<string, CalendarItem[]>;
  courseColorMap: Record<string, string>;
  rangeStart: Date;
  onCourseSelect: (course: CourseResponse) => void;
};

function CalendarGrid({ calendarItems, courseColorMap, rangeStart, onCourseSelect }: CalendarGridProps) {
  const templateColumns = `80px repeat(${calendarDays.length}, minmax(0, 1fr))`;
  const totalHours = calendarEndHour - calendarStartHour;
  const columnHeight = totalHours * calendarHourHeight;

  return (
    <div className="overflow-x-auto rounded-3xl border border-slate-200 bg-white shadow-inner">
      <div className="min-w-[960px]">
        <div className="grid border-b border-slate-200" style={{ gridTemplateColumns: templateColumns }}>
          <div className="flex h-14 items-center justify-center border-r border-slate-200 bg-slate-50 text-xs font-semibold text-slate-500">
            시간
          </div>
          {calendarDays.map((day, index) => (
            <div
              key={day.key}
              className="flex h-14 flex-col items-center justify-center border-r border-slate-200 text-sm font-semibold text-slate-900"
            >
              <span>{day.label}</span>
              <span className="text-xs font-normal text-slate-500">{formatHeaderDate(addDays(rangeStart, index))}</span>
            </div>
          ))}
        </div>
        <div className="grid" style={{ gridTemplateColumns: templateColumns }}>
          <CalendarTimeColumn columnHeight={columnHeight} />
          {calendarDays.map((day) => (
            <CalendarDayColumn
              key={day.key}
              items={calendarItems[day.key] ?? []}
              columnHeight={columnHeight}
              courseColorMap={courseColorMap}
              onCourseSelect={onCourseSelect}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

function CalendarTimeColumn({ columnHeight }: { columnHeight: number }) {
  const hours = Array.from({ length: calendarEndHour - calendarStartHour + 1 }, (_, index) => calendarStartHour + index);
  return (
    <div className="border-r border-slate-200 bg-slate-50" style={{ height: columnHeight }}>
      {hours.map((hour, index) => (
        <div
          key={hour}
          className="relative flex items-start justify-end pr-3"
          style={{ height: index === hours.length - 1 ? calendarHourHeight / 2 : calendarHourHeight }}
        >
          <span className="text-[11px] font-semibold text-slate-500">{formatHourLabel(hour)}</span>
        </div>
      ))}
    </div>
  );
}

function CalendarDayColumn({
  items,
  columnHeight,
  courseColorMap,
  onCourseSelect
}: {
  items: CalendarItem[];
  columnHeight: number;
  courseColorMap: Record<string, string>;
  onCourseSelect: (course: CourseResponse) => void;
}) {
  const hourLines = Array.from({ length: calendarEndHour - calendarStartHour }, (_, index) => index + 1);
  return (
    <div className="relative border-r border-slate-200" style={{ height: columnHeight }}>
      {hourLines.map((line) => (
        <div
          key={line}
          className="absolute left-0 right-0 border-b border-slate-100"
          style={{ top: line * calendarHourHeight, height: 0 }}
        />
      ))}
      {items.length === 0 ? (
        <p className="pointer-events-none absolute inset-0 flex items-center justify-center text-xs text-slate-200">-</p>
      ) : null}
      {items.map(({ course, schedule }, index) => {
        const colorKey = getCourseColorKey(course);
        const colorClass = courseColorMap[colorKey] ?? "bg-blue-400";
        const startHour = timeStringToHours(schedule.startTime) ?? calendarStartHour;
        const endHour = timeStringToHours(schedule.endTime) ?? Math.min(startHour + 1, calendarEndHour);
        const clampedStart = Math.max(calendarStartHour, Math.min(startHour, calendarEndHour));
        const clampedEnd = Math.max(clampedStart + 0.5, Math.min(endHour, calendarEndHour));
        const blockTop = (clampedStart - calendarStartHour) * calendarHourHeight;
        const blockHeight = Math.max((clampedEnd - clampedStart) * calendarHourHeight, calendarHourHeight * 0.6);

        return (
          <div
            key={`${course.courseId ?? course.name}-${schedule.dayOfWeek}-${schedule.startTime}-${index}`}
            className={clsx(
              "absolute left-1 right-1 cursor-pointer rounded-2xl border border-white/70 p-2 text-xs font-semibold text-white shadow-lg transition hover:scale-[1.01]",
              colorClass
            )}
            style={{
              top: blockTop,
              height: blockHeight,
              minHeight: calendarHourHeight * 0.6
            }}
            role="button"
            tabIndex={0}
            onClick={() => onCourseSelect(course)}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                onCourseSelect(course);
              }
            }}
            title={`${course.name ?? "이름 미지정"} • ${course.companyName ?? "-"} ${course.branchName ?? ""}`.trim()}
          >
            <p className="truncate text-sm">{course.name ?? "이름 미지정"}</p>
            <p className="text-[11px] font-normal opacity-90">
              {course.companyName ?? "-"} · {course.branchName ?? "-"}
            </p>
          </div>
        );
      })}
    </div>
  );
}

type CourseFormModalProps = {
  open: boolean;
  mode: CourseFormMode;
  branchOptions: BranchOption[];
  initialCourse: CourseResponse | null;
  onClose: () => void;
  onSubmit: (values: CourseFormValues) => Promise<void>;
};

function CourseFormModal({ open, mode, branchOptions, initialCourse, onClose, onSubmit }: CourseFormModalProps) {
  const [submitError, setSubmitError] = useState<string | null>(null);
  const form = useForm<CourseFormValues>({
    resolver: zodResolver(courseFormSchema),
    defaultValues: getDefaultCourseFormValues(mode, initialCourse, branchOptions)
  });
  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors, isSubmitting },
    reset
  } = form;
  const { fields, append, remove } = useFieldArray({ control, name: "schedules" });
  const scheduleErrors = Array.isArray(errors.schedules) ? errors.schedules : [];
  const scheduleListError = !Array.isArray(errors.schedules) ? errors.schedules?.message : undefined;
  const scheduleValues = watch("schedules");

  useEffect(() => {
    if (!open) {
      return;
    }
    reset(getDefaultCourseFormValues(mode, initialCourse, branchOptions));
    setSubmitError(null);
  }, [branchOptions, initialCourse, mode, open, reset]);

  const handleAddSchedule = () => {
    append(createDefaultSchedule());
  };

  const handleRemoveSchedule = (index: number) => {
    if (fields.length === 1) {
      return;
    }
    remove(index);
  };

  const onValid = async (values: CourseFormValues) => {
    setSubmitError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      const message = error instanceof Error ? error.message : "반 정보를 저장하지 못했습니다.";
      setSubmitError(message);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={mode === "CREATE" ? "새 반 등록" : "반 정보 수정"}
      size="lg"
      mobileLayout="bottom-sheet"
    >
      <form onSubmit={handleSubmit(onValid)} className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2">
          {mode === "CREATE" ? (
            <Select
              label="출강 지점"
              required
              {...register("branchId")}
              error={errors.branchId?.message}
            >
              <option value="">지점을 선택하세요</option>
              {branchOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          ) : (
            <div className="flex flex-col gap-1.5">
              <span className="text-sm font-semibold text-slate-700">출강 지점</span>
              <p className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-800">
                {formatBranchLabel(initialCourse)}
              </p>
              <input type="hidden" {...register("branchId")} />
            </div>
          )}
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-slate-700">
              반 이름 <span className="text-rose-500">*</span>
            </label>
            <Input
              placeholder="예: 고2 심화 수학"
              {...register("name")}
              className={clsx(errors.name && "border-rose-300 focus:ring-rose-200")}
            />
            {errors.name ? <InlineError message={errors.name.message ?? ""} /> : null}
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-semibold text-slate-700">반 설명</label>
          <textarea
            rows={3}
            placeholder="수업 대상, 커리큘럼, 참고 메모를 입력하세요."
            {...register("description")}
            className={clsx(
              "w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-200",
              errors.description && "border-rose-300 focus:ring-rose-200"
            )}
          />
          {errors.description ? <InlineError message={errors.description.message ?? ""} /> : null}
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-slate-700">
              시작일 <span className="text-rose-500">*</span>
            </label>
            <Input
              type="text"
              inputMode="numeric"
              placeholder="YYYY/MM/DD"
              {...register("startDate")}
              className={clsx(errors.startDate && "border-rose-300")}
            />
            {errors.startDate ? <InlineError message={errors.startDate.message ?? ""} /> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-slate-700">
              종료일 <span className="text-rose-500">*</span>
            </label>
            <Input
              type="text"
              inputMode="numeric"
              placeholder="YYYY/MM/DD"
              {...register("endDate")}
              className={clsx(errors.endDate && "border-rose-300")}
            />
            {errors.endDate ? <InlineError message={errors.endDate.message ?? ""} /> : null}
          </div>
        </div>

        <div className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-sm font-semibold text-slate-800">스케줄</p>
            <Button type="button" variant="secondary" className="h-10 px-4 text-sm" onClick={handleAddSchedule}>
              + 요일 추가
            </Button>
          </div>
          <div className="space-y-3">
            {fields.map((field, index) => (
              <div key={field.id} className="rounded-2xl border border-slate-200 p-4">
                <div className="grid gap-3 md:grid-cols-[160px_minmax(0,1fr)_auto]">
                  <Select
                    {...register(`schedules.${index}.dayOfWeek` as const)}
                    error={scheduleErrors[index]?.dayOfWeek?.message}
                  >
                    {calendarDays.map((day) => (
                      <option key={day.key} value={day.key}>
                        {day.label}
                      </option>
                    ))}
                  </Select>
                  <button
                    type="button"
                    onClick={() => handleRemoveSchedule(index)}
                    disabled={fields.length === 1}
                    className={clsx(
                      "rounded-xl border px-3 py-2 text-sm font-semibold transition",
                      fields.length === 1
                        ? "cursor-not-allowed border-slate-200 text-slate-400"
                        : "border-slate-200 text-slate-600 hover:border-rose-200 hover:text-rose-500"
                    )}
                  >
                    삭제
                  </button>
                </div>
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                  <Controller
                    control={control}
                    name={`schedules.${index}.startTime`}
                    render={({ field }) => {
                      const startValue = field.value ?? formatMinutesToTime(calendarStartHour * 60);
                      return (
                        <TimeSlotToggle
                          label="시작 시간"
                          value={startValue}
                          minMinutes={calendarStartHour * 60}
                          maxMinutes={calendarEndHour * 60 - TIME_SLOT_STEP_MINUTES}
                          stepMinutes={TIME_SLOT_STEP_MINUTES}
                          error={scheduleErrors[index]?.startTime?.message}
                          onChange={(next) => {
                            field.onChange(next);
                            const currentEnd = scheduleValues?.[index]?.endTime;
                            if (!currentEnd || !isEndAfterStart(next, currentEnd)) {
                              const adjustedEnd = getNextSlotTime(next, TIME_SLOT_STEP_MINUTES);
                              setValue(`schedules.${index}.endTime`, adjustedEnd, {
                                shouldValidate: true,
                                shouldDirty: true
                              });
                            }
                          }}
                        />
                      );
                    }}
                  />
                  <Controller
                    control={control}
                    name={`schedules.${index}.endTime`}
                    render={({ field }) => {
                      const startValue = scheduleValues?.[index]?.startTime ?? formatMinutesToTime(calendarStartHour * 60);
                      const startMinutes = timeStringToMinutes(startValue) ?? calendarStartHour * 60;
                      const minEndMinutes = Math.min(
                        calendarEndHour * 60,
                        Math.max(startMinutes + TIME_SLOT_STEP_MINUTES, calendarStartHour * 60 + TIME_SLOT_STEP_MINUTES)
                      );
                      const endValue = field.value ?? getNextSlotTime(startValue, TIME_SLOT_STEP_MINUTES);
                      return (
                        <TimeSlotToggle
                          label="종료 시간"
                          value={endValue}
                          minMinutes={minEndMinutes}
                          maxMinutes={calendarEndHour * 60}
                          stepMinutes={TIME_SLOT_STEP_MINUTES}
                          error={scheduleErrors[index]?.endTime?.message}
                          onChange={field.onChange}
                        />
                      );
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
          {scheduleListError ? <InlineError message={scheduleListError} /> : null}
        </div>

        {submitError ? <InlineError message={submitError} /> : null}

        <div className="flex flex-col gap-2 border-t border-slate-100 pt-4 md:flex-row md:justify-end">
          <Button type="button" variant="ghost" className="w-full md:w-auto" onClick={onClose} disabled={isSubmitting}>
            취소
          </Button>
          <Button type="submit" className="w-full md:w-auto" disabled={isSubmitting}>
            {isSubmitting ? "저장 중..." : mode === "CREATE" ? "반 생성" : "변경 사항 저장"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

type TimeSlotToggleProps = {
  label?: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  minMinutes?: number;
  maxMinutes?: number;
  stepMinutes?: number;
};

function TimeSlotToggle({
  label,
  value,
  onChange,
  error,
  minMinutes = calendarStartHour * 60,
  maxMinutes = calendarEndHour * 60,
  stepMinutes = TIME_SLOT_STEP_MINUTES
}: TimeSlotToggleProps) {
  const options = useMemo(() => {
    const normalizedMin = Math.max(calendarStartHour * 60, minMinutes);
    const normalizedMax = Math.min(calendarEndHour * 60, maxMinutes);
    return generateTimeSlots(normalizedMin, normalizedMax, stepMinutes);
  }, [minMinutes, maxMinutes, stepMinutes]);

  const currentValue = options.includes(value) ? value : options[0];

  return (
    <div className="flex flex-col gap-2">
      {label ? (
        <span className="text-sm font-semibold text-slate-700">
          {label}
          <span className="ml-1 text-rose-500">*</span>
        </span>
      ) : null}
      <div className="flex flex-wrap gap-2">
        {options.map((option) => {
          const isActive = currentValue === option;
          return (
            <button
              type="button"
              key={option}
              className={clsx(
                "rounded-2xl px-3 py-2 text-sm font-semibold",
                isActive ? "bg-blue-600 text-white shadow" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              )}
              aria-pressed={isActive}
              onClick={() => onChange(option)}
            >
              {option}
            </button>
          );
        })}
      </div>
      {error ? <InlineError message={error} /> : null}
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
  const pages = Array.from({ length: totalPages }, (_, index) => index);
  return (
    <div className="flex flex-wrap items-center justify-between gap-4">
      <p className="text-sm text-slate-500">
        페이지 {currentPage + 1} / {totalPages}
      </p>
      <div className="flex flex-wrap gap-2">
        {pages.map((page) => (
          <button
            key={page}
            type="button"
            disabled={disabled}
            className={clsx(
              "h-9 w-9 rounded-full text-sm font-semibold transition",
              currentPage === page
                ? "bg-blue-600 text-white shadow-md"
                : "bg-white text-slate-600 shadow hover:bg-slate-50"
            )}
            onClick={() => onPageChange(page)}
          >
            {page + 1}
          </button>
        ))}
      </div>
    </div>
  );
}

const dayLabelMap: Record<string, string> = calendarDays.reduce(
  (acc, day) => ({ ...acc, [day.key]: day.label }),
  {} as Record<string, string>
);

function formatHeaderDate(date: Date) {
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function formatHourLabel(hour: number) {
  return `${String(hour).padStart(2, "0")}:00`;
}

function timeStringToMinutes(value?: string | null) {
  if (!value) {
    return null;
  }
  const [hourPart, minutePart] = value.split(":");
  const hour = Number.parseInt(hourPart ?? "", 10);
  const minute = Number.parseInt(minutePart ?? "", 10);
  if (Number.isNaN(hour) || Number.isNaN(minute)) {
    return null;
  }
  return hour * 60 + minute;
}

function timeStringToHours(value?: string | null) {
  const minutes = timeStringToMinutes(value);
  return minutes === null ? null : minutes / 60;
}

function generateTimeSlots(minMinutes: number, maxMinutes: number, stepMinutes: number) {
  if (stepMinutes <= 0) {
    return [formatMinutesToTime(minMinutes)];
  }
  const start = Math.min(minMinutes, maxMinutes);
  const end = Math.max(minMinutes, maxMinutes);
  const slots: string[] = [];
  for (let minutes = start; minutes <= end; minutes += stepMinutes) {
    slots.push(formatMinutesToTime(minutes));
  }
  if (slots.length === 0) {
    slots.push(formatMinutesToTime(start));
  }
  return slots;
}

function formatMinutesToTime(minutes: number) {
  const safeMinutes = Math.max(calendarStartHour * 60, Math.min(minutes, calendarEndHour * 60));
  const hour = Math.floor(safeMinutes / 60);
  const minute = safeMinutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

function isEndAfterStart(start?: string, end?: string) {
  const startMinutes = timeStringToMinutes(start);
  const endMinutes = timeStringToMinutes(end);
  if (startMinutes === null || endMinutes === null) {
    return false;
  }
  return endMinutes > startMinutes;
}

function getNextSlotTime(value?: string, stepMinutes = TIME_SLOT_STEP_MINUTES) {
  const startMinutes = timeStringToMinutes(value) ?? calendarStartHour * 60;
  const nextMinutes = Math.min(calendarEndHour * 60, startMinutes + stepMinutes);
  return formatMinutesToTime(nextMinutes);
}

function getCourseColorKey(course: CourseResponse) {
  if (course.courseId) {
    return course.courseId;
  }
  if (course.name) {
    return `name:${course.name}`;
  }
  return `temp:${JSON.stringify(course.schedules ?? [])}`;
}

function formatDateRange(start?: string, end?: string) {
  if (!start && !end) {
    return "-";
  }
  if (start && end) {
    return `${formatDisplayDate(start)} ~ ${formatDisplayDate(end)}`;
  }
  if (start) {
    return formatDisplayDate(start);
  }
  if (end) {
    return formatDisplayDate(end);
  }
  return "-";
}

function formatDisplayDate(date: string | Date) {
  const target = date instanceof Date ? date : new Date(`${date}T00:00:00`);
  if (Number.isNaN(target.getTime())) {
    return typeof date === "string" ? date : "-";
  }
  const year = target.getFullYear();
  const month = String(target.getMonth() + 1).padStart(2, "0");
  const day = String(target.getDate()).padStart(2, "0");
  return `${year}.${month}.${day}`;
}

function formatTimeRange(start?: string, end?: string) {
  if (!start || !end) {
    return "";
  }
  return `${start.slice(0, 5)} - ${end.slice(0, 5)}`;
}

function formatTimeInput(value?: string | null) {
  if (!value) {
    return "";
  }
  if (value.length >= 5) {
    return value.slice(0, 5);
  }
  return value;
}

function formatBranchLabel(course: CourseResponse | null | undefined) {
  if (!course) {
    return "-";
  }
  const base = [course.companyName, course.branchName].filter(Boolean).join(" ");
  return base.length > 0 ? base : "지점 정보 없음";
}

function createDefaultSchedule(): CourseFormValues["schedules"][number] {
  return {
    dayOfWeek: "MONDAY",
    startTime: "09:00",
    endTime: "10:00"
  };
}

function getDefaultCourseFormValues(
  mode: CourseFormMode,
  course: CourseResponse | null,
  branchOptions: BranchOption[]
): CourseFormValues {
  if (mode === "EDIT" && course) {
    const schedules =
      course.schedules && course.schedules.length > 0
        ? course.schedules.map((schedule) => ({
            dayOfWeek: (schedule?.dayOfWeek as CourseFormValues["schedules"][number]["dayOfWeek"]) ?? "MONDAY",
            startTime: formatTimeInput(schedule?.startTime),
            endTime: formatTimeInput(schedule?.endTime)
          }))
        : [createDefaultSchedule()];
    return {
      branchId: course.branchId ?? "",
      name: course.name ?? "",
      description: course.description ?? "",
      startDate: formatDateForInput(course.startDate),
      endDate: formatDateForInput(course.endDate),
      schedules
    };
  }

  return {
    branchId: branchOptions.length === 1 ? branchOptions[0].value : "",
    name: "",
    description: "",
    startDate: "",
    endDate: "",
    schedules: [createDefaultSchedule()]
  };
}

function getCurrentWeekRange(): WeekRange {
  return getWeekRangeFromDate(new Date());
}

function getWeekRangeFromDate(date: Date): WeekRange {
  const start = startOfWeek(date);
  return { start, end: addDays(start, 6) };
}

function startOfWeek(date: Date): Date {
  const target = new Date(date);
  const day = target.getDay(); // 0 (Sunday) - 6 (Saturday)
  const diff = day === 0 ? -6 : 1 - day; // Monday as start
  target.setDate(target.getDate() + diff);
  target.setHours(0, 0, 0, 0);
  return target;
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function formatDateParam(date: Date) {
  return date.toISOString().split("T")[0];
}

function formatDateForInput(value?: string | null) {
  if (!value) {
    return "";
  }
  return value.replaceAll("-", "/");
}

function normalizeDateInput(value: string) {
  if (!DATE_INPUT_REGEX.test(value)) {
    throw new Error("날짜는 YYYY/MM/DD 형식으로 입력하세요.");
  }
  const [year, month, day] = value.split("/");
  return `${year}-${month}-${day}`;
}
