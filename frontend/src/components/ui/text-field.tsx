import { InputHTMLAttributes, ReactNode } from "react";
import clsx from "clsx";
import { classhubTheme } from "@/theme/classhub-theme";

type TextFieldProps = {
  label?: string;
  helperText?: string;
  icon?: ReactNode;
} & InputHTMLAttributes<HTMLInputElement>;

export function TextField({ label, helperText, icon, className, ...props }: TextFieldProps) {
  return (
    <label className="flex w-full flex-col gap-2">
      {label && <span className="text-sm font-medium text-slate-700">{label}</span>}
      <div className="relative">
        {icon && (
          <span className="pointer-events-none absolute inset-y-0 left-4 flex items-center text-slate-400">
            {icon}
          </span>
        )}
        <input
          className={clsx(
            "w-full border bg-white text-slate-900 placeholder:text-slate-400 focus-visible:outline-none",
            "transition focus-visible:ring-4 focus-visible:ring-primary/10",
            icon ? "pl-11 pr-4" : "px-4",
            "h-12 rounded-[12px]",
            className
          )}
          style={{
            borderColor: classhubTheme.colors.border.light,
            fontSize: classhubTheme.typography.fontSize.base
          }}
          {...props}
        />
      </div>
      {helperText && <span className="text-xs text-slate-500">{helperText}</span>}
    </label>
  );
}
