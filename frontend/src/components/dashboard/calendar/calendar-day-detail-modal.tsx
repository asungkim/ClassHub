"use client";

import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
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
  const title = dateKey ? formatDateLabel(dateKey) : "상세 기록";
  const courseEvents = events?.course ?? [];
  const personalEvents = events?.personal ?? [];
  const clinicEvents = events?.clinic ?? [];

  return (
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
                    <p className="text-sm font-semibold text-slate-900">{event.title ?? "제목 없음"}</p>
                    <p className="text-xs text-slate-500">{event.courseName ?? "반 정보 없음"}</p>
                    {event.content ? (
                      <p className="mt-2 text-xs text-slate-500 whitespace-pre-line line-clamp-3">
                        {event.content}
                      </p>
                    ) : null}
                  </div>
                  {canEdit ? (
                    <div className="flex gap-2">
                      <Button variant="ghost" onClick={() => onEditCourse(event)} disabled={!event.id}>
                        수정
                      </Button>
                      <Button variant="ghost" onClick={() => onDeleteCourse(event)} disabled={!event.id}>
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
                    <p className="text-sm font-semibold text-slate-900">{event.title ?? "제목 없음"}</p>
                    <p className="text-xs text-slate-500">{event.courseName ?? "반 정보 없음"}</p>
                    {event.content ? (
                      <p className="mt-2 text-xs text-slate-500 whitespace-pre-line line-clamp-3">
                        {event.content}
                      </p>
                    ) : null}
                  </div>
                  {canEdit ? (
                    <div className="flex gap-2">
                      <Button variant="ghost" onClick={() => onEditPersonal(event)} disabled={!event.id}>
                        수정
                      </Button>
                      <Button variant="ghost" onClick={() => onDeletePersonal(event)} disabled={!event.id}>
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
                  <p className="mt-2 text-xs text-slate-500 whitespace-pre-line line-clamp-3">
                    {event.recordSummary.content}
                  </p>
                ) : null}
              </div>
            ))
          )}
        </section>
      </div>
    </Modal>
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
