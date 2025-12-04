"use client";

import clsx from "clsx";

type InlineErrorProps = {
  message: string;
  className?: string;
};

export function InlineError({ message, className }: InlineErrorProps) {
  return (
    <div
      role="alert"
      className={clsx(
        "rounded-xl border border-red-200 bg-red-50/90 px-3 py-2 text-sm font-medium text-red-600",
        className
      )}
    >
      {message}
    </div>
  );
}
