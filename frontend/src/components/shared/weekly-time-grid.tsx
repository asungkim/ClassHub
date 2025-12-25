"use client";

import { CSSProperties, ReactNode, useEffect, useMemo, useRef, useState } from "react";
import clsx from "clsx";

export type WeekDay = {
  key: string;
  label: string;
};

export type TimeRange = {
  startTime?: string | null;
  endTime?: string | null;
};

export type SelectionRange = {
  dayKey: string;
  date?: Date;
  startTime: string;
  endTime: string;
};

export type RenderItemArgs<T> = {
  item: T;
  style: CSSProperties;
};

export type WeeklyTimeGridProps<T> = {
  days: ReadonlyArray<WeekDay>;
  itemsByDay: Record<string, T[]>;
  startHour: number;
  endHour: number;
  hourHeight?: number;
  rangeStart?: Date;
  showDateHeader?: boolean;
  selectionEnabled?: boolean;
  selectionStepMinutes?: number;
  onSelectRange?: (range: SelectionRange) => void;
  getItemRange: (item: T) => TimeRange | null;
  getItemKey: (item: T, index: number) => string;
  renderItem: (args: RenderItemArgs<T>) => ReactNode;
};

const DEFAULT_HOUR_HEIGHT = 56;
const LONG_PRESS_MS = 350;

export const WEEK_DAYS: WeekDay[] = [
  { key: "MONDAY", label: "월" },
  { key: "TUESDAY", label: "화" },
  { key: "WEDNESDAY", label: "수" },
  { key: "THURSDAY", label: "목" },
  { key: "FRIDAY", label: "금" },
  { key: "SATURDAY", label: "토" },
  { key: "SUNDAY", label: "일" }
];

export function WeeklyTimeGrid<T>({
  days,
  itemsByDay,
  startHour,
  endHour,
  hourHeight = DEFAULT_HOUR_HEIGHT,
  rangeStart,
  showDateHeader = Boolean(rangeStart),
  selectionEnabled = false,
  selectionStepMinutes = 30,
  onSelectRange,
  getItemRange,
  getItemKey,
  renderItem
}: WeeklyTimeGridProps<T>) {
  const templateColumns = `80px repeat(${days.length}, minmax(0, 1fr))`;
  const totalHours = endHour - startHour;
  const columnHeight = totalHours * hourHeight;

  return (
    <div className="overflow-x-auto rounded-3xl border border-slate-200 bg-white shadow-inner">
      <div className="min-w-[960px]">
        <div className="grid border-b border-slate-200" style={{ gridTemplateColumns: templateColumns }}>
          <div className="flex h-14 items-center justify-center border-r border-slate-200 bg-slate-50 text-xs font-semibold text-slate-500">
            시간
          </div>
          {days.map((day, index) => {
            const headerDate = rangeStart ? formatHeaderDate(addDays(rangeStart, index)) : null;
            return (
              <div
                key={day.key}
                className="flex h-14 flex-col items-center justify-center border-r border-slate-200 text-sm font-semibold text-slate-900"
              >
                <span>{day.label}</span>
                {showDateHeader && headerDate ? (
                  <span className="text-xs font-normal text-slate-500">{headerDate}</span>
                ) : null}
              </div>
            );
          })}
        </div>
        <div className="grid" style={{ gridTemplateColumns: templateColumns }}>
          <WeeklyTimeColumn startHour={startHour} endHour={endHour} hourHeight={hourHeight} />
          {days.map((day, index) => (
            <WeeklyDayColumn
              key={day.key}
              day={day}
              date={rangeStart ? addDays(rangeStart, index) : undefined}
              items={itemsByDay[day.key] ?? []}
              startHour={startHour}
              endHour={endHour}
              hourHeight={hourHeight}
              selectionEnabled={selectionEnabled}
              selectionStepMinutes={selectionStepMinutes}
              onSelectRange={onSelectRange}
              getItemRange={getItemRange}
              getItemKey={getItemKey}
              renderItem={renderItem}
              columnHeight={columnHeight}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

type WeeklyTimeColumnProps = {
  startHour: number;
  endHour: number;
  hourHeight: number;
};

export function WeeklyTimeColumn({ startHour, endHour, hourHeight }: WeeklyTimeColumnProps) {
  const hours = Array.from({ length: endHour - startHour + 1 }, (_, index) => startHour + index);
  const columnHeight = (endHour - startHour) * hourHeight;

  return (
    <div className="border-r border-slate-200 bg-slate-50" style={{ height: columnHeight }}>
      {hours.map((hour, index) => (
        <div
          key={hour}
          className="relative flex items-start justify-end pr-3"
          style={{ height: index === hours.length - 1 ? hourHeight / 2 : hourHeight }}
        >
          <span className="text-[11px] font-semibold text-slate-500">{formatHourLabel(hour)}</span>
        </div>
      ))}
    </div>
  );
}

type WeeklyDayColumnProps<T> = {
  day: WeekDay;
  date?: Date;
  items: T[];
  startHour: number;
  endHour: number;
  hourHeight: number;
  columnHeight: number;
  selectionEnabled: boolean;
  selectionStepMinutes: number;
  onSelectRange?: (range: SelectionRange) => void;
  getItemRange: (item: T) => TimeRange | null;
  getItemKey: (item: T, index: number) => string;
  renderItem: (args: RenderItemArgs<T>) => ReactNode;
};

function WeeklyDayColumn<T>({
  day,
  date,
  items,
  startHour,
  endHour,
  hourHeight,
  columnHeight,
  selectionEnabled,
  selectionStepMinutes,
  onSelectRange,
  getItemRange,
  getItemKey,
  renderItem
}: WeeklyDayColumnProps<T>) {
  const columnRef = useRef<HTMLDivElement>(null);
  const [selection, setSelection] = useState<{ start: number; end: number } | null>(null);
  const [isSelecting, setIsSelecting] = useState(false);
  const pressTimerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (pressTimerRef.current) {
        window.clearTimeout(pressTimerRef.current);
      }
    };
  }, []);

  const hourLines = useMemo(
    () => Array.from({ length: endHour - startHour }, (_, index) => index + 1),
    [endHour, startHour]
  );

  const getMinutesFromClientY = (clientY: number) => {
    const rect = columnRef.current?.getBoundingClientRect();
    if (!rect) return null;
    const offset = Math.min(Math.max(clientY - rect.top, 0), columnHeight);
    const rawMinutes = startHour * 60 + (offset / hourHeight) * 60;
    return rawMinutes;
  };

  const snapMinutes = (value: number) => {
    const snapped = Math.round(value / selectionStepMinutes) * selectionStepMinutes;
    const minMinutes = startHour * 60;
    const maxMinutes = endHour * 60;
    return Math.min(Math.max(snapped, minMinutes), maxMinutes);
  };

  const updateSelection = (clientY: number) => {
    const minutes = getMinutesFromClientY(clientY);
    if (minutes === null) return;
    const snapped = snapMinutes(minutes);
    setSelection((prev) => {
      if (!prev) {
        return { start: snapped, end: snapped };
      }
      return { ...prev, end: snapped };
    });
  };

  const startSelection = (clientY: number, pointerId: number, target: HTMLDivElement) => {
    const minutes = getMinutesFromClientY(clientY);
    if (minutes === null) return;
    const snapped = snapMinutes(minutes);
    setSelection({ start: snapped, end: snapped });
    setIsSelecting(true);
    target.setPointerCapture(pointerId);
  };

  const finishSelection = (target: HTMLDivElement, pointerId: number) => {
    if (!selection || !onSelectRange) {
      setSelection(null);
      setIsSelecting(false);
      target.releasePointerCapture(pointerId);
      return;
    }

    const start = Math.min(selection.start, selection.end);
    const end = Math.max(selection.start, selection.end);
    const minDuration = selectionStepMinutes;
    const boundedEnd = end === start ? Math.min(start + minDuration, endHour * 60) : end;

    const range: SelectionRange = {
      dayKey: day.key,
      date,
      startTime: formatMinutesToTime(start),
      endTime: formatMinutesToTime(boundedEnd)
    };

    setSelection(null);
    setIsSelecting(false);
    target.releasePointerCapture(pointerId);
    onSelectRange(range);
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!selectionEnabled || !onSelectRange) {
      return;
    }
    const eventTarget = event.target;
    if (eventTarget instanceof Element && eventTarget.closest("[data-weekly-grid-item]")) {
      return;
    }
    if (event.pointerType === "mouse" && event.button !== 0) {
      return;
    }
    const currentTarget = event.currentTarget;
    event.preventDefault();

    if (event.pointerType === "mouse") {
      startSelection(event.clientY, event.pointerId, currentTarget);
      return;
    }

    if (pressTimerRef.current) {
      window.clearTimeout(pressTimerRef.current);
    }

    pressTimerRef.current = window.setTimeout(() => {
      startSelection(event.clientY, event.pointerId, currentTarget);
    }, LONG_PRESS_MS);
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!isSelecting) {
      return;
    }
    event.preventDefault();
    updateSelection(event.clientY);
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    if (pressTimerRef.current) {
      window.clearTimeout(pressTimerRef.current);
      pressTimerRef.current = null;
    }

    if (!isSelecting) {
      return;
    }
    finishSelection(event.currentTarget, event.pointerId);
  };

  const handlePointerCancel = (event: React.PointerEvent<HTMLDivElement>) => {
    if (pressTimerRef.current) {
      window.clearTimeout(pressTimerRef.current);
      pressTimerRef.current = null;
    }
    setSelection(null);
    setIsSelecting(false);
    event.currentTarget.releasePointerCapture(event.pointerId);
  };

  const selectionBlockStyle = useMemo(() => {
    if (!selection) return null;
    const start = Math.min(selection.start, selection.end);
    const end = Math.max(selection.start, selection.end);
    const top = ((start - startHour * 60) / 60) * hourHeight;
    const height = Math.max(((end - start) / 60) * hourHeight, hourHeight * 0.4);
    return { top, height };
  }, [hourHeight, selection, startHour]);

  return (
    <div
      ref={columnRef}
      className={clsx(
        "relative border-r border-slate-200",
        selectionEnabled ? "touch-none" : ""
      )}
      style={{ height: columnHeight }}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerCancel}
    >
      {hourLines.map((line) => (
        <div
          key={line}
          className="absolute left-0 right-0 border-b border-slate-100"
          style={{ top: line * hourHeight, height: 0 }}
        />
      ))}
      {items.length === 0 ? (
        <p className="pointer-events-none absolute inset-0 flex items-center justify-center text-xs text-slate-200">-</p>
      ) : null}
      {items.map((item, index) => {
        const range = getItemRange(item);
        if (!range?.startTime || !range.endTime) {
          return null;
        }
        const startMinutes = timeStringToMinutes(range.startTime);
        const endMinutes = timeStringToMinutes(range.endTime);
        if (startMinutes === null || endMinutes === null) {
          return null;
        }
        const minMinutes = startHour * 60;
        const maxMinutes = endHour * 60;
        const clampedStart = Math.max(minMinutes, Math.min(startMinutes, maxMinutes));
        const clampedEnd = Math.max(clampedStart + selectionStepMinutes, Math.min(endMinutes, maxMinutes));
        const blockTop = ((clampedStart - minMinutes) / 60) * hourHeight;
        const blockHeight = Math.max(((clampedEnd - clampedStart) / 60) * hourHeight, hourHeight * 0.6);
        const style: CSSProperties = {
          top: blockTop,
          height: blockHeight,
          minHeight: hourHeight * 0.6
        };
        return (
          <div key={getItemKey(item, index)} data-weekly-grid-item>
            {renderItem({ item, style })}
          </div>
        );
      })}

      {selectionBlockStyle && isSelecting ? (
        <div
          className="absolute left-1 right-1 rounded-2xl border border-blue-200 bg-blue-100/60"
          style={selectionBlockStyle}
        />
      ) : null}
    </div>
  );
}

function formatHeaderDate(date: Date) {
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function formatHourLabel(hour: number) {
  return `${String(hour).padStart(2, "0")}:00`;
}

function timeStringToMinutes(value?: string | null) {
  if (!value) {
    return null;
  }
  const [hourPart, minutePart] = value.split(":");
  const hour = Number.parseInt(hourPart ?? "", 10);
  const minute = Number.parseInt(minutePart ?? "", 10);
  if (Number.isNaN(hour) || Number.isNaN(minute)) {
    return null;
  }
  return hour * 60 + minute;
}

function formatMinutesToTime(minutes: number) {
  const safeMinutes = Math.max(0, minutes);
  const hour = Math.floor(safeMinutes / 60);
  const minute = safeMinutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}
