"use client";

import { useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Modal } from "@/components/ui/modal";
import { TextField } from "@/components/ui/text-field";
import { Checkbox } from "@/components/ui/checkbox";
import { TimeSelect } from "@/components/ui/time-select";
import { Button } from "@/components/ui/button";
import { useCreateCourse, useUpdateCourse } from "@/hooks/use-courses";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type CourseResponse = components["schemas"]["CourseResponse"];

const DEFAULT_START_TIME = "14:00";
const DEFAULT_END_TIME = "16:00";

const normalizeTime = (timeValue?: string | null, fallback = DEFAULT_START_TIME) => {
  if (!timeValue) {
    return fallback;
  }
  const [hour = "00", minute = "00"] = timeValue.split(":");
  return `${hour.padStart(2, "0")}:${minute.padStart(2, "0")}`;
};

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
    daysOfWeek: z
      .array(z.enum(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]))
      .min(1, "최소 1개의 요일을 선택해주세요"),
    startTime: z.string().regex(/^\d{2}:\d{2}$/, "시작 시간을 선택해주세요"),
    endTime: z.string().regex(/^\d{2}:\d{2}$/, "종료 시간을 선택해주세요")
  })
  .refine((data) => data.startTime < data.endTime, {
    message: "종료 시간은 시작 시간보다 이후여야 합니다",
    path: ["endTime"]
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
      daysOfWeek: [],
      startTime: DEFAULT_START_TIME,
      endTime: DEFAULT_END_TIME
    }
  });

  const selectedDays = watch("daysOfWeek") || [];
  const startTime = watch("startTime") || DEFAULT_START_TIME;
  const endTime = watch("endTime") || DEFAULT_END_TIME;

  const setStartTime = useCallback(
    (value: string) => {
      setValue("startTime", value, { shouldDirty: true, shouldValidate: true });
    },
    [setValue]
  );

  const setEndTime = useCallback(
    (value: string) => {
      setValue("endTime", value, { shouldDirty: true, shouldValidate: true });
    },
    [setValue]
  );

  // 수정 모드일 때 폼 초기화
  useEffect(() => {
    if (open && editingCourse) {
      reset({
        name: editingCourse.name || "",
        company: editingCourse.company || "",
        daysOfWeek: (editingCourse.daysOfWeek || []) as DayOfWeek[],
        startTime: normalizeTime(editingCourse.startTime, DEFAULT_START_TIME),
        endTime: normalizeTime(editingCourse.endTime, DEFAULT_END_TIME)
      });
    } else if (open && !editingCourse) {
      reset({
        name: "",
        company: "",
        daysOfWeek: [],
        startTime: DEFAULT_START_TIME,
        endTime: DEFAULT_END_TIME
      });
    }
  }, [open, editingCourse, reset]);

  const handleDayToggle = (day: DayOfWeek) => {
    const newDays = selectedDays.includes(day)
      ? selectedDays.filter((d) => d !== day)
      : [...selectedDays, day];
    setValue("daysOfWeek", newDays, { shouldDirty: true, shouldValidate: true });
  };

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
      size="md"
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

        {/* 수업 요일 */}
        <fieldset className="space-y-3">
          <legend className="text-sm font-semibold text-slate-700">
            수업 요일 <span className="text-rose-500">*</span>
          </legend>
          <div className="flex flex-wrap gap-2">
            {DAYS_OF_WEEK.map((day) => (
              <label
                key={day.value}
                className={`flex cursor-pointer items-center justify-center gap-2 rounded-lg border px-4 py-2 transition ${
                  selectedDays.includes(day.value)
                    ? "border-blue-500 bg-blue-50 text-blue-700"
                    : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                }`}
              >
                <Checkbox
                  checked={selectedDays.includes(day.value)}
                  onChange={() => handleDayToggle(day.value)}
                  label=""
                  className="sr-only"
                />
                <span className="text-sm font-medium">{day.label}</span>
              </label>
            ))}
          </div>
          {errors.daysOfWeek?.message ? (
            <p className="text-xs font-semibold text-rose-600">{errors.daysOfWeek.message}</p>
          ) : null}
        </fieldset>

        {/* 시작 시간 */}
        <TimeSelect
          label="시작 시간"
          value={startTime}
          onChange={setStartTime}
          error={errors.startTime?.message}
          required
        />

        {/* 종료 시간 */}
        <TimeSelect
          label="종료 시간"
          value={endTime}
          onChange={setEndTime}
          error={errors.endTime?.message}
          required
        />

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
