import clsx from "clsx";
import { forwardRef } from "react";

export type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement> & {
  label?: string;
  error?: string;
  helperText?: string;
};

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, helperText, className, children, ...props }, ref) => {
    const hasError = Boolean(error);

    return (
      <div className="flex flex-col gap-1.5">
        {label ? (
          <label className="text-sm font-semibold text-slate-700">
            {label}
            {props.required ? <span className="ml-1 text-rose-500">*</span> : null}
          </label>
        ) : null}

        <select
          ref={ref}
          className={clsx(
            "h-12 rounded-xl border px-4 text-sm font-medium text-slate-900 transition",
            "focus:outline-none focus:ring-2 focus:ring-offset-2",
            hasError
              ? "border-rose-300 bg-rose-50 focus:border-rose-400 focus:ring-rose-200"
              : "border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-200",
            "disabled:cursor-not-allowed disabled:opacity-50",
            className
          )}
          {...props}
        >
          {children}
        </select>

        {error ? (
          <p className="text-xs font-semibold text-rose-600">{error}</p>
        ) : helperText ? (
          <p className="text-xs text-slate-500">{helperText}</p>
        ) : null}
      </div>
    );
  }
);

Select.displayName = "Select";
