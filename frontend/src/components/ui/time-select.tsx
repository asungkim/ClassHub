import { Select } from "./select";

export type TimeSelectProps = {
  label?: string;
  value: string; // "HH:mm" 형식
  onChange: (value: string) => void;
  error?: string;
  required?: boolean;
  disabled?: boolean;
};

// 00 ~ 23 시간 옵션
const HOUR_OPTIONS = Array.from({ length: 24 }, (_, i) => {
  const hour = i.toString().padStart(2, "0");
  return { value: hour, label: hour };
});

// 00, 15, 30, 45 분 옵션
const MINUTE_OPTIONS = [
  { value: "00", label: "00" },
  { value: "15", label: "15" },
  { value: "30", label: "30" },
  { value: "45", label: "45" }
];

export function TimeSelect({ label, value, onChange, error, required, disabled }: TimeSelectProps) {
  const [hour, minute] = value.split(":");

  const handleHourChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const newHour = event.target.value;
    onChange(`${newHour}:${minute || "00"}`);
  };

  const handleMinuteChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const newMinute = event.target.value;
    onChange(`${hour || "00"}:${newMinute}`);
  };

  return (
    <div className="flex flex-col gap-1.5">
      {label ? (
        <label className="text-sm font-semibold text-slate-700">
          {label}
          {required ? <span className="ml-1 text-rose-500">*</span> : null}
        </label>
      ) : null}

      <div className="flex items-center gap-2">
        <Select
          value={hour || "00"}
          onChange={handleHourChange}
          disabled={disabled}
          className="flex-1"
          aria-label={label ? `${label} - 시` : "시"}
        >
          {HOUR_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}시
            </option>
          ))}
        </Select>

        <span className="text-sm font-medium text-slate-500">:</span>

        <Select
          value={minute || "00"}
          onChange={handleMinuteChange}
          disabled={disabled}
          className="flex-1"
          aria-label={label ? `${label} - 분` : "분"}
        >
          {MINUTE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}분
            </option>
          ))}
        </Select>
      </div>

      {error ? <p className="text-xs font-semibold text-rose-600">{error}</p> : null}
    </div>
  );
}
