"use client";

import { useEffect } from "react";
import { FieldErrors, useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Modal } from "@/components/ui/modal";
import { TextField } from "@/components/ui/text-field";
import { TimeSelect } from "@/components/ui/time-select";
import { Button } from "@/components/ui/button";
import { useCreateCourse, useUpdateCourse } from "@/hooks/use-courses";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type CourseResponse = components["schemas"]["CourseResponse"];
type CourseScheduleRequest = components["schemas"]["CourseScheduleRequest"];

const DEFAULT_START_TIME = "14:00";
const DEFAULT_END_TIME = "16:00";

const normalizeTime = (timeValue?: string | null, fallback = DEFAULT_START_TIME) => {
  if (!timeValue) {
    return fallback;
  }
  const [hour = "00", minute = "00"] = timeValue.split(":");
  return `${hour.padStart(2, "0")}:${minute.padStart(2, "0")}`;
};

const scheduleSchema = z.object({
  dayOfWeek: z.enum(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]),
  startTime: z.string().regex(/^\d{2}:\d{2}$/, "시작 시간을 선택해주세요"),
  endTime: z.string().regex(/^\d{2}:\d{2}$/, "종료 시간을 선택해주세요")
});

// Validation Schema
const courseFormSchema = z
  .object({
    name: z
      .string()
      .min(1, "반 이름을 입력해주세요")
      .max(100, "반 이름은 100자 이내로 입력해주세요"),
    company: z
      .string()
      .min(1, "학원/회사명을 입력해주세요")
      .max(100, "학원/회사명은 100자 이내로 입력해주세요"),
    schedules: z
      .array(scheduleSchema)
      .min(1, "최소 1개의 요일을 추가해주세요")
  })
  .superRefine((data, ctx) => {
    const uniqueDays = new Set(data.schedules.map((schedule) => schedule.dayOfWeek));
    if (uniqueDays.size !== data.schedules.length) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "요일은 중복될 수 없습니다",
        path: ["schedules"]
      });
    }
    data.schedules.forEach((schedule, index) => {
      if (!(schedule.startTime < schedule.endTime)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `${schedule.dayOfWeek} 수업 종료 시간은 시작 시간 이후여야 합니다`,
          path: ["schedules", index, "endTime"]
        });
      }
    });
  });

type CourseFormData = z.infer<typeof courseFormSchema>;

type DayOfWeek = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";

const DAYS_OF_WEEK: { value: DayOfWeek; label: string }[] = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" }
];

const DAY_ORDER: Record<DayOfWeek, number> = {
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
  SUNDAY: 7
};

type CourseFormModalProps = {
  open: boolean;
  onClose: () => void;
  editingCourse: CourseResponse | null;
};

export function CourseFormModal({ open, onClose, editingCourse }: CourseFormModalProps) {
  const createMutation = useCreateCourse();
  const updateMutation = useUpdateCourse();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors }
  } = useForm<CourseFormData>({
    resolver: zodResolver(courseFormSchema),
    defaultValues: {
      name: "",
      company: "",
      schedules: []
    }
  });

  const schedules = watch("schedules") || [];
  const scheduleArrayErrors: FieldErrors<CourseScheduleRequest>[] = Array.isArray(errors.schedules)
    ? (errors.schedules as FieldErrors<CourseScheduleRequest>[])
    : [];
  const schedulesErrorMessage =
      !Array.isArray(errors.schedules) && errors.schedules?.message ? errors.schedules.message : undefined;

  const handleDayToggle = (day: DayOfWeek) => {
    const exists = schedules.find((schedule) => schedule.dayOfWeek === day);
    if (exists) {
      setValue(
        "schedules",
        schedules.filter((schedule) => schedule.dayOfWeek !== day),
        { shouldDirty: true, shouldValidate: true }
      );
    } else {
      setValue(
        "schedules",
        [
          ...schedules,
          {
            dayOfWeek: day,
            startTime: DEFAULT_START_TIME,
            endTime: DEFAULT_END_TIME
          }
        ],
        { shouldDirty: true, shouldValidate: true }
      );
    }
  };

  const updateScheduleTime = (day: DayOfWeek, field: "startTime" | "endTime", value: string) => {
    setValue(
      "schedules",
      schedules.map((schedule) =>
        schedule.dayOfWeek === day ? { ...schedule, [field]: value } : schedule
      ),
      { shouldDirty: true, shouldValidate: true }
    );
  };

  // 수정 모드일 때 폼 초기화
  useEffect(() => {
    if (open && editingCourse) {
      reset({
        name: editingCourse.name || "",
        company: editingCourse.company || "",
        schedules:
          editingCourse.schedules?.map((schedule) => ({
            dayOfWeek: schedule.dayOfWeek as DayOfWeek,
            startTime: normalizeTime(schedule.startTime, DEFAULT_START_TIME),
            endTime: normalizeTime(schedule.endTime, DEFAULT_END_TIME)
          })) ?? []
      });
    } else if (open && !editingCourse) {
      reset({
        name: "",
        company: "",
        schedules: []
      });
    }
  }, [open, editingCourse, reset]);

  const onSubmit = async (data: CourseFormData) => {
    try {
      if (editingCourse && editingCourse.id) {
        // 수정
        await updateMutation.mutateAsync({
          courseId: editingCourse.id,
          request: data
        });
      } else {
        // 생성
        await createMutation.mutateAsync(data);
      }
      onClose();
    } catch (error) {
      // 에러는 mutation의 onError에서 처리됨
      console.error(error);
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;
  const error = createMutation.error || updateMutation.error;
  const errorMessage = error ? getApiErrorMessage(error, "저장에 실패했습니다.") : null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editingCourse ? "반 수정" : "새 반 만들기"}
      size="lg"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* 반 이름 */}
        <TextField
          label="반 이름"
          placeholder="예: 중등 수학 A반"
          required
          error={errors.name?.message}
          {...register("name")}
        />

        {/* 학원/회사 */}
        <TextField
          label="소속 학원/회사"
          placeholder="예: ABC 학원"
          required
          error={errors.company?.message}
          {...register("company")}
        />

        {/* 수업 요일 및 시간표 */}
        <fieldset className="space-y-4">
          <legend className="text-sm font-semibold text-slate-700">
            수업 요일 / 시간표 <span className="text-rose-500">*</span>
          </legend>

          <div className="flex flex-wrap gap-2">
            {DAYS_OF_WEEK.map((day) => {
              const isActive = schedules.some((schedule) => schedule.dayOfWeek === day.value);
              return (
                <button
                  key={day.value}
                  type="button"
                  onClick={() => handleDayToggle(day.value)}
                  className={`rounded-lg border px-4 py-2 text-sm font-medium transition ${
                    isActive
                      ? "border-blue-500 bg-blue-50 text-blue-700"
                      : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                  }`}
                >
                  {day.label}
                </button>
              );
            })}
          </div>
          {schedulesErrorMessage ? (
            <p className="text-xs font-semibold text-rose-600">{schedulesErrorMessage}</p>
          ) : null}

          {schedules.length > 0 ? (
            <div className="space-y-3">
              {schedules
                .slice()
                .sort((a, b) => DAY_ORDER[a.dayOfWeek] - DAY_ORDER[b.dayOfWeek])
                .map((schedule, index) => {
                  const scheduleErrors = scheduleArrayErrors[index];
                  return (
                    <div
                      key={schedule.dayOfWeek}
                      className="flex flex-col gap-3 rounded-2xl border border-slate-200 p-4 md:flex-row md:items-center"
                    >
                      <div className="w-24 text-sm font-semibold text-slate-800">
                        {DAYS_OF_WEEK.find((day) => day.value === schedule.dayOfWeek)?.label}
                      </div>
                      <div className="flex flex-1 flex-col gap-3 md:flex-row">
                        <TimeSelect
                          label="시작"
                          value={schedule.startTime}
                          onChange={(value) => updateScheduleTime(schedule.dayOfWeek, "startTime", value)}
                          error={
                            scheduleErrors && !Array.isArray(scheduleErrors.startTime)
                              ? (scheduleErrors.startTime?.message as string | undefined)
                              : undefined
                          }
                        />
                        <TimeSelect
                          label="종료"
                          value={schedule.endTime}
                          onChange={(value) => updateScheduleTime(schedule.dayOfWeek, "endTime", value)}
                          error={
                            scheduleErrors && !Array.isArray(scheduleErrors.endTime)
                              ? (scheduleErrors.endTime?.message as string | undefined)
                              : undefined
                          }
                        />
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => handleDayToggle(schedule.dayOfWeek)}
                      >
                        제거
                      </Button>
                    </div>
                  );
                })}
            </div>
          ) : null}
        </fieldset>

        {/* 에러 메시지 */}
        {errorMessage ? (
          <div className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{errorMessage}</div>
        ) : null}

        {/* 버튼 */}
        <div className="flex gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={onClose} disabled={isLoading} className="flex-1">
            취소
          </Button>
          <Button type="submit" disabled={isLoading} className="flex-1">
            {isLoading ? "저장 중..." : editingCourse ? "수정" : "생성"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
