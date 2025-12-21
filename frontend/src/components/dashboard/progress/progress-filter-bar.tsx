"use client";

import { Select } from "@/components/ui/select";

export type ProgressSelectOption = {
  value: string;
  label: string;
};

type ProgressFilterBarProps = {
  label: string;
  placeholder: string;
  value: string;
  options: ProgressSelectOption[];
  onChange: (value: string) => void;
  disabled?: boolean;
};

export function ProgressFilterBar({
  label,
  placeholder,
  value,
  options,
  onChange,
  disabled = false
}: ProgressFilterBarProps) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
      <Select
        label={label}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={disabled}
      >
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </Select>
    </div>
  );
}
