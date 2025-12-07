"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { useCreateStudentProfile } from "@/hooks/use-student-profiles";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type StudentProfileCreateRequest = components["schemas"]["StudentProfileCreateRequest"];

type FormState = {
  name: string;
  phoneNumber: string;
  parentPhone: string;
  schoolName: string;
  grade: string;
  age: string;
  courseId: string;
  assistantId: string;
  memberId?: string;
  defaultClinicSlotId?: string;
};

const initialForm: FormState = {
  name: "",
  phoneNumber: "",
  parentPhone: "",
  schoolName: "",
  grade: "",
  age: "",
  courseId: "",
  assistantId: "",
  memberId: "",
  defaultClinicSlotId: ""
};

export default function StudentCreatePage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const router = useRouter();
  const [form, setForm] = useState<FormState>(initialForm);
  const [clientError, setClientError] = useState<string | null>(null);
  const createMutation = useCreateStudentProfile();

  if (!canRender) {
    return fallback;
  }

  const handleChange = (field: keyof FormState) => (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setClientError(null);

    if (!form.name || !form.phoneNumber || !form.parentPhone || !form.schoolName || !form.grade || !form.age) {
      setClientError("필수 항목을 모두 입력해주세요.");
      return;
    }
    if (!form.courseId || !form.assistantId) {
      setClientError("코스 ID와 조교 ID를 입력해주세요.");
      return;
    }

    const ageValue = Number(form.age);
    if (Number.isNaN(ageValue) || ageValue <= 0) {
      setClientError("나이는 0보다 큰 숫자로 입력해주세요.");
      return;
    }

    const payload: StudentProfileCreateRequest = {
      name: form.name,
      phoneNumber: form.phoneNumber,
      parentPhone: form.parentPhone,
      schoolName: form.schoolName,
      grade: form.grade,
      age: ageValue,
      courseId: form.courseId,
      assistantId: form.assistantId
    };

    if (form.memberId) {
      payload.memberId = form.memberId;
    }
    if (form.defaultClinicSlotId) {
      payload.defaultClinicSlotId = form.defaultClinicSlotId;
    }

    try {
      await createMutation.mutateAsync(payload);
      router.push("/dashboard/students");
    } catch (error) {
      setClientError(getApiErrorMessage(error, "학생 등록에 실패했습니다."));
    }
  };

  return (
    <DashboardShell title="학생 등록" subtitle="새 학생 정보를 입력하고 등록하세요.">
      <form className="space-y-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <TextField label="이름" placeholder="학생 이름" value={form.name} onChange={handleChange("name")} required />
          <TextField
            label="나이"
            type="number"
            min={1}
            placeholder="예: 15"
            value={form.age}
            onChange={handleChange("age")}
            required
          />
          <TextField
            label="전화번호"
            placeholder="숫자와 - 만 입력"
            value={form.phoneNumber}
            onChange={handleChange("phoneNumber")}
            required
          />
          <TextField
            label="부모 연락처"
            placeholder="숫자와 - 만 입력"
            value={form.parentPhone}
            onChange={handleChange("parentPhone")}
            required
          />
          <TextField
            label="학교명"
            placeholder="학교 이름"
            value={form.schoolName}
            onChange={handleChange("schoolName")}
            required
          />
          <TextField
            label="학년"
            placeholder="예: 중2, 고1"
            value={form.grade}
            onChange={handleChange("grade")}
            required
          />
          <TextField
            label="코스 ID"
            placeholder="UUID"
            value={form.courseId}
            onChange={handleChange("courseId")}
            required
          />
          <TextField
            label="담당 조교 ID"
            placeholder="UUID"
            value={form.assistantId}
            onChange={handleChange("assistantId")}
            required
          />
          <TextField
            label="학생 Member ID (선택)"
            placeholder="UUID"
            value={form.memberId}
            onChange={handleChange("memberId")}
          />
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
            disabled={createMutation.isPending}
          >
            취소
          </Button>
          <Button type="submit" className="h-11 px-5 text-sm" disabled={createMutation.isPending}>
            {createMutation.isPending ? "등록 중..." : "학생 등록"}
          </Button>
        </div>
      </form>
    </DashboardShell>
  );
}
