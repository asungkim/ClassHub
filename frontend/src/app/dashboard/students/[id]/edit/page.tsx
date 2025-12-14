"use client";

import { useEffect, useMemo, useState, FormEvent } from "react";
import { useParams, useRouter } from "next/navigation";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { TextField } from "@/components/ui/text-field";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/ui/error-state";
import { useStudentProfileDetail, useUpdateStudentProfile } from "@/hooks/use-student-profiles";
import { useAssistantList } from "@/hooks/use-assistants";
import { useCourses } from "@/hooks/use-courses";
import { getApiErrorMessage } from "@/lib/api-error";
import { CoursePicker } from "@/components/course/course-picker";
import type { components } from "@/types/openapi";

type StudentProfileUpdateRequest = components["schemas"]["StudentProfileUpdateRequest"];
type StudentProfileResponse = components["schemas"]["StudentProfileResponse"];

type FormState = {
  name: string;
  phoneNumber: string;
  parentPhone: string;
  schoolName: string;
  grade: string;
  age: string;
  assistantId: string;
  defaultClinicSlotId?: string;
  selectedCourseIds: string[];
};

const emptyForm: FormState = {
  name: "",
  phoneNumber: "",
  parentPhone: "",
  schoolName: "",
  grade: "",
  age: "",
  assistantId: "",
  defaultClinicSlotId: "",
  selectedCourseIds: []
};

export default function StudentEditPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const params = useParams<{ id: string }>();
  const profileId = params?.id;
  const router = useRouter();
  const detailQuery = useStudentProfileDetail(profileId ?? "");
  const updateMutation = useUpdateStudentProfile();
  const assistantsQuery = useAssistantList({ active: true, page: 0 });
  const coursesQuery = useCourses(true);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [clientError, setClientError] = useState<string | null>(null);

  const isLoading = detailQuery.isLoading;
  const isError = detailQuery.isError;
  const student = detailQuery.data;
  const assistants = assistantsQuery.data?.content ?? [];

  useEffect(() => {
    if (student) {
      setForm(mapResponseToForm(student));
    }
  }, [student]);

  const titleSuffix = useMemo(() => (student?.name ? ` - ${student.name}` : ""), [student?.name]);

  if (!canRender) {
    return fallback;
  }

  const handleChange = (field: keyof FormState) => (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSelectChange = (field: keyof FormState) => (event: React.ChangeEvent<HTMLSelectElement>) => {
    const value = event.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setClientError(null);
    if (!profileId) {
      setClientError("학생 ID가 없습니다.");
      return;
    }

    const payload = buildUpdatePayload(form);
    if (typeof payload === "string") {
      setClientError(payload);
      return;
    }

    try {
      await updateMutation.mutateAsync({ profileId, body: payload });
      router.push("/dashboard/students");
    } catch (error) {
      setClientError(getApiErrorMessage(error, "학생 정보 수정에 실패했습니다."));
    }
  };

  if (isError) {
    return (
      <DashboardShell title="학생 정보 수정" subtitle="학생 정보를 수정합니다.">
        <ErrorState
          title="학생 정보를 불러오지 못했습니다"
          description={getApiErrorMessage(detailQuery.error, "학생 정보를 불러올 수 없습니다.")}
          retryLabel="다시 시도"
          onRetry={() => detailQuery.refetch()}
        />
      </DashboardShell>
    );
  }

  if (isLoading || !student) {
    return (
      <DashboardShell title="학생 정보 수정" subtitle="학생 정보를 불러오는 중입니다.">
        <EditSkeleton />
      </DashboardShell>
    );
  }

  return (
    <DashboardShell title={`학생 정보 수정${titleSuffix}`} subtitle="학생 정보를 수정하세요.">
      <form className="space-y-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <TextField label="이름" value={form.name} onChange={handleChange("name")} required />
          <TextField
            label="나이"
            type="number"
            min={1}
            value={form.age}
            onChange={handleChange("age")}
            required
          />
          <TextField label="전화번호" value={form.phoneNumber} onChange={handleChange("phoneNumber")} required />
          <TextField label="부모 연락처" value={form.parentPhone} onChange={handleChange("parentPhone")} required />
          <TextField label="학교명" value={form.schoolName} onChange={handleChange("schoolName")} required />
          <TextField label="학년" value={form.grade} onChange={handleChange("grade")} required />
          <div className="md:col-span-2">
            <CoursePicker
              courses={coursesQuery.data ?? []}
              selectedCourseIds={form.selectedCourseIds}
              onChange={(nextSelection) => setForm((prev) => ({ ...prev, selectedCourseIds: nextSelection }))}
              isLoading={coursesQuery.isLoading}
            />
          </div>
          <Select
            label="담당 조교 (선택)"
            value={form.assistantId}
            onChange={handleSelectChange("assistantId")}
            disabled={assistantsQuery.isLoading}
          >
            <option value="">담당 조교 없음</option>
            {assistants.map((assistant) => (
              <option key={assistant.memberId} value={assistant.memberId}>
                {assistant.name ?? "이름 없음"}
              </option>
            ))}
          </Select>
          <TextField
            label="기본 클리닉 슬롯 ID (선택)"
            placeholder="UUID"
            value={form.defaultClinicSlotId}
            onChange={handleChange("defaultClinicSlotId")}
          />
        </div>

        {clientError ? (
          <p className="rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700">{clientError}</p>
        ) : null}

        <div className="flex flex-wrap justify-end gap-3">
          <Button
            type="button"
            variant="secondary"
            className="h-11 px-4 text-sm"
            onClick={() => router.push("/dashboard/students")}
            disabled={updateMutation.isPending}
          >
            취소
          </Button>
          <Button type="submit" className="h-11 px-5 text-sm" disabled={updateMutation.isPending}>
            {updateMutation.isPending ? "저장 중..." : "저장"}
          </Button>
        </div>
      </form>
    </DashboardShell>
  );
}

function mapResponseToForm(student: StudentProfileResponse): FormState {
  const enrolledCourseIds =
    student.enrolledCourses
      ?.map((course) => course.courseId)
      .filter((courseId): courseId is string => Boolean(courseId)) ?? [];
  return {
    name: student.name ?? "",
    phoneNumber: student.phoneNumber ?? "",
    parentPhone: student.parentPhone ?? "",
    schoolName: student.schoolName ?? "",
    grade: student.grade ?? "",
    age: student.age ? String(student.age) : "",
    assistantId: student.assistantId ?? "",
    defaultClinicSlotId: student.defaultClinicSlotId ?? "",
    selectedCourseIds: enrolledCourseIds
  };
}

function buildUpdatePayload(form: FormState): StudentProfileUpdateRequest | string {
  if (!form.name || !form.phoneNumber || !form.parentPhone || !form.schoolName || !form.grade || !form.age) {
    return "필수 항목을 모두 입력해주세요.";
  }
  if (form.selectedCourseIds.length === 0) {
    return "수업을 하나 이상 선택해주세요.";
  }
  const ageValue = Number(form.age);
  if (Number.isNaN(ageValue) || ageValue <= 0) {
    return "나이는 0보다 큰 숫자로 입력해주세요.";
  }

  return {
    name: form.name,
    phoneNumber: form.phoneNumber,
    parentPhone: form.parentPhone,
    schoolName: form.schoolName,
    grade: form.grade,
    age: ageValue,
    courseIds: form.selectedCourseIds,
    assistantId: form.assistantId || undefined,
    defaultClinicSlotId: form.defaultClinicSlotId || undefined,
    memberId: undefined
  };
}

function EditSkeleton() {
  return (
    <div className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {Array.from({ length: 10 }).map((_, idx) => (
          <div key={idx} className="space-y-2">
            <div className="h-4 w-24 animate-pulse rounded bg-slate-200" />
            <div className="h-11 w-full animate-pulse rounded bg-slate-200" />
          </div>
        ))}
      </div>
      <div className="flex justify-end gap-3">
        <div className="h-11 w-24 animate-pulse rounded bg-slate-200" />
        <div className="h-11 w-24 animate-pulse rounded bg-slate-200" />
      </div>
    </div>
  );
}
