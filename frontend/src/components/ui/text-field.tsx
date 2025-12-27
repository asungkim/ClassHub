import { InputHTMLAttributes, ReactNode } from "react";
import clsx from "clsx";
import { classhubTheme } from "@/theme/classhub-theme";

type TextFieldProps = {
  label?: string;
  helperText?: string;
  error?: string;
  icon?: ReactNode;
  rightElement?: ReactNode;
} & InputHTMLAttributes<HTMLInputElement>;

export function TextField({ label, helperText, error, icon, rightElement, className, ...props }: TextFieldProps) {
  const hasError = Boolean(error);
  const hasLeftIcon = Boolean(icon);
  const hasRightElement = Boolean(rightElement);
  const inputId = props.id ?? props.name;

  return (
    <div className="flex w-full flex-col gap-2">
      {label ? (
        inputId ? (
          <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
            {label}
            {props.required && <span className="ml-1 text-rose-500">*</span>}
          </label>
        ) : (
          <span className="text-sm font-medium text-slate-700">
            {label}
            {props.required && <span className="ml-1 text-rose-500">*</span>}
          </span>
        )
      ) : null}
      <div className="relative">
        {icon && (
          <span className="pointer-events-none absolute inset-y-0 left-4 flex items-center text-slate-400">
            {icon}
          </span>
        )}
        {rightElement && (
          <span className="absolute inset-y-0 right-3 z-10 flex items-center text-slate-400">
            {rightElement}
          </span>
        )}
        <input
          id={inputId}
          className={clsx(
            "relative z-0 w-full border bg-white text-slate-900 placeholder:text-slate-400 focus-visible:outline-none",
            "transition focus-visible:ring-4",
            hasError
              ? "border-rose-300 focus-visible:ring-rose-100"
              : "focus-visible:ring-primary/10",
            hasLeftIcon ? "pl-11" : "pl-4",
            hasRightElement ? "pr-11" : "pr-4",
            "h-12 rounded-[12px]",
            className
          )}
          style={{
            borderColor: hasError ? undefined : classhubTheme.colors.border.light,
            fontSize: classhubTheme.typography.fontSize.base
          }}
          {...props}
        />
      </div>
      {error ? (
        <span className="text-xs font-semibold text-rose-600">{error}</span>
      ) : helperText ? (
        <span className="text-xs text-slate-500">{helperText}</span>
      ) : null}
    </div>
  );
}
