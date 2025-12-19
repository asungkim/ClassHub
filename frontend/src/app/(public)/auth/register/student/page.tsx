"use client";

import { useEffect, useState, useMemo } from "react";
import type { Route } from "next";
import { useRouter } from "next/navigation";
import Link from "next/link";
import clsx from "clsx";
import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import { useSession } from "@/components/session/session-provider";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { PasswordRequirementList } from "@/components/ui/password-requirement-list";
import { formatPhoneNumber, validatePhoneNumber } from "@/lib/format-phone";
import { getDashboardRoute } from "@/lib/routes";
import type { components } from "@/types/openapi";

type RegisterStudentRequest = components["schemas"]["RegisterStudentRequest"];
type SchoolLevel = "ELEMENTARY" | "MIDDLE" | "HIGH" | "GAP_YEAR";

const levelOptions: Array<{ label: string; value: SchoolLevel }> = [
  { label: "초등", value: "ELEMENTARY" },
  { label: "중등", value: "MIDDLE" },
  { label: "고등", value: "HIGH" },
  { label: "N수/검정", value: "GAP_YEAR" }
];

const gradeOptionsMap: Record<SchoolLevel, Array<{ label: string; value: RegisterStudentRequest["grade"] }>> = {
  ELEMENTARY: [
    { label: "초1", value: "ELEMENTARY_1" },
    { label: "초2", value: "ELEMENTARY_2" },
    { label: "초3", value: "ELEMENTARY_3" },
    { label: "초4", value: "ELEMENTARY_4" },
    { label: "초5", value: "ELEMENTARY_5" },
    { label: "초6", value: "ELEMENTARY_6" }
  ],
  MIDDLE: [
    { label: "중1", value: "MIDDLE_1" },
    { label: "중2", value: "MIDDLE_2" },
    { label: "중3", value: "MIDDLE_3" }
  ],
  HIGH: [
    { label: "고1", value: "HIGH_1" },
    { label: "고2", value: "HIGH_2" },
    { label: "고3", value: "HIGH_3" }
  ],
  GAP_YEAR: [{ label: "검정 / N수", value: "GAP_YEAR" }]
};

export default function StudentRegisterPage() {
  const router = useRouter();
  const { status, member, setToken } = useSession();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [schoolName, setSchoolName] = useState("");
  const [schoolLevel, setSchoolLevel] = useState<SchoolLevel>("ELEMENTARY");
  const [grade, setGrade] = useState<RegisterStudentRequest["grade"]>("ELEMENTARY_1");
  const [birthYear, setBirthYear] = useState("");
  const [birthMonth, setBirthMonth] = useState("");
  const [birthDay, setBirthDay] = useState("");
  const [parentPhone, setParentPhone] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const gradeOptions = useMemo(() => gradeOptionsMap[schoolLevel], [schoolLevel]);
  const birthYears = useMemo(() => {
    const currentYear = new Date().getFullYear();
    return Array.from({ length: 30 }, (_, index) => String(currentYear - index));
  }, []);
  const birthMonths = Array.from({ length: 12 }, (_, index) => String(index + 1).padStart(2, "0"));
  const birthDays = useMemo(() => {
    if (!birthYear || !birthMonth) return [];
    const daysInMonth = new Date(Number(birthYear), Number(birthMonth), 0).getDate();
    return Array.from({ length: daysInMonth }, (_, index) => String(index + 1).padStart(2, "0"));
  }, [birthYear, birthMonth]);
  const composedBirthDate = birthYear && birthMonth && birthDay ? `${birthYear}-${birthMonth}-${birthDay}` : "";

  // 로그인 상태면 대시보드로
  useEffect(() => {
    if (status === "authenticated" && member?.role) {
      const dashboardRoute = getDashboardRoute(member.role) as Route;
      router.replace(dashboardRoute);
    }
  }, [status, member, router]);

  const handlePhoneInput = (value: string) => {
    setPhone(formatPhoneNumber(value));
  };

  const handleParentPhoneInput = (value: string) => {
    setParentPhone(formatPhoneNumber(value));
  };

  const handleSchoolLevelChange = (level: SchoolLevel) => {
    setSchoolLevel(level);
    const defaultGrade = gradeOptionsMap[level][0]?.value ?? "GAP_YEAR";
    setGrade(defaultGrade);
    setSchoolName((prev) => enforceSchoolSuffix(prev, level));
  };
  const handleSchoolNameBlur = () => {
    setSchoolName((prev) => enforceSchoolSuffix(prev, schoolLevel));
  };

  const isPasswordValid = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-={}:;"'`~<>,.?/\\|\[\]]).{8,64}$/.test(password);
  const passwordsMatch = password === confirmPassword;
  const isPhoneValid = validatePhoneNumber(phone);
  const isParentPhoneValid = validatePhoneNumber(parentPhone);
  const isFormValid =
    email &&
    password &&
    confirmPassword &&
    name &&
    phone &&
    schoolName &&
    grade &&
    composedBirthDate &&
    parentPhone &&
    isPasswordValid &&
    passwordsMatch &&
    isPhoneValid &&
    isParentPhoneValid;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});

    const errors: Record<string, string> = {};
    if (!email) errors.email = "이메일을 입력해주세요.";
    if (!password) errors.password = "비밀번호를 입력해주세요.";
    if (!isPasswordValid) errors.password = "비밀번호는 영문, 숫자, 특수문자를 포함하고 8~64자여야 합니다.";
    if (!confirmPassword) errors.confirmPassword = "비밀번호 확인을 입력해주세요.";
    if (!passwordsMatch) errors.confirmPassword = "비밀번호가 일치하지 않습니다.";
    if (!name) errors.name = "이름을 입력해주세요.";
    if (!phone) errors.phone = "전화번호를 입력해주세요.";
    if (!isPhoneValid) errors.phone = "010으로 시작하는 유효한 전화번호를 입력해주세요.";
    if (!schoolName) errors.schoolName = "학교명을 입력해주세요.";
    if (!grade) errors.grade = "학년을 선택해주세요.";
    if (!composedBirthDate) errors.birthDate = "생년월일을 선택해주세요.";
    if (!parentPhone) errors.parentPhone = "보호자 전화번호를 입력해주세요.";
    if (!isParentPhoneValid) errors.parentPhone = "010으로 시작하는 유효한 보호자 번호를 입력해주세요.";

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      const firstField = Object.keys(errors)[0];
      document.querySelector<HTMLInputElement>(`input[name="${firstField}"]`)?.focus();
      return;
    }

    try {
      setIsSubmitting(true);

      const requestBody: RegisterStudentRequest = {
        email,
        password,
        name,
        phoneNumber: phone,
        schoolName,
        grade,
        birthDate: composedBirthDate,
        parentPhone
      };

      const response = await api.POST("/api/v1/members/register/student", { body: requestBody });

      if (response.error || !response.data?.data?.accessToken) {
        const message = getApiErrorMessage(response.error, "회원가입에 실패했습니다. 다시 시도해주세요.");
        if (message.includes("이메일") || message.includes("DUPLICATE")) {
          setFieldErrors({ email: "이미 가입된 이메일입니다." });
        } else {
          setFormError(message);
        }
        return;
      }

      const accessToken = response.data.data.accessToken;
      await setToken(accessToken);
    } catch (error) {
      const message = error instanceof Error ? error.message : "회원가입 중 오류가 발생했습니다.";
      setFormError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (status === "loading" || status === "authenticated") {
    return (
      <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16">
        <div className="relative z-10 mx-auto flex w-full max-w-6xl items-center justify-center">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
        </div>
      </div>
    );
  }

  return (
    <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 px-4 py-16 text-gray-900">
      <div className="pointer-events-none absolute inset-0">
        <div className="animate-blob animation-delay-0 absolute top-16 left-8 h-72 w-72 rounded-full bg-blue-200 opacity-20 blur-3xl md:opacity-30" />
        <div className="animate-blob animation-delay-2000 absolute top-40 right-4 h-72 w-72 rounded-full bg-purple-200 opacity-20 blur-3xl md:opacity-30" />
        <div className="animate-blob animation-delay-4000 absolute -bottom-10 left-1/2 h-80 w-80 rounded-full bg-pink-200 opacity-20 blur-3xl md:opacity-30" />
      </div>

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-8 lg:flex-row lg:items-center">
        <div className="hidden lg:block lg:flex-1">
          <div className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg">
                <ClassHubIcon />
              </div>
              <div>
                <h1 className="text-4xl font-bold text-gray-900">ClassHub</h1>
                <p className="text-sm text-gray-600">내 수업과 클리닉을 한눈에</p>
              </div>
            </div>
            <div className="space-y-3 text-gray-700">
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>수업/클리닉/과제를 모두 관리</span>
              </div>
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>보호자에게도 안심 정보를 제공</span>
              </div>
              <div className="flex items-center gap-3">
                <CheckCircleIcon className="h-6 w-6 text-emerald-600" />
                <span>모바일에서도 간편한 입력</span>
              </div>
            </div>
          </div>
        </div>

        <div className="w-full lg:flex-1">
          <div className="relative w-full max-w-2xl mx-auto rounded-3xl bg-white/85 p-8 shadow-2xl ring-1 ring-white/60 backdrop-blur">
            <div className="mb-6 text-center lg:hidden">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg">
                <ClassHubIcon />
              </div>
              <h2 className="text-2xl font-bold text-gray-900">학생 회원가입</h2>
            </div>

            <div className="mb-6">
              <h2 className="text-2xl font-bold text-gray-900">학생 회원가입</h2>
              <p className="mt-1 text-sm text-gray-600">내 수업 시간표와 클리닉을 한눈에 확인하세요.</p>
            </div>

            {formError && (
              <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
      <section className="grid gap-4 md:grid-cols-2">
        <TextField
          label="이메일"
                  type="email"
                  name="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="student@classhub.com"
                  error={fieldErrors.email}
                  required
                />
                <TextField
                  label="이름"
                  type="text"
                  name="name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="홍길동"
                  error={fieldErrors.name}
                  required
                />
                <TextField
                  label="비밀번호"
                  type="password"
                  name="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  error={fieldErrors.password}
                  required
                />
              <TextField
                label="비밀번호 확인"
                type="password"
                name="confirmPassword"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                error={fieldErrors.confirmPassword}
                required
              />
              {password && (
                <div className="md:col-span-2 rounded-xl border border-slate-200 bg-slate-50 p-3">
                  <p className="mb-2 text-xs font-semibold text-slate-700">비밀번호 요구사항</p>
                  <PasswordRequirementList password={password} />
                </div>
              )}
              {confirmPassword && password && passwordsMatch && (
                <div className="md:col-span-2 flex items-center gap-2 text-xs font-medium text-emerald-600">
                  <CheckCircleIcon className="h-4 w-4" />
                  비밀번호가 일치합니다.
                </div>
              )}
                <TextField
                  label="학생 전화번호"
                  type="tel"
                  name="phone"
                  value={phone}
                  onChange={(e) => handlePhoneInput(e.target.value)}
                  placeholder="01012345678"
                  error={fieldErrors.phone}
                  inputMode="numeric"
                  pattern="[0-9-]*"
                  maxLength={13}
                  required
                />
                <TextField
                  label="학교명"
                  type="text"
                  name="schoolName"
                  value={schoolName}
                onChange={(e) => setSchoolName(e.target.value)}
                onBlur={handleSchoolNameBlur}
                placeholder="대치중"
                error={fieldErrors.schoolName}
                required
              />
            </section>

              <section className="space-y-4">
                <div>
                  <p className="text-sm font-semibold text-slate-800">학년 선택</p>
                  <p className="text-xs text-slate-500">학교 단계를 선택하면 해당 학년이 노출됩니다.</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {levelOptions.map((level) => (
                      <button
                        key={level.value}
                        type="button"
                        onClick={() => handleSchoolLevelChange(level.value)}
                        className={clsx(
                          "rounded-2xl px-4 py-2 text-sm font-semibold transition",
                          schoolLevel === level.value
                            ? "bg-slate-900 text-white shadow"
                            : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        )}
                      >
                        {level.label}
                      </button>
                    ))}
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {gradeOptions.map((option) => (
                      <button
                        key={option.value}
                        type="button"
                        onClick={() => setGrade(option.value)}
                        className={clsx(
                          "rounded-2xl px-4 py-2 text-sm font-semibold transition",
                          grade === option.value
                            ? "bg-blue-100 text-blue-700 shadow-inner"
                            : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        )}
                      >
                        {option.label}
                      </button>
                    ))}
                  </div>
                  {fieldErrors.grade && <p className="mt-2 text-xs text-rose-600">{fieldErrors.grade}</p>}
                </div>

                <div>
                  <p className="text-sm font-semibold text-slate-800">생년월일</p>
                  <div className="mt-2 grid gap-3 sm:grid-cols-3">
                    <select
                      value={birthYear}
                      onChange={(event) => setBirthYear(event.target.value)}
                      className="rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-700 shadow-inner focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                    >
                      <option value="">년</option>
                      {birthYears.map((year) => (
                        <option key={year} value={year}>
                          {year}
                        </option>
                      ))}
                    </select>
                    <select
                      value={birthMonth}
                      onChange={(event) => setBirthMonth(event.target.value)}
                      className="rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-700 shadow-inner focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                    >
                      <option value="">월</option>
                      {birthMonths.map((month) => (
                        <option key={month} value={month}>
                          {Number(month)}월
                        </option>
                      ))}
                    </select>
                    <select
                      value={birthDay}
                      onChange={(event) => setBirthDay(event.target.value)}
                      className="rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-700 shadow-inner focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                      disabled={!birthDays.length}
                    >
                      <option value="">일</option>
                      {birthDays.map((day) => (
                        <option key={day} value={day}>
                          {Number(day)}일
                        </option>
                      ))}
                    </select>
                  </div>
                  {fieldErrors.birthDate && <p className="mt-1 text-xs text-rose-600">{fieldErrors.birthDate}</p>}
                </div>
              </section>

              <section className="grid gap-4 md:grid-cols-2">
                <TextField
                  label="보호자 전화번호"
                  type="tel"
                  name="parentPhone"
                  value={parentPhone}
                  onChange={(e) => handleParentPhoneInput(e.target.value)}
                  placeholder="01098765432"
                  error={fieldErrors.parentPhone}
                  inputMode="numeric"
                  pattern="[0-9-]*"
                  maxLength={13}
                  required
                />
              </section>

              <Button
                type="submit"
                disabled={!isFormValid || isSubmitting}
                className="w-full"
                leftIcon={isSubmitting ? <SpinnerIcon className="h-5 w-5 animate-spin" /> : undefined}
              >
                {isSubmitting ? "가입 중..." : "학생 회원가입"}
              </Button>

              <p className="text-center text-sm text-gray-600">
                이미 계정이 있나요?{" "}
                <Link href="/" className="font-semibold text-blue-600 hover:text-blue-700">
                  로그인
                </Link>
              </p>
            </form>
          </div>
        </div>
      </div>

      <style jsx>{`
        @keyframes blob {
          0% {
            transform: translate(0px, 0px) scale(1);
          }
          33% {
            transform: translate(30px, -50px) scale(1.1);
          }
          66% {
            transform: translate(-20px, 20px) scale(0.9);
          }
          100% {
            transform: translate(0px, 0px) scale(1);
          }
        }
        .animate-blob {
          animation: blob 8s infinite;
        }
        .animation-delay-2000 {
          animation-delay: 2s;
        }
        .animation-delay-4000 {
          animation-delay: 4s;
        }
      `}</style>
    </div>
  );
}

function ClassHubIcon() {
  return (
    <svg viewBox="0 0 48 48" fill="none" className="h-8 w-8">
      <rect x="4" y="4" width="40" height="40" rx="8" className="fill-white/20" />
      <path
        d="M14 18h20M14 24h12M14 30h20"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function CheckCircleIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <circle cx="12" cy="12" r="10" />
      <polyline points="9 12 11 14 15 10" />
    </svg>
  );
}

function SpinnerIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" {...props}>
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.37 0 0 5.37 0 12h4Z" />
    </svg>
  );
}

function enforceSchoolSuffix(name: string, level: SchoolLevel): string {
  const trimmed = name.trim();
  if (!trimmed) return "";

  // 사용자가 '대치고등학교'처럼 입력하면 '대치고'까지만 남긴다.
  const suffixMatch = trimmed.match(/^(.*?(초|중|고))/u);
  if (suffixMatch) {
    return suffixMatch[1];
  }

  if (trimmed.endsWith("초") || trimmed.endsWith("중") || trimmed.endsWith("고")) {
    return trimmed;
  }

  if (level === "ELEMENTARY") return `${trimmed}초`;
  if (level === "MIDDLE") return `${trimmed}중`;
  if (level === "HIGH") return `${trimmed}고`;
  return trimmed;
}
