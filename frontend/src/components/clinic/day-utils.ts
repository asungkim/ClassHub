export const DAY_OPTIONS = [
  { value: "MONDAY", label: "월요일", shortLabel: "월" },
  { value: "TUESDAY", label: "화요일", shortLabel: "화" },
  { value: "WEDNESDAY", label: "수요일", shortLabel: "수" },
  { value: "THURSDAY", label: "목요일", shortLabel: "목" },
  { value: "FRIDAY", label: "금요일", shortLabel: "금" },
  { value: "SATURDAY", label: "토요일", shortLabel: "토" },
  { value: "SUNDAY", label: "일요일", shortLabel: "일" }
] as const;

export type DayOfWeekLiteral = (typeof DAY_OPTIONS)[number]["value"];

export const DAY_LABELS: Record<DayOfWeekLiteral, string> = DAY_OPTIONS.reduce(
  (acc, item) => {
    acc[item.value] = item.shortLabel;
    return acc;
  },
  {} as Record<DayOfWeekLiteral, string>
);

export const TIME_RANGE_START = 10;
export const TIME_RANGE_END = 22;

export function generateTimeSlots() {
  return Array.from({ length: TIME_RANGE_END - TIME_RANGE_START }, (_, index) =>
    formatHour(TIME_RANGE_START + index)
  );
}

export function formatHour(hour: number) {
  return `${hour.toString().padStart(2, "0")}:00`;
}

export function addMinutesToTime(time: string, minutes: number) {
  const total = timeToMinutes(time) + minutes;
  const clamped = Math.max(0, total);
  const hour = Math.floor(clamped / 60);
  const min = clamped % 60;
  return `${hour.toString().padStart(2, "0")}:${min.toString().padStart(2, "0")}`;
}

export function timeToMinutes(time: string) {
  const [hours, minutes] = time.split(":").map(Number);
  return hours * 60 + (minutes || 0);
}

export function isTimeRangeValid(startTime: string, endTime: string) {
  return timeToMinutes(startTime) < timeToMinutes(endTime);
}
