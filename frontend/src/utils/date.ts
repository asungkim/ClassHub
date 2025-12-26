export function formatDateYmdKst(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export function formatDateLabelKst(value?: string | null) {
  if (!value) {
    return "날짜 정보 없음";
  }
  const raw = value.trim();
  if (DATE_ONLY_PATTERN.test(raw)) {
    const [year, month, day] = raw.split("-").map((part) => Number.parseInt(part, 10));
    if (!Number.isNaN(year) && !Number.isNaN(month) && !Number.isNaN(day)) {
      const date = new Date(year, month - 1, day);
      return new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "short",
        day: "numeric"
      }).format(date);
    }
  }

  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric"
  }).format(parsed);
}

export function formatDateTimeLabelKst(value?: string | null) {
  if (!value) {
    return "";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(parsed);
}
