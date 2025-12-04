"use client";

import { ReactNode } from "react";
import clsx from "clsx";
import { Button } from "@/components/ui/button";

type ErrorStateProps = {
  title: string;
  description?: string;
  icon?: ReactNode;
  retryLabel?: string;
  onRetry?: () => void;
  className?: string;
};

export function ErrorState({ title, description, icon, retryLabel = "다시 시도", onRetry, className }: ErrorStateProps) {
  return (
    <div
      className={clsx(
        "rounded-3xl border border-white/60 bg-white/80 px-6 py-8 text-center shadow-sm backdrop-blur",
        className
      )}
    >
      {icon ? <div className="mb-4 flex justify-center text-4xl">{icon}</div> : null}
      <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
      {description ? <p className="mt-2 text-sm text-slate-600">{description}</p> : null}
      {onRetry ? (
        <div className="mt-6 flex justify-center">
          <Button onClick={onRetry}>{retryLabel}</Button>
        </div>
      ) : null}
    </div>
  );
}
