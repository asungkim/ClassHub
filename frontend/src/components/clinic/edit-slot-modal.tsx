import { useEffect, useState } from "react";
import { Modal } from "@/components/ui/modal";
import { TextField } from "@/components/ui/text-field";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { TimeSelect } from "@/components/ui/time-select";
import {
  DAY_OPTIONS,
  DayOfWeekLiteral,
  addMinutesToTime,
  isTimeRangeValid
} from "@/components/clinic/day-utils";
import type { components } from "@/types/openapi";

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];
type ClinicSlotUpdateRequest = components["schemas"]["ClinicSlotUpdateRequest"];

type EditSlotModalProps = {
  open: boolean;
  slot: ClinicSlotResponse | null;
  onClose: () => void;
  onSubmit: (slotId: string, values: ClinicSlotUpdateRequest) => Promise<void>;
  onDelete: (slotId: string) => Promise<void>;
  onToggleActive: (slotId: string, nextActive: boolean) => Promise<void>;
  isUpdating?: boolean;
  isDeleting?: boolean;
  isToggling?: boolean;
  conflictChecker?: (
    dayOfWeek: DayOfWeekLiteral,
    startTime: string,
    endTime: string
  ) => ClinicSlotResponse | undefined;
};

const DEFAULT_DAY: DayOfWeekLiteral = "MONDAY";

export function EditSlotModal({
  open,
  slot,
  onClose,
  onSubmit,
  onDelete,
  onToggleActive,
  isUpdating = false,
  isDeleting = false,
  isToggling = false,
  conflictChecker
}: EditSlotModalProps) {
  const [dayOfWeek, setDayOfWeek] = useState<DayOfWeekLiteral>(DEFAULT_DAY);
  const [startTime, setStartTime] = useState("10:00");
  const [endTime, setEndTime] = useState("11:00");
  const [capacity, setCapacity] = useState(10);
  const [isActive, setIsActive] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [conflictMessage, setConflictMessage] = useState<string | null>(null);
  const [toggleError, setToggleError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  useEffect(() => {
    if (!open || !slot) return;
    const resolvedDay = (slot.dayOfWeek as DayOfWeekLiteral) ?? DEFAULT_DAY;
    const start = slot.startTime?.slice(0, 5) ?? "10:00";
    const end = slot.endTime?.slice(0, 5) ?? addMinutesToTime(start, 60);

    setDayOfWeek(resolvedDay);
    setStartTime(start);
    setEndTime(end);
    setCapacity(slot.capacity ?? 10);
    setIsActive(slot.isActive !== false);
    setError(null);
    setConflictMessage(null);
    setToggleError(null);
  }, [open, slot]);

  const handleSubmit = async () => {
    if (!slot?.id) return;
    setError(null);
    setConflictMessage(null);

    if (!isTimeRangeValid(startTime, endTime)) {
      setError("시작 시간은 종료 시간보다 빨라야 합니다.");
      return;
    }

    const conflict = conflictChecker?.(dayOfWeek, startTime, endTime);
    if (conflict) {
      setConflictMessage(
        `${formatDisplayDay(conflict.dayOfWeek)} ${conflict.startTime?.slice(0, 5)}-${conflict.endTime?.slice(
          0,
          5
        )} 슬롯과 겹칩니다.`
      );
      return;
    }

    try {
      await onSubmit(slot.id, {
        dayOfWeek,
        startTime,
        endTime,
        capacity
      });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "슬롯 수정에 실패했습니다.");
    }
  };

  const handleToggleActive = async () => {
    if (!slot?.id) return;
    setToggleError(null);
    const nextActive = !isActive;
    try {
      await onToggleActive(slot.id, nextActive);
      setIsActive(nextActive);
    } catch (toggleErr) {
      setToggleError(toggleErr instanceof Error ? toggleErr.message : "상태 변경에 실패했습니다.");
    }
  };

  const handleDelete = async () => {
    if (!slot?.id) return;
    setConfirmOpen(false);
    try {
      await onDelete(slot.id);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "슬롯 삭제에 실패했습니다.");
    }
  };

  return (
    <>
      <Modal open={open} onClose={onClose} title="슬롯 수정" size="md">
        {slot ? (
          <div className="flex flex-1 flex-col gap-5 overflow-y-auto px-6 py-4">
            <div className="rounded-xl border border-slate-100 bg-slate-50 px-4 py-3 text-xs text-slate-500">
              마지막 수정 {slot.updatedAt ? new Date(slot.updatedAt).toLocaleString() : "정보 없음"}
            </div>

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

            <div className="rounded-xl border border-slate-100 bg-white/70 px-4 py-3">
              <Checkbox
                checked={isActive}
                onChange={handleToggleActive}
                disabled={isToggling}
                label={
                  <div className="flex flex-col">
                    <span className="font-semibold text-slate-700">슬롯 활성화</span>
                    <span className="text-xs text-slate-500">
                      {isActive ? "학생이 예약할 수 있는 상태입니다." : "시간표에 비활성 슬롯으로 표시됩니다."}
                    </span>
                  </div>
                }
              />
              {toggleError && <p className="mt-2 text-xs text-rose-500">{toggleError}</p>}
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
        ) : (
          <div className="px-6 py-8 text-center text-sm text-slate-500">선택된 슬롯이 없습니다.</div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-6 py-4">
          <Button
            variant="ghost"
            className="text-rose-600 hover:bg-rose-50"
            onClick={() => setConfirmOpen(true)}
            disabled={isDeleting || !slot}
          >
            슬롯 삭제
          </Button>
          <div className="flex gap-3">
            <Button variant="ghost" onClick={onClose} disabled={isUpdating}>
              취소
            </Button>
            <Button onClick={handleSubmit} disabled={isUpdating || !slot}>
              {isUpdating ? "저장 중..." : "저장"}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleDelete}
        title="슬롯 삭제"
        message="정말로 이 슬롯을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다."
        confirmText="삭제"
        cancelText="취소"
        isLoading={isDeleting}
      />
    </>
  );
}

function formatDisplayDay(day?: string | null) {
  const option = DAY_OPTIONS.find((item) => item.value === day);
  return option?.label ?? "해당 요일";
}
