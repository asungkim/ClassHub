"use client";

import type { RefObject } from "react";

type StudentSearchOption = {
  studentId: string;
  name: string;
  courses: string[];
};

type StudentCalendarHeaderProps = {
  searchValue: string;
  searchLoading: boolean;
  searchResults: StudentSearchOption[];
  onSearchChange: (value: string) => void;
  onSelectStudent: (option: StudentSearchOption) => void;
  inputRef: RefObject<HTMLInputElement | null>;
};

export function StudentCalendarHeader({
  searchValue,
  searchLoading,
  searchResults,
  onSearchChange,
  onSelectStudent,
  inputRef
}: StudentCalendarHeaderProps) {
  return (
    <div className="space-y-4">
      <div className="rounded-3xl border border-slate-200 bg-white px-6 py-5 shadow-sm">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary/70">Student Calendar</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">학생별 캘린더</h2>
          <p className="mt-1 text-sm text-slate-500">
            학생별 캘린더 학습 데이터를 조회하고 관리할 수 있습니다.
          </p>
        </div>
      </div>

      <div className="relative rounded-3xl border border-slate-200 bg-white px-6 py-5 shadow-sm">
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-slate-700">학생 검색</span>
          <input
            ref={inputRef}
            className="h-12 rounded-xl border border-slate-200 px-4 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-200"
            placeholder="학생 이름을 입력하세요"
            value={searchValue}
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </label>
        {searchLoading ? <p className="mt-2 text-xs text-slate-400">검색 중...</p> : null}
        {searchValue.trim().length > 0 && searchResults.length > 0 ? (
          <div className="absolute left-6 right-6 top-full z-20 mt-2 max-h-60 overflow-y-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-xl">
            {searchResults.map((option) => (
              <button
                key={option.studentId}
                type="button"
                onClick={() => onSelectStudent(option)}
                className="w-full rounded-xl px-4 py-3 text-left text-sm hover:bg-slate-50"
              >
                <p className="font-semibold text-slate-900">{option.name}</p>
                <p className="text-xs text-slate-500">{option.courses.join(", ")}</p>
              </button>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}
