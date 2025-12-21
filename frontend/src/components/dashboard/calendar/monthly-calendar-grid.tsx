"use client";

import clsx from "clsx";

type CalendarDay = {
  key: string;
  date: Date;
  isCurrentMonth: boolean;
};

type DaySummary = {
  course?: {
    title: string;
    extraCount: number;
  };
  personal?: {
    title: string;
    extraCount: number;
  };
  clinic?: {
    title: string;
    extraCount: number;
  };
};

type MonthlyCalendarGridProps = {
  days: CalendarDay[];
  summaries: Record<string, DaySummary>;
  selectedKey: string | null;
  onSelectDate: (key: string) => void;
  monthLabel: string;
  onPrevMonth: () => void;
  onNextMonth: () => void;
  canMovePrev: boolean;
  canMoveNext: boolean;
};

const weekdayLabels = ["월", "화", "수", "목", "금", "토", "일"];

export function MonthlyCalendarGrid({
  days,
  summaries,
  selectedKey,
  onSelectDate,
  monthLabel,
  onPrevMonth,
  onNextMonth,
  canMovePrev,
  canMoveNext
}: MonthlyCalendarGridProps) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-center justify-between">
        <span className="text-base font-semibold text-slate-800">{monthLabel}</span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onPrevMonth}
            disabled={!canMovePrev}
            className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 text-sm text-slate-600 disabled:opacity-40"
          >
            ◀
          </button>
          <button
            type="button"
            onClick={onNextMonth}
            disabled={!canMoveNext}
            className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 text-sm text-slate-600 disabled:opacity-40"
          >
            ▶
          </button>
        </div>
      </div>
      <div className="grid grid-cols-7 gap-2 text-center text-xs font-semibold text-slate-500">
        {weekdayLabels.map((label) => (
          <div key={label}>{label}</div>
        ))}
      </div>
      <div className="mt-3 grid grid-cols-7 gap-2">
        {days.map((day) => {
          const summary = summaries[day.key];
          const isSelected = selectedKey === day.key;
          return (
            <button
              key={day.key}
              type="button"
              onClick={() => (summary ? onSelectDate(day.key) : null)}
              className={clsx(
                "flex h-28 flex-col rounded-2xl border px-2 py-2 text-left transition",
                day.isCurrentMonth ? "border-slate-200" : "border-slate-100 bg-slate-50/60",
                summary ? "hover:border-blue-200 hover:bg-blue-50/40" : "cursor-default",
                isSelected && "border-blue-400 bg-blue-50"
              )}
            >
              <div className="flex h-full flex-col">
                <div className="flex flex-1 items-start">
                  <span className="text-xs font-semibold text-slate-700">{day.date.getDate()}</span>
                </div>
                <SummaryRow summary={summary?.course} color="#5B5FED" />
                <SummaryRow summary={summary?.personal} color="#10B981" />
                <SummaryRow summary={summary?.clinic} color="#F59E0B" />
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

type SummaryRowProps = {
  summary?: {
    title: string;
    extraCount: number;
  };
  color: string;
};

function SummaryRow({ summary, color }: SummaryRowProps) {
  return (
    <div className="flex min-w-0 flex-1 items-stretch gap-2">
      {summary ? (
        <>
          <span className="w-1 rounded-full" style={{ backgroundColor: color }} />
          <div className="flex min-w-0 flex-1 items-center gap-2">
            <span className="min-w-0 flex-1 truncate text-[11px] text-slate-600">{summary.title}</span>
            {summary.extraCount > 0 ? (
              <span className="text-[11px] text-slate-400">+{summary.extraCount}</span>
            ) : null}
          </div>
        </>
      ) : null}
    </div>
  );
}
