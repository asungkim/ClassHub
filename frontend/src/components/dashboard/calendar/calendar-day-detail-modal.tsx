"use client";

import { useState } from "react";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import type { ClinicEvent, CourseProgressEvent, PersonalProgressEvent } from "@/types/progress";

type DayEvents = {
  course: CourseProgressEvent[];
  personal: PersonalProgressEvent[];
  clinic: ClinicEvent[];
};

type CalendarDayDetailModalProps = {
  open: boolean;
  dateKey: string | null;
  events: DayEvents | null;
  canEdit: boolean;
  onClose: () => void;
  onEditCourse: (event: CourseProgressEvent) => void;
  onEditPersonal: (event: PersonalProgressEvent) => void;
  onDeleteCourse: (event: CourseProgressEvent) => void;
  onDeletePersonal: (event: PersonalProgressEvent) => void;
};

export function CalendarDayDetailModal({
  open,
  dateKey,
  events,
  canEdit,
  onClose,
  onEditCourse,
  onEditPersonal,
  onDeleteCourse,
  onDeletePersonal
}: CalendarDayDetailModalProps) {
  const [confirmTarget, setConfirmTarget] = useState<{
    type: "course" | "personal";
    event: CourseProgressEvent | PersonalProgressEvent;
  } | null>(null);
  const title = dateKey ? formatDateLabel(dateKey) : "상세 기록";
  const courseEvents = events?.course ?? [];
  const personalEvents = events?.personal ?? [];
  const clinicEvents = events?.clinic ?? [];

  return (
    <>
      <Modal open={open} onClose={onClose} title={title} size="lg">
        <div className="space-y-6">
        <section className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-700">공통 진도</h3>
          {courseEvents.length === 0 ? (
            <p className="text-sm text-slate-400">해당 날짜에 공통 진도가 없습니다.</p>
          ) : (
            courseEvents.map((event, index) => (
              <div key={event.id ?? `course-${index}`} className="rounded-2xl border border-slate-200 px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-semibold text-slate-900">{event.title ?? "제목 없음"}</p>
                      <Badge variant="secondary">{event.courseName ?? "반 정보 없음"}</Badge>
                    </div>
                    {event.content ? (
                      <p className="mt-2 text-xs text-slate-500 whitespace-pre-line">
                        {event.content}
                      </p>
                    ) : null}
                  </div>
                  {canEdit ? (
                    <div className="flex gap-2">
                      <Button variant="ghost" onClick={() => onEditCourse(event)} disabled={!event.id}>
                        수정
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={() => event.id && setConfirmTarget({ type: "course", event })}
                        disabled={!event.id}
                      >
                        삭제
                      </Button>
                    </div>
                  ) : null}
                </div>
              </div>
            ))
          )}
        </section>

        <section className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-700">개인 진도</h3>
          {personalEvents.length === 0 ? (
            <p className="text-sm text-slate-400">해당 날짜에 개인 진도가 없습니다.</p>
          ) : (
            personalEvents.map((event, index) => (
              <div key={event.id ?? `personal-${index}`} className="rounded-2xl border border-slate-200 px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-semibold text-slate-900">{event.title ?? "제목 없음"}</p>
                      <Badge variant="secondary">{event.courseName ?? "반 정보 없음"}</Badge>
                    </div>
                    {event.content ? (
                      <p className="mt-2 text-xs text-slate-500 whitespace-pre-line">
                        {event.content}
                      </p>
                    ) : null}
                  </div>
                  {canEdit ? (
                    <div className="flex gap-2">
                      <Button variant="ghost" onClick={() => onEditPersonal(event)} disabled={!event.id}>
                        수정
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={() => event.id && setConfirmTarget({ type: "personal", event })}
                        disabled={!event.id}
                      >
                        삭제
                      </Button>
                    </div>
                  ) : null}
                </div>
              </div>
            ))
          )}
        </section>

        <section className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-700">클리닉 기록</h3>
          {clinicEvents.length === 0 ? (
            <p className="text-sm text-slate-400">해당 날짜에 클리닉 기록이 없습니다.</p>
          ) : (
            clinicEvents.map((event, index) => (
              <div
                key={event.clinicAttendanceId ?? `clinic-${index}`}
                className="rounded-2xl border border-slate-200 px-4 py-3"
              >
                <p className="text-sm font-semibold text-slate-900">
                  {event.recordSummary?.title ?? "클리닉 기록"}
                </p>
                <p className="text-xs text-slate-500">{event.recordSummary?.writerRole ?? "작성자"}</p>
                {event.recordSummary?.content ? (
                  <p className="mt-2 text-xs text-slate-500 whitespace-pre-line">
                    {event.recordSummary.content}
                  </p>
                ) : null}
              </div>
            ))
          )}
        </section>
        </div>
      </Modal>

      <ConfirmDialog
        open={Boolean(confirmTarget)}
        onClose={() => setConfirmTarget(null)}
        onConfirm={() => {
          if (!confirmTarget) return;
          if (confirmTarget.type === "course") {
            onDeleteCourse(confirmTarget.event as CourseProgressEvent);
          } else {
            onDeletePersonal(confirmTarget.event as PersonalProgressEvent);
          }
          setConfirmTarget(null);
        }}
        title="삭제 확인"
        message={
          confirmTarget?.type === "course"
            ? "해당 진도를 삭제하면 반에 대한 기록이 사라집니다. 삭제할까요?"
            : "해당 개인 진도는 영구적으로 삭제됩니다. 삭제할까요?"
        }
        confirmText="삭제"
        cancelText="취소"
      />
    </>
  );
}

function formatDateLabel(dateKey: string) {
  const date = new Date(dateKey);
  if (Number.isNaN(date.getTime())) {
    return dateKey;
  }
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short"
  });
}
