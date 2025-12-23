"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { useClinicSlots } from "@/hooks/clinic/use-clinic-slots";
import { fetchTeacherBranchAssignments } from "@/lib/dashboard-api";
import { api } from "@/lib/api";
import { getApiErrorMessage, getFetchError } from "@/lib/api-error";
import { useToast } from "@/components/ui/toast";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/ui/inline-error";
import { Skeleton } from "@/components/ui/skeleton";
import { Modal } from "@/components/ui/modal";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { TextField } from "@/components/ui/text-field";
import { TimeSelect } from "@/components/ui/time-select";
import { EmptyState } from "@/components/shared/empty-state";
import { WeeklyTimeGrid } from "@/components/shared/weekly-time-grid";
import type { components } from "@/types/openapi";
import type { TeacherBranchAssignment } from "@/types/dashboard";

const DAY_OPTIONS = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" }
] as const;

const TIME_PLACEHOLDER = "--:--";
const GRID_START_HOUR = 6;
const GRID_END_HOUR = 22;
const GRID_HOUR_HEIGHT = 56;

const GRID_DAYS = DAY_OPTIONS.map((day) => ({ key: day.value, label: day.label }));

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];
type ClinicSlotCreateRequest = components["schemas"]["ClinicSlotCreateRequest"];
type ClinicSlotUpdateRequest = components["schemas"]["ClinicSlotUpdateRequest"];

type SlotFormState = {
  dayOfWeek: ClinicSlotCreateRequest["dayOfWeek"];
  startTime: string;
  endTime: string;
  defaultCapacity: string;
};

const DEFAULT_FORM_STATE: SlotFormState = {
  dayOfWeek: "MONDAY",
  startTime: "09:00",
  endTime: "10:00",
  defaultCapacity: "6"
};

const formatTime = (time?: string) => (time && time.length >= 5 ? time.slice(0, 5) : TIME_PLACEHOLDER);

export default function TeacherClinicSlotsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const { showToast } = useToast();
  const [branches, setBranches] = useState<TeacherBranchAssignment[]>([]);
  const [branchLoading, setBranchLoading] = useState(false);
  const [branchError, setBranchError] = useState<string | null>(null);
  const [selectedBranchId, setSelectedBranchId] = useState<string | null>(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formState, setFormState] = useState<SlotFormState>(DEFAULT_FORM_STATE);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingSlot, setEditingSlot] = useState<ClinicSlotResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ClinicSlotResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const loadBranches = useCallback(async () => {
    setBranchLoading(true);
    setBranchError(null);
    try {
      const result = await fetchTeacherBranchAssignments({ status: "ACTIVE", page: 0, size: 100 });
      setBranches(result.items ?? []);
    } catch (err) {
      const message = err instanceof Error ? err.message : "지점 목록을 불러오지 못했습니다.";
      setBranchError(message);
    } finally {
      setBranchLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadBranches();
  }, [loadBranches]);

  useEffect(() => {
    if (branches.length === 0) {
      setSelectedBranchId(null);
      return;
    }
    if (selectedBranchId && branches.some((branch) => branch.branchId === selectedBranchId)) {
      return;
    }
    setSelectedBranchId(branches[0]?.branchId ?? null);
  }, [branches, selectedBranchId]);

  const selectedBranch = useMemo(
    () => branches.find((branch) => branch.branchId === selectedBranchId) ?? null,
    [branches, selectedBranchId]
  );

  const {
    slots,
    isLoading: slotsLoading,
    error: slotsError,
    refresh: refreshSlots
  } = useClinicSlots({ branchId: selectedBranchId ?? undefined }, Boolean(selectedBranchId));

  const slotsByDay = useMemo(() => {
    const base: Record<string, ClinicSlotResponse[]> = {};
    DAY_OPTIONS.forEach((day) => {
      base[day.value] = [];
    });
    slots.forEach((slot) => {
      if (!slot.dayOfWeek) {
        return;
      }
      base[slot.dayOfWeek] = [...(base[slot.dayOfWeek] ?? []), slot];
    });
    Object.values(base).forEach((list) => {
      list.sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
    });
    return base;
  }, [slots]);

  const openCreateModal = (preset?: Partial<SlotFormState>) => {
    setEditingSlot(null);
    setFormState({ ...DEFAULT_FORM_STATE, ...preset });
    setFormError(null);
    setIsModalOpen(true);
  };

  const openEditModal = (slot: ClinicSlotResponse) => {
    setEditingSlot(slot);
    setFormState({
      dayOfWeek: slot.dayOfWeek ?? "MONDAY",
      startTime: slot.startTime ?? "09:00",
      endTime: slot.endTime ?? "10:00",
      defaultCapacity: slot.defaultCapacity?.toString() ?? ""
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleSelectRange = (range: { dayKey: string; startTime: string; endTime: string }) => {
    if (!selectedBranchId) {
      showToast("error", "지점을 먼저 선택해 주세요.");
      return;
    }
    openCreateModal({
      dayOfWeek: range.dayKey as SlotFormState["dayOfWeek"],
      startTime: range.startTime,
      endTime: range.endTime
    });
  };

  const closeModal = () => {
    if (isSubmitting) {
      return;
    }
    setIsModalOpen(false);
  };

  const submitSlot = async () => {
    if (!selectedBranchId) {
      showToast("error", "지점을 먼저 선택해주세요.");
      return;
    }

    const capacity = Number(formState.defaultCapacity);
    if (!formState.dayOfWeek || !formState.startTime || !formState.endTime) {
      setFormError("요일과 시간을 모두 입력해주세요.");
      return;
    }
    if (!Number.isFinite(capacity) || capacity < 1) {
      setFormError("정원은 1명 이상이어야 합니다.");
      return;
    }
    if (formState.startTime >= formState.endTime) {
      setFormError("종료 시간이 시작 시간보다 늦어야 합니다.");
      return;
    }

    setIsSubmitting(true);
    setFormError(null);

    try {
      if (editingSlot?.slotId) {
        const body: ClinicSlotUpdateRequest = {
          dayOfWeek: formState.dayOfWeek,
          startTime: formState.startTime,
          endTime: formState.endTime,
          defaultCapacity: capacity
        };
        const response = await api.PATCH("/api/v1/clinic-slots/{slotId}", {
          params: { path: { slotId: editingSlot.slotId } },
          body
        });
        const fetchError = getFetchError(response);
        if (fetchError) {
          throw new Error(getApiErrorMessage(fetchError, "슬롯을 수정하지 못했습니다."));
        }
        if (!response.data?.data) {
          throw new Error("슬롯을 수정하지 못했습니다.");
        }
        showToast("success", "슬롯이 수정되었습니다.");
      } else {
        const body: ClinicSlotCreateRequest = {
          branchId: selectedBranchId,
          dayOfWeek: formState.dayOfWeek,
          startTime: formState.startTime,
          endTime: formState.endTime,
          defaultCapacity: capacity
        };
        const response = await api.POST("/api/v1/clinic-slots", { body });
        const fetchError = getFetchError(response);
        if (fetchError) {
          throw new Error(getApiErrorMessage(fetchError, "슬롯을 생성하지 못했습니다."));
        }
        if (!response.data?.data) {
          throw new Error("슬롯을 생성하지 못했습니다.");
        }
        showToast("success", "슬롯이 생성되었습니다.");
      }
      setIsModalOpen(false);
      await refreshSlots();
    } catch (err) {
      const message = err instanceof Error ? err.message : "슬롯 저장에 실패했습니다.";
      setFormError(message);
      showToast("error", message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const confirmDelete = (slot: ClinicSlotResponse) => {
    setDeleteTarget(slot);
  };

  const handleDelete = async () => {
    if (!deleteTarget?.slotId) {
      return;
    }
    setIsDeleting(true);
    try {
      const response = await api.DELETE("/api/v1/clinic-slots/{slotId}", {
        params: { path: { slotId: deleteTarget.slotId } }
      });
      const fetchError = getFetchError(response);
      if (fetchError) {
        throw new Error(getApiErrorMessage(fetchError, "슬롯을 삭제하지 못했습니다."));
      }
      showToast("success", "슬롯이 삭제되었습니다.");
      setDeleteTarget(null);
      await refreshSlots();
    } catch (err) {
      const message = err instanceof Error ? err.message : "슬롯을 삭제하지 못했습니다.";
      showToast("error", message);
    } finally {
      setIsDeleting(false);
    }
  };

  if (!canRender) {
    return fallback;
  }

  return (
    <div className="space-y-6 lg:space-y-8">
      <Card title="지점별 클리닉 (슬롯)" description="출강 지점별 클리닉 슬롯을 관리합니다.">
        <div className="space-y-6">
          <div>
            <p className="text-sm font-semibold text-slate-700">출강 지점 선택</p>
            <p className="text-xs text-slate-500">지점을 선택하면 해당 지점의 슬롯을 확인할 수 있습니다.</p>
          </div>

          {branchError && (
            <div className="space-y-3">
              <InlineError message={branchError} />
              <Button variant="secondary" onClick={() => void loadBranches()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {branchLoading && <Skeleton className="h-12 w-full max-w-sm" />}

          {!branchLoading && branches.length === 0 && (
            <EmptyState message="연결된 지점이 없습니다." description="지점 연결 후 슬롯을 관리할 수 있습니다." />
          )}

          {!branchLoading && branches.length > 0 && (
            <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
              <Select
                label="지점"
                value={selectedBranchId ?? ""}
                onChange={(event) => setSelectedBranchId(event.target.value)}
                className="md:w-80"
              >
                {branches.map((branch) => (
                  <option key={branch.branchId} value={branch.branchId}>
                    {branch.companyName ?? "학원"} · {branch.branchName ?? "지점"}
                  </option>
                ))}
              </Select>

              <Button onClick={() => openCreateModal()} disabled={!selectedBranchId}>
                슬롯 추가
              </Button>
            </div>
          )}
        </div>
      </Card>

      <Card
        title="슬롯 시간표"
        description={selectedBranch ? `${selectedBranch.branchName ?? "지점"} 슬롯 시간표입니다.` : ""}
      >
        <div className="space-y-6">
          {slotsError && (
            <div className="space-y-3">
              <InlineError message={slotsError} />
              <Button variant="secondary" onClick={() => void refreshSlots()}>
                다시 불러오기
              </Button>
            </div>
          )}

          {slotsLoading && (
            <div className="space-y-3">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-[640px] w-full" />
            </div>
          )}

          {!slotsLoading && !selectedBranchId && (
            <EmptyState message="지점을 먼저 선택해 주세요." description="지점 선택 후 슬롯이 표시됩니다." />
          )}

          {!slotsLoading && selectedBranchId && !slotsError && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm text-slate-500">
                  셀을 드래그/롱프레스하면 슬롯 생성이 시작됩니다.
                </p>
                {slots.length === 0 && (
                  <p className="text-sm font-semibold text-slate-700">등록된 슬롯이 없습니다.</p>
                )}
              </div>
              <WeeklyTimeGrid
                days={GRID_DAYS}
                itemsByDay={slotsByDay}
                startHour={GRID_START_HOUR}
                endHour={GRID_END_HOUR}
                hourHeight={GRID_HOUR_HEIGHT}
                showDateHeader={false}
                selectionEnabled={Boolean(selectedBranchId)}
                onSelectRange={handleSelectRange}
                getItemRange={(slot) => ({
                  startTime: slot.startTime,
                  endTime: slot.endTime
                })}
                getItemKey={(slot, index) => slot.slotId ?? `${slot.dayOfWeek}-${slot.startTime}-${index}`}
                renderItem={({ item, style }) => (
                  <div
                    className="absolute left-1 right-1 rounded-2xl border border-blue-200 bg-blue-50 p-2 text-xs text-slate-700 shadow-sm transition hover:border-blue-300 hover:shadow-md"
                    style={style}
                    role="button"
                    tabIndex={0}
                    onClick={() => openEditModal(item)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        openEditModal(item);
                      }
                    }}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-slate-900">
                          {formatTime(item.startTime)} - {formatTime(item.endTime)}
                        </p>
                        <p className="text-[11px] text-slate-500">정원 {item.defaultCapacity ?? "-"}</p>
                      </div>
                      <button
                        type="button"
                        className="shrink-0 text-[11px] font-semibold text-rose-500 hover:text-rose-600"
                        onClick={(event) => {
                          event.stopPropagation();
                          confirmDelete(item);
                        }}
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                )}
              />
            </div>
          )}
        </div>
      </Card>

      <Modal open={isModalOpen} onClose={closeModal} title={editingSlot ? "슬롯 수정" : "슬롯 추가"} size="sm">
        <div className="space-y-4">
          <Select
            label="요일"
            value={formState.dayOfWeek}
            onChange={(event) => setFormState((prev) => ({ ...prev, dayOfWeek: event.target.value as SlotFormState["dayOfWeek"] }))}
          >
            {DAY_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>

          <TimeSelect
            label="시작 시간"
            value={formState.startTime}
            onChange={(value) => setFormState((prev) => ({ ...prev, startTime: value }))}
          />

          <TimeSelect
            label="종료 시간"
            value={formState.endTime}
            onChange={(value) => setFormState((prev) => ({ ...prev, endTime: value }))}
          />

          <TextField
            label="기본 정원"
            type="number"
            min={1}
            value={formState.defaultCapacity}
            onChange={(event) => setFormState((prev) => ({ ...prev, defaultCapacity: event.target.value }))}
          />

          {formError && <InlineError message={formError} />}

          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={closeModal} disabled={isSubmitting}>
              취소
            </Button>
            <Button onClick={() => void submitSlot()} disabled={isSubmitting}>
              {isSubmitting ? "저장 중..." : "저장"}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        title="슬롯을 삭제할까요?"
        message="삭제된 슬롯은 복구할 수 없습니다."
        confirmText={isDeleting ? "삭제 중..." : "삭제"}
        isLoading={isDeleting}
      />
    </div>
  );
}
