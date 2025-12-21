"use client";

import { Button } from "@/components/ui/button";

type StudentInfo = {
  name: string;
  phoneNumber?: string;
  parentPhoneNumber?: string;
  courses: string[];
};

type StudentInfoCardProps = {
  student: StudentInfo | null;
  onChangeStudent: () => void;
};

export function StudentInfoCard({ student, onChangeStudent }: StudentInfoCardProps) {
  if (!student) {
    return (
      <div className="rounded-3xl border border-dashed border-slate-200 bg-white px-6 py-6 text-sm text-slate-400">
        학생을 선택하면 정보가 표시됩니다.
      </div>
    );
  }

  return (
    <div className="rounded-3xl border border-slate-200 bg-white px-6 py-5 shadow-sm">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold text-slate-900">{student.name}</h3>
            <Button variant="ghost" onClick={onChangeStudent}>
              변경
            </Button>
          </div>
          <p className="text-sm text-slate-600">반: {student.courses.join(", ")}</p>
          <p className="text-sm text-slate-600">
            연락처: {student.phoneNumber ?? "-"} / 보호자: {student.parentPhoneNumber ?? "-"}
          </p>
        </div>
        <div className="flex items-center gap-3 text-sm text-slate-500">
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: "#5B5FED" }} />
            공통 진도
          </span>
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: "#10B981" }} />
            개인 진도
          </span>
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: "#F59E0B" }} />
            클리닉 기록
          </span>
        </div>
      </div>
    </div>
  );
}
