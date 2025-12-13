import clsx from "clsx";
import type { components } from "@/types/openapi";

type CourseResponse = components["schemas"]["CourseResponse"];
type CourseScheduleResponse = components["schemas"]["CourseScheduleResponse"];

type CoursePickerProps = {
  courses: CourseResponse[];
  selectedCourseId?: string;
  onSelect: (courseId: string) => void;
  isLoading?: boolean;
};

const DAY_LABELS: Record<string, string> = {
  MONDAY: "월",
  TUESDAY: "화",
  WEDNESDAY: "수",
  THURSDAY: "목",
  FRIDAY: "금",
  SATURDAY: "토",
  SUNDAY: "일"
};

export function CoursePicker({ courses, selectedCourseId, onSelect, isLoading }: CoursePickerProps) {
  if (isLoading) {
    return (
      <div className="space-y-3">
        <p className="text-sm font-semibold text-slate-700">수업 선택</p>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          {Array.from({ length: 2 }).map((_, index) => (
            <div key={index} className="h-28 rounded-2xl border border-slate-200 bg-slate-50" />
          ))}
        </div>
      </div>
    );
  }

  if (!courses.length) {
    return (
      <div className="space-y-2 rounded-2xl border border-dashed border-slate-200 bg-white p-4">
        <p className="text-sm font-semibold text-slate-900">선택할 수업이 없습니다.</p>
        <p className="text-sm text-slate-600">먼저 반을 생성한 뒤 학생을 등록해주세요.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <p className="text-sm font-semibold text-slate-700">수업 선택</p>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        {courses.map((course) => {
          const courseId = course.id ?? "";
          const isSelected = selectedCourseId === courseId;
          return (
            <button
              key={courseId}
              type="button"
              className={clsx(
                "rounded-2xl border p-4 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2",
                isSelected
                        ? "border-blue-400 bg-blue-50 shadow"
                        : "border-slate-200 bg-white hover:border-blue-200"
              )}
              onClick={() => courseId && onSelect(courseId)}
            >
              <p className="text-base font-semibold text-slate-900">{course.name ?? "이름 없음"}</p>
              <p className="text-sm text-slate-600">{course.company ?? "소속 미입력"}</p>
              <p className="mt-2 text-xs text-slate-500">{formatSchedules(course.schedules)}</p>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function formatSchedules(schedules?: CourseScheduleResponse[]) {
  if (!schedules?.length) {
    return "시간표 정보 없음";
  }
  return schedules
          .map((schedule) => {
            const day = schedule?.dayOfWeek
                    ? DAY_LABELS[schedule.dayOfWeek] ?? schedule.dayOfWeek
                    : "요일 미정";
            const start = formatTime(schedule?.startTime);
            const end = formatTime(schedule?.endTime);
            return `${day} ${start}-${end}`;
          })
          .join(", ");
}

function formatTime(value?: string | null) {
  if (!value) return "--:--";
  return value.slice(0, 5);
}
