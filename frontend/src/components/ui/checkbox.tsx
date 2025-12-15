import { InputHTMLAttributes, ReactNode } from "react";
import { classhubTheme } from "@/theme/classhub-theme";

type CheckboxProps = {
  label?: ReactNode;
} & Omit<InputHTMLAttributes<HTMLInputElement>, "type">;

export function Checkbox({ label, ...props }: CheckboxProps) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-3 text-sm text-slate-600">
      <input
        type="checkbox"
        className="h-4 w-4 rounded border border-slate-300 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
        style={{
          accentColor: classhubTheme.colors.primary.main
        }}
        {...props}
      />
      {label}
    </label>
  );
}
