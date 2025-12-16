"use client";

import { useEffect, useState, type ReactNode, type PointerEvent } from "react";
import clsx from "clsx";
import type { components } from "@/types/openapi";
import {
  DAY_LABELS,
  DAY_OPTIONS,
  DayOfWeekLiteral,
  TIME_RANGE_START,
  TIME_RANGE_END,
  formatHour,
  generateTimeSlots,
  timeToMinutes
} from "@/components/clinic/day-utils";

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];

const DAYS: DayOfWeekLiteral[] = DAY_OPTIONS.map((day) => day.value);
const TIME_SLOTS = generateTimeSlots();
const DEFAULT_ROW_HEIGHT = 64;
const MOBILE_ROW_HEIGHT = 52;
const MOBILE_BREAKPOINT = 768;

interface ClinicSlotGridProps {
  slots: ClinicSlotResponse[];
  onSlotClick?: (slot: ClinicSlotResponse) => void;
  onEmptySlotClick?: (day: DayOfWeekLiteral, time: string) => void;
  onCreateClick?: () => void;
  onSelectionComplete?: (day: DayOfWeekLiteral, startTime: string, endTime: string) => void;
}

type SelectionState = {
  day: DayOfWeekLiteral;
  startIndex: number;
  endIndex: number;
};

export function ClinicSlotGrid({
  slots,
  onSlotClick,
  onEmptySlotClick,
  onCreateClick,
  onSelectionComplete
}: ClinicSlotGridProps) {
  const [selection, setSelection] = useState<SelectionState | null>(null);
  const [isSelecting, setIsSelecting] = useState(false);
  const [rowHeight, setRowHeight] = useState(DEFAULT_ROW_HEIGHT);

  const beginSelection = (day: DayOfWeekLiteral, index: number) => {
    setSelection({ day, startIndex: index, endIndex: index });
    setIsSelecting(true);
  };

  const extendSelection = (day: DayOfWeekLiteral, index: number) => {
    if (!isSelecting || !selection || selection.day !== day) return;
    setSelection({
      day,
      startIndex: Math.min(selection.startIndex, index),
      endIndex: Math.max(selection.endIndex, index)
    });
  };

  const resetSelection = () => {
    setSelection(null);
    setIsSelecting(false);
  };

  const finalizeSelection = () => {
    if (!selection) {
      resetSelection();
      return;
    }
    const startIndex = Math.min(selection.startIndex, selection.endIndex);
    const endIndex = Math.max(selection.startIndex, selection.endIndex);
    const startTime = getStartTimeFromIndex(startIndex);
    const endTime = getEndTimeFromIndex(endIndex + 1);
    if (onSelectionComplete) {
      onSelectionComplete(selection.day, startTime, endTime);
    } else if (onEmptySlotClick) {
      onEmptySlotClick(selection.day, startTime);
    }
    resetSelection();
  };

  useEffect(() => {
    if (!isSelecting) return;
    const handlePointerUp = () => finalizeSelection();
    const handlePointerCancel = () => resetSelection();
    window.addEventListener("pointerup", handlePointerUp);
    window.addEventListener("pointercancel", handlePointerCancel);
    return () => {
      window.removeEventListener("pointerup", handlePointerUp);
      window.removeEventListener("pointercancel", handlePointerCancel);
    };
  }, [isSelecting, selection]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const updateRowHeight = () => {
      if (typeof window === "undefined") return;
      const nextHeight = window.innerWidth < MOBILE_BREAKPOINT ? MOBILE_ROW_HEIGHT : DEFAULT_ROW_HEIGHT;
      setRowHeight(nextHeight);
    };
    updateRowHeight();
    window.addEventListener("resize", updateRowHeight);
    return () => window.removeEventListener("resize", updateRowHeight);
  }, []);

  const cellPointerDown = (day: DayOfWeekLiteral, rowIndex: number) => {
    beginSelection(day, rowIndex);
  };

  const cellPointerEnter = (day: DayOfWeekLiteral, rowIndex: number) => {
    extendSelection(day, rowIndex);
  };

  const spanTracker: Record<DayOfWeekLiteral, number> = {} as Record<DayOfWeekLiteral, number>;
  DAYS.forEach((day) => {
    spanTracker[day] = 0;
  });

  const rows = TIME_SLOTS.map((time, rowIndex) => {
    const cells: ReactNode[] = [];
    DAYS.forEach((day) => {
      const remainingSpan = spanTracker[day];
      if (remainingSpan > 0) {
        spanTracker[day] = remainingSpan - 1;
        return;
      }

      const slot = findSlotStartingAt(slots, day, time);
      if (slot) {
        const span = calculateRowSpan(slot);
        spanTracker[day] = span - 1;
        cells.push(
          <td
            key={day + "-" + time}
            rowSpan={span}
            className="border-r border-slate-200 p-1 align-top last:border-r-0"
          >
            <ClinicSlotBlock slot={slot} rowSpan={span} rowHeight={rowHeight} onClick={() => onSlotClick?.(slot)} />
          </td>
        );
        return;
      }

      const isSelected =
        !!selection &&
        selection.day === day &&
        rowIndex >= Math.min(selection.startIndex, selection.endIndex) &&
        rowIndex <= Math.max(selection.startIndex, selection.endIndex);

      cells.push(
        <td
          key={day + "-" + time}
          className="border-r border-slate-200 p-1 last:border-r-0"
          style={{ height: rowHeight }}
        >
          <EmptyTimeSlot
            isSelected={isSelected}
            rowHeight={rowHeight}
            onPointerDown={(event) => {
              event.preventDefault();
              cellPointerDown(day, rowIndex);
            }}
            onPointerEnter={(event) => {
              event.preventDefault();
              cellPointerEnter(day, rowIndex);
            }}
            onPointerUp={(event) => {
              event.preventDefault();
              finalizeSelection();
            }}
            onClick={() => {
              if (!isSelecting) {
                onEmptySlotClick?.(day, time);
              }
            }}
          />
        </td>
      );
    });

    return (
      <tr
        key={time}
        className="border-b border-slate-200 last:border-b-0"
        style={{ height: rowHeight }}
      >
        <td className="sticky left-0 z-10 border-r border-slate-200 bg-slate-50 p-2 text-xs text-slate-600 sm:p-3 sm:text-sm">
          {time}
        </td>
        {cells}
      </tr>
    );
  });

  const boundaryHeight = Math.max(40, rowHeight / 1.5);
  const lastHourStart = formatHour(Math.max(TIME_RANGE_START, TIME_RANGE_END - 1));
  rows.push(
    <tr key="boundary-22" className="border-t border-slate-200" style={{ height: boundaryHeight }}>
      <td className="sticky left-0 z-10 border-r border-slate-200 bg-slate-50 p-2 text-xs text-slate-600 sm:p-3 sm:text-sm">
        {formatHour(TIME_RANGE_END)}
      </td>
      {DAYS.map((day) => (
        <td key={`boundary-${day}`} className="border-r border-slate-200 p-1 last:border-r-0">
          <AddSlotButton
            rowHeight={boundaryHeight}
            onClick={() => onEmptySlotClick?.(day, lastHourStart)}
            disabled={!onEmptySlotClick}
          />
        </td>
      ))}
    </tr>
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-base font-semibold text-slate-900 sm:text-lg">주간 시간표</h2>
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            매주 토요일 23시에 다음 주 세션 자동 생성
          </span>
        </div>
        <button
          onClick={onCreateClick}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90"
        >
          + 슬롯 추가
        </button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50">
              <th className="sticky left-0 z-10 w-20 border-r border-slate-200 bg-slate-50 p-2 text-left text-xs font-medium text-slate-600 sm:p-3 sm:text-sm">
                시간
              </th>
              {DAYS.map((day) => (
                <th
                  key={day}
                  className="min-w-[96px] border-r border-slate-200 p-2 text-center text-xs font-medium text-slate-600 last:border-r-0 sm:min-w-[120px] sm:p-3 sm:text-sm"
                >
                  {DAY_LABELS[day]}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>{rows}</tbody>
        </table>
      </div>
    </div>
  );
}

function findSlotStartingAt(slots: ClinicSlotResponse[], day: DayOfWeekLiteral, time: string) {
  return slots.find((slot) => {
    if (slot.dayOfWeek !== day) return false;
    const start = slot.startTime?.slice(0, 5) ?? "";
    return start === time;
  });
}

function calculateRowSpan(slot: ClinicSlotResponse) {
  const start = slot.startTime ? timeToMinutes(slot.startTime.slice(0, 5)) : 0;
  const end = slot.endTime ? timeToMinutes(slot.endTime.slice(0, 5)) : start + 60;
  const diff = Math.max(60, end - start);
  return Math.min(TIME_RANGE_END - TIME_RANGE_START, Math.ceil(diff / 60));
}

interface ClinicSlotBlockProps {
  slot: ClinicSlotResponse;
  rowSpan: number;
  rowHeight: number;
  onClick?: () => void;
}

function ClinicSlotBlock({ slot, onClick, rowSpan, rowHeight }: ClinicSlotBlockProps) {
  const isActive = slot.isActive !== false;
  const bgColor = isActive
    ? "bg-gradient-to-br from-blue-50 via-blue-100 to-blue-200 text-blue-900 hover:from-blue-100 hover:to-blue-300"
    : "bg-slate-100 text-slate-500 hover:bg-slate-200";
  const dotColor = isActive ? "bg-blue-500/80" : "bg-slate-400/80";
  const borderColor = isActive ? "border-blue-200/70" : "border-slate-200";
  const textColor = isActive ? "text-slate-900" : "text-slate-500";
  
  return (
    <button
      onClick={onClick}
      style={{ minHeight: rowSpan * rowHeight }}
      className={`flex h-full w-full flex-col justify-between rounded-2xl border p-3 text-left text-xs shadow-sm transition-colors sm:text-sm ${bgColor} ${borderColor}`}
    >
      <div className={`flex items-center gap-2 ${textColor} text-xs font-semibold`}>
        <span className={`h-2.5 w-2.5 rounded-full shadow-inner ${dotColor}`} />
        {slot.startTime?.slice(0, 5)} - {slot.endTime?.slice(0, 5)}
      </div>
      {!isActive ? (
        <div className="text-[11px] font-medium text-slate-500">비활성 슬롯</div>
      ) : (
        <div className="flex justify-end text-[11px] text-slate-600 sm:text-xs">
          <span className="flex items-center gap-1 rounded-full bg-white/70 px-2 py-0.5 text-[11px] font-medium text-slate-700 shadow-sm">
            <svg className="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            {slot.capacity}
          </span>
        </div>
      )}
    </button>
  );
}

interface EmptyTimeSlotProps {
  isSelected: boolean;
  rowHeight: number;
  onPointerDown: (event: PointerEvent<HTMLButtonElement>) => void;
  onPointerEnter: (event: PointerEvent<HTMLButtonElement>) => void;
  onPointerUp: (event: PointerEvent<HTMLButtonElement>) => void;
  onClick?: () => void;
}

function EmptyTimeSlot({ isSelected, rowHeight, onPointerDown, onPointerEnter, onPointerUp, onClick }: EmptyTimeSlotProps) {
  return (
    <button
      onPointerDown={onPointerDown}
      onPointerEnter={onPointerEnter}
      onPointerUp={onPointerUp}
      onClick={onClick}
      style={{ minHeight: Math.max(40, rowHeight - 8) }}
      className={clsx(
        "group h-full w-full rounded p-2 text-center transition-colors",
        isSelected ? "bg-blue-50 text-blue-600" : "hover:bg-slate-50"
      )}
    >
      <div
        className={clsx(
          "flex h-full items-center justify-center transition-colors",
          isSelected ? "text-blue-600" : "text-slate-300 group-hover:text-slate-400"
        )}
      >
        <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
        </svg>
      </div>
    </button>
  );
}

function AddSlotButton({ rowHeight, onClick, disabled }: { rowHeight: number; onClick?: () => void; disabled?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      style={{ minHeight: Math.max(40, rowHeight - 8) }}
      className={clsx(
        "group h-full w-full rounded p-2 text-center transition-colors",
        disabled ? "bg-slate-50 text-slate-300" : "hover:bg-slate-50 text-slate-300 hover:text-slate-400"
      )}
    >
      <div className="flex h-full items-center justify-center">
        <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
        </svg>
      </div>
    </button>
  );
}

function getStartTimeFromIndex(index: number) {
  const hour = TIME_RANGE_START + index;
  return formatHour(Math.min(hour, TIME_RANGE_END - 1));
}

function getEndTimeFromIndex(boundaryIndex: number) {
  const clamped = Math.min(boundaryIndex, TIME_RANGE_END - TIME_RANGE_START);
  const hour = TIME_RANGE_START + clamped;
  return formatHour(hour);
}
