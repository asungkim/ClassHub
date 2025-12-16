"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { useState } from "react";
import { ClinicSlotGrid } from "@/components/clinic/ClinicSlotGrid";
import { CreateSlotModal } from "@/components/clinic/create-slot-modal";
import { EditSlotModal } from "@/components/clinic/edit-slot-modal";
import {
  DAY_OPTIONS,
  DayOfWeekLiteral,
  TIME_RANGE_START,
  TIME_RANGE_END,
  addMinutesToTime,
  formatHour,
  timeToMinutes
} from "@/components/clinic/day-utils";
import {
  useClinicSlots,
  useCreateClinicSlot,
  useUpdateClinicSlot,
  useDeleteClinicSlot,
  useActivateClinicSlot,
  useDeactivateClinicSlot,
  getClinicSlotConflict
} from "@/hooks/clinic/use-clinic-slots";
import type { components } from "@/types/openapi";

type ClinicSlotResponse = components["schemas"]["ClinicSlotResponse"];

export default function ClinicSlotsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const { data: slots = [], isLoading, error } = useClinicSlots();
  const createSlot = useCreateClinicSlot();
  const updateSlot = useUpdateClinicSlot();
  const deleteSlot = useDeleteClinicSlot();
  const activateSlot = useActivateClinicSlot();
  const deactivateSlot = useDeactivateClinicSlot();
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<ClinicSlotResponse | null>(null);
  const [createDefaults, setCreateDefaults] = useState<{
    dayOfWeek: DayOfWeekLiteral;
    startTime: string;
    endTime: string;
    capacity: number;
  }>({
    dayOfWeek: DAY_OPTIONS[0].value,
    startTime: formatHour(TIME_RANGE_START),
    endTime: formatHour(TIME_RANGE_START + 1),
    capacity: 10
  });

  if (!canRender) return fallback;

  const handleSlotClick = (slot: ClinicSlotResponse) => {
    setSelectedSlot(slot);
    setEditModalOpen(true);
  };

  const openCreateModal = (day: DayOfWeekLiteral, start: string, end?: string) => {
    setCreateDefaults({
      dayOfWeek: day,
      startTime: start,
      endTime: end ?? defaultEndTime(start),
      capacity: 10
    });
    setCreateModalOpen(true);
  };

  const handleEmptySlotClick = (day: DayOfWeekLiteral, time: string) => {
    openCreateModal(day, time);
  };

  const handleSelectionComplete = (day: DayOfWeekLiteral, startTime: string, endTime: string) => {
    openCreateModal(day, startTime, endTime);
  };

  const handleCreateClick = () => {
    openCreateModal(DAY_OPTIONS[0].value, formatHour(TIME_RANGE_START));
  };

  const handleCreateSubmit = async (values: components["schemas"]["ClinicSlotCreateRequest"]) => {
    await createSlot.mutateAsync(values);
  };

  const handleEditSubmit = async (slotId: string, values: components["schemas"]["ClinicSlotUpdateRequest"]) => {
    await updateSlot.mutateAsync({ slotId, request: values });
    setEditModalOpen(false);
    setSelectedSlot(null);
  };

  const handleDeleteSlot = async (slotId: string) => {
    await deleteSlot.mutateAsync(slotId);
    setEditModalOpen(false);
    setSelectedSlot(null);
  };

  const handleToggleActive = async (slotId: string, nextActive: boolean) => {
    if (nextActive) {
      await activateSlot.mutateAsync(slotId);
      return;
    }
    await deactivateSlot.mutateAsync(slotId);
  };

  const closeEditModal = () => {
    setEditModalOpen(false);
    setSelectedSlot(null);
  };

  if (isLoading) {
    return (
      <DashboardShell
        title="클리닉 슬롯 관리"
        subtitle="주간 클리닉 시간표를 설정하고 관리합니다"
      >
        <div className="flex h-64 items-center justify-center">
          <p className="text-slate-500">로딩 중...</p>
        </div>
      </DashboardShell>
    );
  }

  if (error) {
    return (
      <DashboardShell
        title="클리닉 슬롯 관리"
        subtitle="주간 클리닉 시간표를 설정하고 관리합니다"
      >
        <div className="flex h-64 items-center justify-center">
          <p className="text-rose-600">데이터를 불러오는데 실패했습니다</p>
        </div>
      </DashboardShell>
    );
  }

  return (
    <DashboardShell
      title="클리닉 슬롯 관리"
      subtitle="주간 클리닉 시간표를 설정하고 관리합니다"
    >
      <ClinicSlotGrid
        slots={slots}
        onSlotClick={handleSlotClick}
        onEmptySlotClick={handleEmptySlotClick}
        onCreateClick={handleCreateClick}
        onSelectionComplete={handleSelectionComplete}
      />

      <CreateSlotModal
        open={createModalOpen}
        initialValues={createDefaults}
        onClose={() => setCreateModalOpen(false)}
        onSubmit={handleCreateSubmit}
        isSubmitting={createSlot.isPending}
        conflictChecker={(dayOfWeek, startTime, endTime) =>
          getClinicSlotConflict(slots, dayOfWeek, startTime, endTime)
        }
      />

      <EditSlotModal
        open={editModalOpen}
        slot={selectedSlot}
        onClose={closeEditModal}
        onSubmit={handleEditSubmit}
        onDelete={handleDeleteSlot}
        onToggleActive={handleToggleActive}
        isUpdating={updateSlot.isPending}
        isDeleting={deleteSlot.isPending}
        isToggling={activateSlot.isPending || deactivateSlot.isPending}
        conflictChecker={(dayOfWeek, startTime, endTime) =>
          getClinicSlotConflict(slots, dayOfWeek, startTime, endTime, selectedSlot?.id)
        }
      />
    </DashboardShell>
  );
}

function defaultEndTime(startTime: string) {
  const proposed = addMinutesToTime(startTime, 60);
  const maxMinutes = TIME_RANGE_END * 60;
  return timeToMinutes(proposed) > maxMinutes ? formatHour(TIME_RANGE_END) : proposed;
}
