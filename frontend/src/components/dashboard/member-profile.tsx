"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useSession } from "@/components/session/session-provider";
import { Card } from "@/components/ui/card";
import { TextField } from "@/components/ui/text-field";
import { Select } from "@/components/ui/select";
import { DatePicker } from "@/components/ui/date-picker";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { SectionHeading } from "@/components/ui/section-heading";
import { ErrorState } from "@/components/ui/error-state";
import { PasswordRequirementList } from "@/components/ui/password-requirement-list";
import { formatPhoneNumber, validatePhoneNumber } from "@/lib/format-phone";
import { formatStudentGrade } from "@/utils/student";
import type { components } from "@/types/openapi";

type MemberProfileResponse = components["schemas"]["MemberProfileResponse"];
type MemberProfileUpdateRequest = components["schemas"]["MemberProfileUpdateRequest"];
type MemberProfileInfo = components["schemas"]["MemberProfileInfo"];
type StudentInfoResponse = components["schemas"]["StudentInfoResponse"];
type StudentGrade = NonNullable<StudentInfoResponse["grade"]>;

type FormState = {
  email: string;
  name: string;
  phoneNumber: string;
  password: string;
  schoolName: string;
  grade: StudentGrade | "";
  birthDate: string;
  parentPhone: string;
};

const STUDENT_GRADE_VALUES: StudentGrade[] = [
  "ELEMENTARY_1",
  "ELEMENTARY_2",
  "ELEMENTARY_3",
  "ELEMENTARY_4",
  "ELEMENTARY_5",
  "ELEMENTARY_6",
  "MIDDLE_1",
  "MIDDLE_2",
  "MIDDLE_3",
  "HIGH_1",
  "HIGH_2",
  "HIGH_3",
  "GAP_YEAR"
];

const STUDENT_GRADE_OPTIONS: Array<{ value: StudentGrade; label: string }> = STUDENT_GRADE_VALUES.map((value) => ({
  value,
  label: formatStudentGrade(value) || value
}));

const PASSWORD_REGEX =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-={}:;"'`~<>,.?/\\|\[\]]).{8,64}$/;

function buildFormState(member: MemberProfileInfo, studentInfo?: StudentInfoResponse | null): FormState {
  return {
    email: member.email ?? "",
    name: member.name ?? "",
    phoneNumber: member.phoneNumber ? formatPhoneNumber(member.phoneNumber) : "",
    password: "",
    schoolName: studentInfo?.schoolName ?? "",
    grade: studentInfo?.grade ?? "",
    birthDate: studentInfo?.birthDate ?? "",
    parentPhone: studentInfo?.parentPhone ? formatPhoneNumber(studentInfo.parentPhone) : ""
  };
}

export function MemberProfileView() {
  const { refreshSession } = useSession();
  const [profile, setProfile] = useState<MemberProfileResponse | null>(null);
  const [form, setForm] = useState<FormState | null>(null);
  const [initialForm, setInitialForm] = useState<FormState | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const isStudent = profile?.member?.role === "STUDENT";

  const isDirty = useMemo(() => {
    if (!form || !initialForm) {
      return false;
    }
    if (form.password) {
      return true;
    }
    const keys: Array<keyof FormState> = [
      "email",
      "name",
      "phoneNumber",
      "schoolName",
      "grade",
      "birthDate",
      "parentPhone"
    ];
    return keys.some((key) => form[key] !== initialForm[key]);
  }, [form, initialForm]);

  const loadProfile = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const response = await api.GET("/api/v1/members/me");
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "내 정보를 불러오지 못했습니다."));
      }
      const data = response.data?.data;
      if (!data?.member) {
        throw new Error("내 정보를 불러오지 못했습니다.");
      }
      setProfile(data);
      const nextForm = buildFormState(data.member, data.studentInfo);
      setForm(nextForm);
      setInitialForm(nextForm);
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : "내 정보를 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form) {
      return;
    }
    setFormError(null);
    setFieldErrors({});
    setSuccessMessage(null);

    const errors: Record<string, string> = {};
    if (!form.email) errors.email = "이메일을 입력해주세요.";
    if (!form.name) errors.name = "이름을 입력해주세요.";
    if (!form.phoneNumber) errors.phoneNumber = "전화번호를 입력해주세요.";
    if (form.phoneNumber && !validatePhoneNumber(form.phoneNumber)) {
      errors.phoneNumber = "010으로 시작하는 유효한 전화번호를 입력해주세요.";
    }
    if (form.password && !PASSWORD_REGEX.test(form.password)) {
      errors.password = "비밀번호는 영문, 숫자, 특수문자를 포함하고 8~64자여야 합니다.";
    }
    if (isStudent) {
      if (!form.schoolName) errors.schoolName = "학교명을 입력해주세요.";
      if (!form.grade) errors.grade = "학년을 선택해주세요.";
      if (!form.birthDate) errors.birthDate = "생년월일을 선택해주세요.";
      if (!form.parentPhone) errors.parentPhone = "보호자 전화번호를 입력해주세요.";
      if (form.parentPhone && !validatePhoneNumber(form.parentPhone)) {
        errors.parentPhone = "010으로 시작하는 유효한 보호자 번호를 입력해주세요.";
      }
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    try {
      setIsSaving(true);
      const payload: MemberProfileUpdateRequest = {
        email: form.email,
        name: form.name,
        phoneNumber: form.phoneNumber,
        password: form.password || undefined,
        studentInfo: isStudent
          ? {
              schoolName: form.schoolName,
              grade: form.grade || undefined,
              birthDate: form.birthDate || undefined,
              parentPhone: form.parentPhone
            }
          : undefined
      };

      const response = await api.PUT("/api/v1/members/me", { body: payload });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "내 정보 저장에 실패했습니다."));
      }

      const updated = response.data?.data;
      if (!updated?.member) {
        throw new Error("내 정보 저장에 실패했습니다.");
      }

      setProfile(updated);
      const nextForm = buildFormState(updated.member, updated.studentInfo);
      setForm({ ...nextForm, password: "" });
      setInitialForm({ ...nextForm, password: "" });
      setSuccessMessage("내 정보가 저장되었습니다.");
      await refreshSession();
    } catch (error) {
      const message = error instanceof Error ? error.message : "내 정보 저장에 실패했습니다.";
      setFormError(message);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center rounded-2xl border border-slate-200 bg-white">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
      </div>
    );
  }

  if (loadError || !form) {
    return (
      <ErrorState
        title="내 정보를 불러오지 못했습니다."
        description={loadError ?? "잠시 후 다시 시도해주세요."}
        onRetry={loadProfile}
      />
    );
  }

  return (
    <div className="space-y-8">
      <SectionHeading
        eyebrow="My Profile"
        title="내 정보"
        description="연락처와 비밀번호, 학생 정보 등을 최신 상태로 유지해주세요."
      />

      <form className="space-y-6" onSubmit={handleSubmit}>
        <Card title="기본 정보" description="로그인에 사용하는 기본 정보를 수정합니다.">
          <div className="grid gap-4 md:grid-cols-2">
            <TextField
              label="이메일"
              name="email"
              type="email"
              value={form.email}
              onChange={(event) => setForm((prev) => (prev ? { ...prev, email: event.target.value } : prev))}
              error={fieldErrors.email}
              required
            />
            <TextField
              label="이름"
              name="name"
              value={form.name}
              onChange={(event) => setForm((prev) => (prev ? { ...prev, name: event.target.value } : prev))}
              error={fieldErrors.name}
              required
            />
            <TextField
              label="전화번호"
              name="phoneNumber"
              value={form.phoneNumber}
              onChange={(event) =>
                setForm((prev) => (prev ? { ...prev, phoneNumber: formatPhoneNumber(event.target.value) } : prev))
              }
              error={fieldErrors.phoneNumber}
              helperText="숫자만 입력해주세요."
              required
            />
            <div className="space-y-2">
              <TextField
                label="새 비밀번호"
                name="password"
                type={showPassword ? "text" : "password"}
                value={form.password}
                onChange={(event) => setForm((prev) => (prev ? { ...prev, password: event.target.value } : prev))}
                error={fieldErrors.password}
                helperText="입력한 경우에만 비밀번호가 변경됩니다."
                rightElement={
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    className="rounded-full p-1 text-slate-400 transition hover:text-slate-600"
                    aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
                  >
                    {showPassword ? <EyeOffIcon className="h-4 w-4" /> : <EyeIcon className="h-4 w-4" />}
                  </button>
                }
              />
              {form.password ? <PasswordRequirementList password={form.password} /> : null}
            </div>
          </div>
        </Card>

        {isStudent ? (
          <Card title="학생 정보" description="학생 프로필 정보를 수정합니다.">
            <div className="grid gap-4 md:grid-cols-2">
              <TextField
                label="학교명"
                name="schoolName"
                value={form.schoolName}
                onChange={(event) => setForm((prev) => (prev ? { ...prev, schoolName: event.target.value } : prev))}
                error={fieldErrors.schoolName}
                required
              />
              <Select
                label="학년"
                name="grade"
                value={form.grade}
                onChange={(event) =>
                  setForm((prev) => (prev ? { ...prev, grade: event.target.value as StudentGrade } : prev))
                }
                error={fieldErrors.grade}
                required
              >
                <option value="">학년 선택</option>
                {STUDENT_GRADE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
              <div className="space-y-1.5">
                <DatePicker
                  label="생년월일"
                  value={form.birthDate}
                  onChange={(value) => setForm((prev) => (prev ? { ...prev, birthDate: value } : prev))}
                  className="h-12"
                  error={Boolean(fieldErrors.birthDate)}
                  required
                />
                {fieldErrors.birthDate ? (
                  <span className="text-xs font-semibold text-rose-600">{fieldErrors.birthDate}</span>
                ) : null}
              </div>
              <TextField
                label="보호자 연락처"
                name="parentPhone"
                value={form.parentPhone}
                onChange={(event) =>
                  setForm((prev) => (prev ? { ...prev, parentPhone: formatPhoneNumber(event.target.value) } : prev))
                }
                error={fieldErrors.parentPhone}
                helperText="숫자만 입력해주세요."
                required
              />
            </div>
          </Card>
        ) : null}

        {formError ? <InlineError message={formError} /> : null}
        {successMessage ? (
          <div className="rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            {successMessage}
          </div>
        ) : null}

        <div className="flex justify-end">
          <Button type="submit" disabled={!isDirty || isSaving}>
            {isSaving ? "저장 중..." : "저장하기"}
          </Button>
        </div>
      </form>
    </div>
  );
}

function EyeIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function EyeOffIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="M3 5l18 14" />
      <path d="M10.8 10.8a3 3 0 0 0 4.2 4.2" />
      <path d="M9.5 5.5A9.9 9.9 0 0 1 12 5c6.5 0 10 7 10 7a18.7 18.7 0 0 1-4.1 4.9" />
      <path d="M6.3 6.3A18.7 18.7 0 0 0 2 12s3.5 6 10 6a9.9 9.9 0 0 0 5.1-1.4" />
    </svg>
  );
}
