const GRADE_LABELS: Record<string, string> = {
  ELEMENTARY_1: "초1",
  ELEMENTARY_2: "초2",
  ELEMENTARY_3: "초3",
  ELEMENTARY_4: "초4",
  ELEMENTARY_5: "초5",
  ELEMENTARY_6: "초6",
  MIDDLE_1: "중1",
  MIDDLE_2: "중2",
  MIDDLE_3: "중3",
  HIGH_1: "고1",
  HIGH_2: "고2",
  HIGH_3: "고3",
  GAP_YEAR: "N수"
};

export function formatStudentGrade(value?: string | null) {
  if (!value) {
    return "";
  }
  return GRADE_LABELS[value] ?? value;
}

export function formatStudentBirthDate(value?: string | null) {
  if (!value) {
    return "-";
  }
  try {
    return new Intl.DateTimeFormat("ko", { dateStyle: "medium" }).format(new Date(value));
  } catch {
    return value;
  }
}
