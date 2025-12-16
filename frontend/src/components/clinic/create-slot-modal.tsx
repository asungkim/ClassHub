import { useEffect, useState } from "react";
import { Modal } from "@/components/ui/modal";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { TimeSelect } from "@/components/ui/time-select";
import { DAY_OPTIONS, DayOfWeekLiteral, addMinutesToTime, isTimeRangeValid } from "@/components/clinic/day-utils";
import type { components } from "@/types/openapi";

type ClinicSlotCreateRequest = components["schemas"]["ClinicSlotCreateRequest"];
type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];

type CreateSlotModalProps = {
  open: boolean;
  initialValues?: {
    dayOfWeek?: DayOfWeekLiteral;
    startTime?: string;
    endTime?: string;
    capacity?: number;
  };
  onClose: () => void;
  onSubmit: (values: ClinicSlotCreateRequest) => Promise<void>;
  isSubmitting?: boolean;
  conflictChecker?: (
    dayOfWeek: DayOfWeekLiteral,
    startTime: string,
    endTime: string
  ) => ClinicSlotResponse | undefined;
};

const DEFAULT_VALUES = {
  dayOfWeek: "MONDAY" as DayOfWeekLiteral,
  startTime: "14:00",
  endTime: "15:00",
  capacity: 10
};

export function CreateSlotModal({
  open,
  initialValues,
  onClose,
  onSubmit,
  isSubmitting = false,
  conflictChecker
}: CreateSlotModalProps) {
  const [dayOfWeek, setDayOfWeek] = useState<DayOfWeekLiteral>(DEFAULT_VALUES.dayOfWeek);
  const [startTime, setStartTime] = useState(DEFAULT_VALUES.startTime);
  const [endTime, setEndTime] = useState(DEFAULT_VALUES.endTime);
  const [capacity, setCapacity] = useState(DEFAULT_VALUES.capacity);
  const [error, setError] = useState<string | null>(null);
  const [conflictMessage, setConflictMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    const nextDay = initialValues?.dayOfWeek ?? DEFAULT_VALUES.dayOfWeek;
    const nextStart = initialValues?.startTime ?? DEFAULT_VALUES.startTime;
    const nextEnd =
      initialValues?.endTime ??
      addMinutesToTime(nextStart, 60);

    setDayOfWeek(nextDay);
    setStartTime(nextStart);
    setEndTime(nextEnd);
    setCapacity(initialValues?.capacity ?? DEFAULT_VALUES.capacity);
    setError(null);
    setConflictMessage(null);
  }, [initialValues, open]);

  const handleSubmit = async () => {
    setError(null);
    setConflictMessage(null);

    if (!isTimeRangeValid(startTime, endTime)) {
      setError("시작 시간은 종료 시간보다 빨라야 합니다.");
      return;
    }

    const conflict = conflictChecker?.(dayOfWeek, startTime, endTime);
    if (conflict) {
      setConflictMessage(
        `${formatDisplayDay(conflict.dayOfWeek)} ${conflict.startTime?.slice(0, 5)}-${conflict.endTime?.slice(0, 5)} 슬롯과 겹칩니다.`
      );
      return;
    }

    try {
      await onSubmit({
        dayOfWeek,
        startTime,
        endTime,
        capacity
      });
      onClose();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "슬롯 생성에 실패했습니다.");
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="새 슬롯 추가" size="md">
      <div className="flex flex-1 flex-col gap-5 overflow-y-auto px-6 py-4">
        <div className="grid gap-4 md:grid-cols-2">
          <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
            요일
            <select
              value={dayOfWeek}
              onChange={(event) => setDayOfWeek(event.target.value as DayOfWeekLiteral)}
              className="h-12 rounded-[12px] border border-slate-200 px-4 text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-500/10"
            >
              {DAY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <TextField
            label="정원"
            type="number"
            min={1}
            max={50}
            value={capacity}
            onChange={(event) => setCapacity(Number(event.target.value))}
            required
          />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <TimeSelect label="시작 시간" value={startTime} onChange={setStartTime} required />
          <TimeSelect label="종료 시간" value={endTime} onChange={setEndTime} required />
        </div>

        {conflictMessage && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
            {conflictMessage}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        )}
      </div>

      <div className="flex items-center justify-end gap-3 border-t border-slate-100 px-6 py-4">
        <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>
          취소
        </Button>
        <Button onClick={handleSubmit} disabled={isSubmitting}>
          {isSubmitting ? "저장 중..." : "저장"}
        </Button>
      </div>
    </Modal>
  );
}

function formatDisplayDay(day?: string | null) {
  const option = DAY_OPTIONS.find((item) => item.value === day);
  return option?.label ?? "해당 요일";
}
