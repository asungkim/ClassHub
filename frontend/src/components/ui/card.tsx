import { ReactNode } from "react";
import clsx from "clsx";
import { classhubTheme } from "@/theme/classhub-theme";

type CardProps = {
  title?: string;
  description?: string;
  children: ReactNode;
  actions?: ReactNode;
  media?: ReactNode;
  mediaClassName?: string;
};

export function Card({ title, description, children, actions, media, mediaClassName }: CardProps) {
  return (
    <section
      className="overflow-hidden rounded-2xl border border-slate-200/60 bg-white shadow-[0_1px_3px_rgba(0,0,0,0.08)]"
      style={{
        borderRadius: classhubTheme.borderRadius.lg,
        boxShadow: classhubTheme.shadows.card
      }}
    >
      {media && (
        <div className={clsx("max-h-60 overflow-hidden", mediaClassName)}>
          {media}
        </div>
      )}
      <div className="flex flex-col gap-4 p-8">
        {(title || description || actions) && (
          <header className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
            <div>
              {title && <h3 className="text-lg font-semibold text-slate-900">{title}</h3>}
              {description && <p className="text-sm text-slate-500">{description}</p>}
            </div>
            {actions && <div className="flex gap-2">{actions}</div>}
          </header>
        )}
        <div>{children}</div>
      </div>
    </section>
  );
}
